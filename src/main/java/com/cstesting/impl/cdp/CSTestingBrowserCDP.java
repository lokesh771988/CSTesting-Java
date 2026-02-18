package com.cstesting.impl.cdp;

import com.cstesting.CSTestingBrowser;
import com.cstesting.CSTestingOptions;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * CSTesting browser using Chrome DevTools Protocol. Launches Chrome from Java (no Playwright, no npx).
 */
public final class CSTestingBrowserCDP implements CSTestingBrowser {

    private static final Gson GSON = new Gson();
    private final CDPConnection cdp;
    private final Process chromeProcess;

    CSTestingBrowserCDP(CDPConnection cdp, Process chromeProcess) {
        this.cdp = cdp;
        this.chromeProcess = chromeProcess;
        JsonObject noParams = new JsonObject();
        cdp.send("Page.enable", noParams);
        cdp.send("Runtime.enable", noParams);
    }

    public static CSTestingBrowser create(CSTestingOptions options) {
        ChromeLauncher.LaunchResult result = ChromeLauncher.launch(options.isHeadless());
        try {
            CDPConnection conn = new CDPConnection(result.webSocketUrl);
            return new CSTestingBrowserCDP(conn, result.process);
        } catch (Exception e) {
            result.process.destroyForcibly();
            throw new RuntimeException("Failed to connect to Chrome CDP: " + e.getMessage(), e);
        }
    }

    private String eval(String expression) {
        JsonObject params = new JsonObject();
        params.addProperty("expression", expression);
        params.addProperty("returnByValue", true);
        JsonObject res = cdp.send("Runtime.evaluate", params);
        if (res.has("exceptionDetails")) return null;
        if (!res.has("result")) return null;
        var value = res.getAsJsonObject("result").get("value");
        if (value == null || value.isJsonNull()) return null;
        if (value.isJsonPrimitive()) return value.getAsString();
        return value.toString();
    }

    private Object evalObject(String expression) {
        JsonObject params = new JsonObject();
        params.addProperty("expression", expression);
        params.addProperty("returnByValue", true);
        JsonObject res = cdp.send("Runtime.evaluate", params);
        if (res.has("exceptionDetails")) return null;
        if (!res.has("result")) return null;
        return res.getAsJsonObject("result").get("value");
    }

    private void evalClick(String selector) {
        String s = GSON.toJson(selector);
        eval("(function(){ var el = document.querySelector(" + s + "); if(el) el.click(); })()");
    }

    @Override
    public void gotoUrl(String url) {
        JsonObject params = new JsonObject();
        params.addProperty("url", url);
        JsonObject result = cdp.send("Page.navigate", params);
        if (result.has("errorText") && !result.get("errorText").getAsString().isEmpty()) {
            throw new RuntimeException("Navigation failed: " + result.get("errorText").getAsString());
        }
        waitForLoad();
    }

    @Override
    public void click(String selector) {
        evalClick(selector);
    }

    @Override
    public void type(String selector, String text) {
        String sel = GSON.toJson(selector);
        String txt = GSON.toJson(text);
        eval("(function(){ var el = document.querySelector(" + sel + "); if(el) { el.focus(); el.value = " + txt + "; el.dispatchEvent(new Event('input', { bubbles: true })); } })()");
    }

    @Override
    public void select(String selector, Object option) {
        String sel = GSON.toJson(selector);
        String opt = option instanceof String ? GSON.toJson((String) option) : String.valueOf(option);
        eval("(function(){ var el = document.querySelector(" + sel + "); if(el && el.tagName==='SELECT') el.value = " + opt + "; })()");
    }

    @Override
    public void check(String selector) {
        String s = GSON.toJson(selector);
        eval("(function(){ var el = document.querySelector(" + s + "); if(el && !el.checked) el.click(); })()");
    }

    @Override
    public void uncheck(String selector) {
        String s = GSON.toJson(selector);
        eval("(function(){ var el = document.querySelector(" + s + "); if(el && el.checked) el.click(); })()");
    }

    @Override
    public void waitForSelector(String selector, Integer timeoutMs) {
        long deadline = System.currentTimeMillis() + (timeoutMs != null ? timeoutMs : 30_000);
        String s = GSON.toJson(selector);
        while (System.currentTimeMillis() < deadline) {
            String v = eval("document.querySelector(" + s + ") ? 'ok' : null");
            if ("ok".equals(v)) return;
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
        }
        throw new RuntimeException("Timeout waiting for selector: " + selector);
    }

    @Override
    public void waitForURL(String urlOrPattern, Integer timeoutMs) {
        long deadline = System.currentTimeMillis() + (timeoutMs != null ? timeoutMs : 30_000);
        String pattern = urlOrPattern.contains("*") ? urlOrPattern.replace(".", "\\.").replace("**", ".*").replace("*", "[^/]*") : null;
        while (System.currentTimeMillis() < deadline) {
            String current = url();
            if (current != null) {
                if (pattern != null && current.matches(pattern)) return;
                if (pattern == null && current.contains(urlOrPattern)) return;
            }
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
        }
        throw new RuntimeException("Timeout waiting for URL: " + urlOrPattern);
    }

    @Override
    public void waitForLoad() {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            String ready = eval("document.readyState");
            if ("complete".equals(ready)) return;
            try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
        }
    }

    @Override
    public String url() {
        String u = eval("window.location.href");
        return u != null ? u : "";
    }

    @Override
    public String content() {
        String c = eval("document.documentElement.outerHTML");
        return c != null ? c : "";
    }

    @Override
    public Object evaluate(String expression) {
        return evalObject("(" + expression + ")");
    }

    @Override
    public boolean isVisible(String selector) {
        String s = GSON.toJson(selector);
        String v = eval("(function(){ var el = document.querySelector(" + s + "); return el ? (el.offsetParent !== null && getComputedStyle(el).visibility !== 'hidden') : false; })()");
        return Boolean.parseBoolean(v);
    }

    @Override
    public boolean isDisabled(String selector) {
        String s = GSON.toJson(selector);
        String v = eval("(function(){ var el = document.querySelector(" + s + "); return el ? el.disabled : false; })()");
        return Boolean.parseBoolean(v);
    }

    @Override
    public boolean isSelected(String selector) {
        String s = GSON.toJson(selector);
        String v = eval("(function(){ var el = document.querySelector(" + s + "); return el ? el.checked : false; })()");
        return Boolean.parseBoolean(v);
    }

    @Override
    public String getTextContent(String selector) {
        String s = GSON.toJson(selector);
        String v = eval("(function(){ var el = document.querySelector(" + s + "); return el ? el.textContent : ''; })()");
        return v != null ? v : "";
    }

    @Override
    public CSTestingBrowser frame(String iframeSelector) {
        throw new UnsupportedOperationException("frame() not yet implemented for CDP backend");
    }

    @Override
    public void close() {
        cdp.close();
        if (chromeProcess.isAlive()) chromeProcess.destroyForcibly();
    }
}
