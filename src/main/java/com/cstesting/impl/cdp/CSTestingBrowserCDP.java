package com.cstesting.impl.cdp;

import com.cstesting.Assertion;
import com.cstesting.CSTestingBrowser;
import com.cstesting.Locator;
import com.cstesting.CSTestingOptions;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CSTesting browser using Chrome DevTools Protocol. Launches Chrome from Java (no Playwright, no npx).
 */
public final class CSTestingBrowserCDP implements CSTestingBrowser {

    private static final Gson GSON = new Gson();
    private static final Map<String, Integer> FRAME_ID_TO_CONTEXT_ID = new ConcurrentHashMap<>();

    private CDPConnection cdp;
    private final Process chromeProcess;
    private final Integer executionContextId; // null = main frame
    private final String frameId;            // null = main frame
    private final Integer documentRootNodeId; // null = main frame; for frames, root of frame's document for DOM.querySelector
    private volatile String lastAlertMessage;
    private volatile Boolean pendingAccept; // true = accept, false = dismiss, null = none
    private volatile String pendingPromptText;

    CSTestingBrowserCDP(CDPConnection cdp, Process chromeProcess) {
        this(cdp, chromeProcess, null, null, null);
        initCDP();
    }

    private void initCDP() {
        JsonObject noParams = new JsonObject();
        cdp.send("Page.enable", noParams);
        cdp.send("Runtime.enable", noParams);
        cdp.send("DOM.enable", noParams);
        cdp.setEventHandler(this::handleCDPEvent);
    }

    private CSTestingBrowserCDP(CDPConnection cdp, Process chromeProcess, Integer executionContextId, String frameId, Integer documentRootNodeId) {
        this.cdp = cdp;
        this.chromeProcess = chromeProcess;
        this.executionContextId = executionContextId;
        this.frameId = frameId;
        this.documentRootNodeId = documentRootNodeId;
    }

    private void handleCDPEvent(CDPConnection connection, JsonObject event) {
        String method = event.has("method") ? event.get("method").getAsString() : null;
        if ("Runtime.executionContextCreated".equals(method)) {
            JsonObject params = event.has("params") ? event.getAsJsonObject("params") : new JsonObject();
            if (params.has("context")) {
                JsonObject ctx = params.getAsJsonObject("context");
                int id = ctx.get("id").getAsInt();
                if (ctx.has("auxData") && !ctx.get("auxData").isJsonNull()) {
                    JsonObject aux = ctx.getAsJsonObject("auxData");
                    if (aux.has("frameId")) {
                        FRAME_ID_TO_CONTEXT_ID.put(aux.get("frameId").getAsString(), id);
                    }
                }
            }
            return;
        }
        if (!"Page.javascriptDialogOpening".equals(method))
            return;
        JsonObject params = event.has("params") ? event.getAsJsonObject("params") : new JsonObject();
        lastAlertMessage = params.has("message") ? params.get("message").getAsString() : "";
        Boolean accept = pendingAccept;
        String promptText = pendingPromptText;
        pendingAccept = null;
        pendingPromptText = null;
        if (accept != null) {
            JsonObject handleParams = new JsonObject();
            handleParams.addProperty("accept", accept);
            if (promptText != null) handleParams.addProperty("promptText", promptText);
            connection.send("Page.handleJavaScriptDialog", handleParams);
        }
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
        if (executionContextId != null) params.addProperty("contextId", executionContextId);
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
        if (executionContextId != null) params.addProperty("contextId", executionContextId);
        JsonObject res = cdp.send("Runtime.evaluate", params);
        if (res.has("exceptionDetails")) return null;
        if (!res.has("result")) return null;
        return res.getAsJsonObject("result").get("value");
    }

    private void evalClick(String selector) {
        String s = GSON.toJson(selector);
        eval("(function(){ var el = document.querySelector(" + s + "); if(el) el.click(); })()");
    }

    /** Get viewport (x,y) of element center. Returns int[2] or null if element not found. */
    private int[] getElementCenter(String elExpr) {
        String result = eval("(function(){ var el = " + elExpr + "; if(!el) return null; el.scrollIntoView({block:'center'}); " +
            "var r = el.getBoundingClientRect(); return Math.round(r.left + r.width/2) + ',' + Math.round(r.top + r.height/2); })()");
        if (result == null || result.isEmpty() || result.equals("null")) return null;
        String[] parts = result.split(",");
        if (parts.length != 2) return null;
        try {
            return new int[]{ Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()) };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void dispatchMouseEvent(String type, int x, int y, String button, int clickCount) {
        JsonObject params = new JsonObject();
        params.addProperty("type", type);
        params.addProperty("x", x);
        params.addProperty("y", y);
        params.addProperty("button", button);
        params.addProperty("clickCount", clickCount);
        cdp.send("Input.dispatchMouseEvent", params);
    }

    private void hoverAt(String elExpr) {
        int[] c = getElementCenter(elExpr);
        if (c != null) dispatchMouseEvent("mouseMoved", c[0], c[1], "none", 0);
    }

    private void doubleClickAt(String elExpr) {
        int[] c = getElementCenter(elExpr);
        if (c != null) {
            dispatchMouseEvent("mousePressed", c[0], c[1], "left", 2);
            dispatchMouseEvent("mouseReleased", c[0], c[1], "left", 2);
        }
    }

    private void rightClickAt(String elExpr) {
        int[] c = getElementCenter(elExpr);
        if (c != null) {
            dispatchMouseEvent("mousePressed", c[0], c[1], "right", 1);
            dispatchMouseEvent("mouseReleased", c[0], c[1], "right", 1);
        }
    }

    private void dragAndDropFromTo(String fromExpr, String toExpr) {
        int[] from = getElementCenter(fromExpr);
        int[] to = getElementCenter(toExpr);
        if (from != null && to != null) {
            dispatchMouseEvent("mouseMoved", from[0], from[1], "none", 0);
            dispatchMouseEvent("mousePressed", from[0], from[1], "left", 1);
            dispatchMouseEvent("mouseMoved", to[0], to[1], "left", 0);
            dispatchMouseEvent("mouseReleased", to[0], to[1], "left", 1);
        }
    }

    /** Returns a JS expression that evaluates to the element (or throws if multiple and no index). */
    private String elementExpr(Locator loc) {
        String sel = GSON.toJson(loc.getResolvedSelector());
        boolean xpath = loc.isXPath();
        Integer idx = loc.getIndex();
        String idxJs = idx == null ? "undefined" : String.valueOf(idx);
        if (xpath) {
            return "(function(){ var r = document.evaluate(" + sel + ", document, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null); " +
                "var len = r.snapshotLength; if(len===0) return null; " +
                "if(" + idxJs + "===undefined) { if(len>1) throw new Error('Multiple elements'); return r.snapshotItem(0); } " +
                "var i = " + idxJs + "===-1 ? len-1 : " + idxJs + "; return r.snapshotItem(i); })()";
        }
        return "(function(){ var els = document.querySelectorAll(" + sel + "); var len = els.length; if(len===0) return null; " +
            "if(" + idxJs + "===undefined) { if(len>1) throw new Error('Multiple elements'); return els[0]; } " +
            "var i = " + idxJs + "===-1 ? len-1 : " + idxJs + "; return els[i]; })()";
    }

    @Override
    public void gotoUrl(String url) {
        JsonObject params = new JsonObject();
        params.addProperty("url", url);
        if (frameId != null) params.addProperty("frameId", frameId);
        JsonObject result = cdp.send("Page.navigate", params);
        if (result.has("errorText") && !result.get("errorText").getAsString().isEmpty()) {
            throw new RuntimeException("Navigation failed: " + result.get("errorText").getAsString());
        }
        waitForLoad();
    }

    @Override
    public void back() {
        eval("history.back()");
        waitForLoad();
    }

    @Override
    public void forward() {
        eval("history.forward()");
        waitForLoad();
    }

    @Override
    public void refresh() {
        eval("location.reload()");
        waitForLoad();
    }

    @Override
    public void click(String selector) {
        evalClick(selector);
    }

    @Override
    public void click(Locator locator) {
        eval("(function(){ var el = " + elementExpr(locator) + "; if(el) el.click(); })()");
    }

    @Override
    public void hover(String selector) {
        String s = GSON.toJson(selector);
        hoverAt("document.querySelector(" + s + ")");
    }

    @Override
    public void hover(Locator locator) {
        hoverAt(elementExpr(locator));
    }

    @Override
    public void doubleClick(String selector) {
        String s = GSON.toJson(selector);
        doubleClickAt("document.querySelector(" + s + ")");
    }

    @Override
    public void doubleClick(Locator locator) {
        doubleClickAt(elementExpr(locator));
    }

    @Override
    public void rightClick(String selector) {
        String s = GSON.toJson(selector);
        rightClickAt("document.querySelector(" + s + ")");
    }

    @Override
    public void rightClick(Locator locator) {
        rightClickAt(elementExpr(locator));
    }

    @Override
    public void dragAndDrop(String fromSelector, String toSelector) {
        String fromS = GSON.toJson(fromSelector);
        String toS = GSON.toJson(toSelector);
        dragAndDropFromTo("document.querySelector(" + fromS + ")", "document.querySelector(" + toS + ")");
    }

    @Override
    public void dragAndDrop(Locator from, Locator to) {
        dragAndDropFromTo(elementExpr(from), elementExpr(to));
    }

    @Override
    public void type(String selector, String text) {
        String sel = GSON.toJson(selector);
        String txt = GSON.toJson(text);
        eval("(function(){ var el = document.querySelector(" + sel + "); if(el) { el.focus(); el.value = " + txt + "; el.dispatchEvent(new Event('input', { bubbles: true })); } })()");
    }

    @Override
    public void type(Locator locator, String text) {
        String txt = GSON.toJson(text);
        eval("(function(){ var el = " + elementExpr(locator) + "; if(el) { el.focus(); el.value = " + txt + "; el.dispatchEvent(new Event('input', { bubbles: true })); } })()");
    }

    private static String selectOneJs(String elExpr, String optJson, boolean isIndex) {
        if (isIndex) {
            return "(function(){ var el = " + elExpr + "; if(el && el.tagName==='SELECT') { el.selectedIndex = " + optJson + "; el.dispatchEvent(new Event('change',{bubbles:true})); } })()";
        }
        return "(function(){ var el = " + elExpr + "; if(!el || el.tagName!=='SELECT') return; " +
            "var v = " + optJson + "; el.value = v; " +
            "if(el.value !== v) { for(var i=0;i<el.options.length;i++) { var o=el.options[i]; if(o.value===v || o.text.trim()===v) { o.selected=true; break; } } } " +
            "el.dispatchEvent(new Event('change',{bubbles:true})); })()";
    }

    @Override
    public void select(String selector, Object option) {
        String sel = GSON.toJson(selector);
        boolean isIndex = option instanceof Integer || option instanceof Long;
        String optJson = isIndex ? String.valueOf(option instanceof Long ? ((Long) option).intValue() : option) : GSON.toJson(String.valueOf(option));
        eval(selectOneJs("document.querySelector(" + sel + ")", optJson, isIndex));
    }

    @Override
    public void select(Locator locator, Object option) {
        boolean isIndex = option instanceof Integer || option instanceof Long;
        String optJson = isIndex ? String.valueOf(option instanceof Long ? ((Long) option).intValue() : option) : GSON.toJson(String.valueOf(option));
        eval(selectOneJs(elementExpr(locator), optJson, isIndex));
    }

    private void selectOptionsJs(String elExpr, String optionsJson) {
        eval("(function(){ var el = " + elExpr + "; if(!el || el.tagName!=='SELECT') return; var opts = " + optionsJson + "; " +
            "for(var i=0;i<el.options.length;i++) el.options[i].selected = false; " +
            "for(var j=0;j<opts.length;j++) { var o = opts[j]; var idx = typeof o === 'number' ? o : -1; " +
            "if(idx>=0 && idx<el.options.length) { el.options[idx].selected = true; } else { " +
            "var s = String(o); for(var k=0;k<el.options.length;k++) { var opt=el.options[k]; if(opt.value===s || opt.text.trim()===s) { opt.selected=true; break; } } } } " +
            "el.dispatchEvent(new Event('change',{bubbles:true})); })()");
    }

    private void deselectOptionsJs(String elExpr, String optionsJson) {
        eval("(function(){ var el = " + elExpr + "; if(!el || el.tagName!=='SELECT') return; var opts = " + optionsJson + "; " +
            "for(var j=0;j<opts.length;j++) { var o = opts[j]; var idx = typeof o === 'number' ? o : -1; " +
            "if(idx>=0 && idx<el.options.length) { el.options[idx].selected = false; } else { " +
            "var s = String(o); for(var k=0;k<el.options.length;k++) { var opt=el.options[k]; if(opt.value===s || opt.text.trim()===s) { opt.selected=false; break; } } } } " +
            "el.dispatchEvent(new Event('change',{bubbles:true})); })()");
    }

    private static String optionsToJson(Object[] options) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < options.length; i++) {
            if (i > 0) sb.append(",");
            if (options[i] instanceof Number) {
                sb.append(((Number) options[i]).intValue());
            } else {
                sb.append(GSON.toJson(String.valueOf(options[i])));
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public void selectOptions(String selector, Object... options) {
        if (options == null || options.length == 0) return;
        String sel = GSON.toJson(selector);
        String optsJson = optionsToJson(options);
        selectOptionsJs("document.querySelector(" + sel + ")", optsJson);
    }

    @Override
    public void selectOptions(Locator locator, Object... options) {
        if (options == null || options.length == 0) return;
        selectOptionsJs(elementExpr(locator), optionsToJson(options));
    }

    @Override
    public void deselectOptions(String selector, Object... options) {
        if (options == null || options.length == 0) return;
        String sel = GSON.toJson(selector);
        deselectOptionsJs("document.querySelector(" + sel + ")", optionsToJson(options));
    }

    @Override
    public void deselectOptions(Locator locator, Object... options) {
        if (options == null || options.length == 0) return;
        deselectOptionsJs(elementExpr(locator), optionsToJson(options));
    }

    private List<String> getSelectedValuesJs(String elExpr) {
        String json = eval("(function(){ var el = " + elExpr + "; if(!el || el.tagName!=='SELECT') return '[]'; " +
            "return JSON.stringify(Array.from(el.selectedOptions).map(function(o){ return o.value; })); })()");
        if (json == null || json.isEmpty()) return Collections.emptyList();
        return GSON.fromJson(json, TypeToken.getParameterized(List.class, String.class).getType());
    }

    private List<String> getSelectedLabelsJs(String elExpr) {
        String json = eval("(function(){ var el = " + elExpr + "; if(!el || el.tagName!=='SELECT') return '[]'; " +
            "return JSON.stringify(Array.from(el.selectedOptions).map(function(o){ return o.text; })); })()");
        if (json == null || json.isEmpty()) return Collections.emptyList();
        return GSON.fromJson(json, TypeToken.getParameterized(List.class, String.class).getType());
    }

    @Override
    public List<String> getSelectedValues(String selector) {
        String sel = GSON.toJson(selector);
        return getSelectedValuesJs("document.querySelector(" + sel + ")");
    }

    @Override
    public List<String> getSelectedValues(Locator locator) {
        return getSelectedValuesJs(elementExpr(locator));
    }

    @Override
    public List<String> getSelectedLabels(String selector) {
        String sel = GSON.toJson(selector);
        return getSelectedLabelsJs("document.querySelector(" + sel + ")");
    }

    @Override
    public List<String> getSelectedLabels(Locator locator) {
        return getSelectedLabelsJs(elementExpr(locator));
    }

    private static String checkUncheckJs(String elExpr, boolean check) {
        String condition = check ? "!el.checked" : "el.checked";
        return "(function(){ var el = " + elExpr + "; if(!el) return; el.scrollIntoView({block:'center'}); "
            + "if(el && " + condition + ") { el.focus(); el.click(); } })()";
    }

    @Override
    public void check(String selector) {
        String s = GSON.toJson(selector);
        eval(checkUncheckJs("document.querySelector(" + s + ")", true));
    }

    @Override
    public void uncheck(String selector) {
        String s = GSON.toJson(selector);
        eval(checkUncheckJs("document.querySelector(" + s + ")", false));
    }

    @Override
    public void check(Locator locator) {
        eval(checkUncheckJs(elementExpr(locator), true));
    }

    @Override
    public void uncheck(Locator locator) {
        eval(checkUncheckJs(elementExpr(locator), false));
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
    public void waitForSelector(Locator locator, Integer timeoutMs) {
        waitForSelector(locator.getResolvedSelector(), timeoutMs);
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
        waitForLoad(30_000);
    }

    @Override
    public void waitForLoad(Integer timeoutMs) {
        long deadline = System.currentTimeMillis() + (timeoutMs != null ? timeoutMs : 30_000);
        while (System.currentTimeMillis() < deadline) {
            String ready = eval("document.readyState");
            if ("complete".equals(ready)) return;
            try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
        }
        throw new RuntimeException("Timeout waiting for page load");
    }

    @Override
    public void waitForPage(Integer timeoutMs) {
        waitForLoad(timeoutMs);
    }

    @Override
    public void waitForNetworkLoad(Integer timeoutMs) {
        waitForLoad(timeoutMs);
        long idleMs = 300;
        long deadline = System.currentTimeMillis() + (timeoutMs != null ? timeoutMs : 30_000);
        long lastActivity = System.currentTimeMillis();
        while (System.currentTimeMillis() < deadline) {
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
            if (System.currentTimeMillis() - lastActivity >= idleMs) return;
        }
    }

    @Override
    public void waitForTime(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("waitForTime interrupted", e);
        }
    }

    @Override
    public void scrollToPageTop() {
        eval("(function(){ var el = document.scrollingElement || document.documentElement || document.body; if(el) el.scrollTop = 0; })()");
    }

    @Override
    public void scrollToPageBottom() {
        eval("(function(){ var el = document.scrollingElement || document.documentElement || document.body; if(el) el.scrollTop = Math.max(0, el.scrollHeight - window.innerHeight); })()");
    }

    @Override
    public void scrollUp() {
        eval("(function(){ var el = document.scrollingElement || document.documentElement || document.body; if(el) el.scrollTop -= window.innerHeight; })()");
    }

    @Override
    public void scrollDown() {
        eval("(function(){ var el = document.scrollingElement || document.documentElement || document.body; if(el) el.scrollTop += window.innerHeight; })()");
    }

    @Override
    public void scrollBy(int deltaX, int deltaY) {
        eval("(function(){ var el = document.scrollingElement || document.documentElement || document.body; if(el) { el.scrollTop += " + deltaY + "; el.scrollLeft += " + deltaX + "; } })()");
    }

    @Override
    public void scrollToSelector(String selector) {
        String s = GSON.toJson(selector);
        eval("(function(){ var el = document.querySelector(" + s + "); if(el) el.scrollIntoView({block:'center'}); })()");
    }

    @Override
    public void scrollToSelector(Locator locator) {
        eval("(function(){ var el = " + elementExpr(locator) + "; if(el) el.scrollIntoView({block:'center'}); })()");
    }

    @Override
    public void acceptNextAlert() {
        pendingAccept = true;
        pendingPromptText = null;
    }

    @Override
    public void acceptNextAlert(String promptText) {
        pendingAccept = true;
        pendingPromptText = promptText;
    }

    @Override
    public void dismissNextAlert() {
        pendingAccept = false;
        pendingPromptText = null;
    }

    @Override
    public String getLastAlertMessage() {
        return lastAlertMessage != null ? lastAlertMessage : "";
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
    public boolean isVisible(Locator locator) {
        String v = eval("(function(){ var el = " + elementExpr(locator) + "; return el ? (el.offsetParent !== null && getComputedStyle(el).visibility !== 'hidden') : false; })()");
        return Boolean.parseBoolean(v);
    }

    @Override
    public boolean isDisabled(String selector) {
        String s = GSON.toJson(selector);
        String v = eval("(function(){ var el = document.querySelector(" + s + "); return el ? el.disabled : false; })()");
        return Boolean.parseBoolean(v);
    }

    @Override
    public boolean isDisabled(Locator locator) {
        String v = eval("(function(){ var el = " + elementExpr(locator) + "; return el ? el.disabled : false; })()");
        return Boolean.parseBoolean(v);
    }

    @Override
    public boolean isSelected(String selector) {
        String s = GSON.toJson(selector);
        String v = eval("(function(){ var el = document.querySelector(" + s + "); return el ? el.checked : false; })()");
        return Boolean.parseBoolean(v);
    }

    @Override
    public boolean isSelected(Locator locator) {
        String v = eval("(function(){ var el = " + elementExpr(locator) + "; return el ? el.checked : false; })()");
        return Boolean.parseBoolean(v);
    }

    @Override
    public String getTextContent(String selector) {
        String s = GSON.toJson(selector);
        String v = eval("(function(){ var el = document.querySelector(" + s + "); return el ? el.textContent : ''; })()");
        return v != null ? v : "";
    }

    @Override
    public String getTextContent(Locator locator) {
        String v = eval("(function(){ var el = " + elementExpr(locator) + "; return el ? el.textContent : ''; })()");
        return v != null ? v : "";
    }

    @Override
    public String getValue(String selector) {
        String s = GSON.toJson(selector);
        String v = eval("(function(){ var el = document.querySelector(" + s + "); return el && (el.value !== undefined) ? el.value : ''; })()");
        return v != null ? v : "";
    }

    @Override
    public String getValue(Locator locator) {
        String v = eval("(function(){ var el = " + elementExpr(locator) + "; return el && (el.value !== undefined) ? el.value : ''; })()");
        return v != null ? v : "";
    }

    @Override
    public String getAttribute(String selector, String attrName) {
        String sel = GSON.toJson(selector);
        String attr = GSON.toJson(attrName);
        String v = eval("(function(){ var el = document.querySelector(" + sel + "); return el ? el.getAttribute(" + attr + ") : null; })()");
        return v;
    }

    @Override
    public String getAttribute(Locator locator, String attrName) {
        String attr = GSON.toJson(attrName);
        String v = eval("(function(){ var el = " + elementExpr(locator) + "; return el ? el.getAttribute(" + attr + ") : null; })()");
        return v;
    }

    @Override
    public boolean isEditable(String selector) {
        String s = GSON.toJson(selector);
        String v = eval("(function(){ var el = document.querySelector(" + s + "); if(!el) return false; if(el.disabled) return false; var tag = el.tagName && el.tagName.toUpperCase(); if(tag==='INPUT'||tag==='TEXTAREA') return !el.readOnly; if(tag==='SELECT') return true; return false; })()");
        return Boolean.parseBoolean(v);
    }

    @Override
    public boolean isEditable(Locator locator) {
        String v = eval("(function(){ var el = " + elementExpr(locator) + "; if(!el) return false; if(el.disabled) return false; var tag = el.tagName && el.tagName.toUpperCase(); if(tag==='INPUT'||tag==='TEXTAREA') return !el.readOnly; if(tag==='SELECT') return true; return false; })()");
        return Boolean.parseBoolean(v);
    }

    @Override
    public int locatorCount(Locator locator) {
        String sel = GSON.toJson(locator.getResolvedSelector());
        if (locator.isXPath()) {
            String v = eval("(function(){ var r = document.evaluate(" + sel + ", document, null, XPathResult.NUMBER_TYPE, null); return r.numberValue; })()");
            return v != null ? (int) Double.parseDouble(v) : 0;
        }
        String v = eval("(function(){ return document.querySelectorAll(" + sel + ").length; })()");
        return v != null ? Integer.parseInt(v) : 0;
    }

    @Override
    public String title() {
        String t = eval("document.title");
        return t != null ? t : "";
    }

    @Override
    public Assertion assertThat(Locator locator) {
        return new Assertion(this, locator);
    }

    @Override
    public Assertion assertThat() {
        return new Assertion(this, null);
    }

    @Override
    public Locator locator(String selector) {
        return new Locator(this, selector);
    }

    private int getDebugPort() {
        URI uri = cdp.getUri();
        int p = uri.getPort();
        return p > 0 ? p : 9222;
    }

    @Override
    public List<String> getWindowHandles() {
        return ChromeLauncher.getAllPageWebSocketUrls(getDebugPort());
    }

    @Override
    public String getCurrentWindowHandle() {
        String current = cdp.getUri().toString();
        List<String> handles = getWindowHandles();
        for (String h : handles) {
            if (h.equals(current)) return h;
        }
        return handles.isEmpty() ? "" : handles.get(0);
    }

    @Override
    public void switchToWindow(String handle) {
        if (executionContextId != null) {
            throw new UnsupportedOperationException("switchToWindow not supported from inside a frame; switch on the main page browser.");
        }
        if (handle == null || handle.isEmpty()) return;
        if (handle.equals(cdp.getUri().toString())) return;
        cdp.close();
        try {
            cdp = new CDPConnection(URI.create(handle));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while connecting to tab", e);
        }
        FRAME_ID_TO_CONTEXT_ID.clear();
        initCDP();
    }

    /** Create a browser instance for the given tab URL (new connection). Used by getPages() and newTab(). */
    private CSTestingBrowserCDP pageForUrl(String wsUrl) {
        try {
            CDPConnection conn = new CDPConnection(URI.create(wsUrl));
            CSTestingBrowserCDP b = new CSTestingBrowserCDP(conn, chromeProcess, null, null, null);
            b.initCDP();
            return b;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while connecting to tab", e);
        }
    }

    @Override
    public List<CSTestingBrowser> getPages() {
        if (executionContextId != null) {
            return ChromeLauncher.getAllPageWebSocketUrls(getDebugPort()).stream()
                .map(this::pageForUrl)
                .collect(java.util.stream.Collectors.toList());
        }
        List<String> urls = ChromeLauncher.getAllPageWebSocketUrls(getDebugPort());
        List<CSTestingBrowser> list = new ArrayList<>();
        String current = cdp.getUri().toString();
        for (String url : urls) {
            if (url.equals(current)) list.add(this);
            else list.add(pageForUrl(url));
        }
        return list;
    }

    @Override
    public CSTestingBrowser newTab() {
        if (executionContextId != null) {
            throw new UnsupportedOperationException("newTab not supported from inside a frame; use the main page browser.");
        }
        JsonObject params = new JsonObject();
        params.addProperty("url", "about:blank");
        JsonObject res = cdp.send("Target.createTarget", params);
        String targetId = res.has("targetId") ? res.get("targetId").getAsString() : null;
        if (targetId == null) throw new RuntimeException("Target.createTarget did not return targetId");
        for (int i = 0; i < 25; i++) {
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
            for (com.google.gson.JsonObject t : ChromeLauncher.getPageTargets(getDebugPort())) {
                if (targetId.equals(t.has("id") ? t.get("id").getAsString() : null) && t.has("webSocketDebuggerUrl")) {
                    String newUrl = t.get("webSocketDebuggerUrl").getAsString();
                    return pageForUrl(newUrl);
                }
            }
        }
        throw new RuntimeException("New tab did not appear in /json/list within 5s");
    }

    @Override
    public CSTestingBrowser frame(String iframeSelector) {
        int rootNodeId;
        if (documentRootNodeId != null) {
            rootNodeId = documentRootNodeId;
        } else {
            JsonObject docRes = cdp.send("DOM.getDocument", new JsonObject());
            if (!docRes.has("root") || !docRes.getAsJsonObject("root").has("nodeId")) {
                throw new RuntimeException("DOM.getDocument failed");
            }
            rootNodeId = docRes.getAsJsonObject("root").get("nodeId").getAsInt();
        }
        JsonObject queryParams = new JsonObject();
        queryParams.addProperty("nodeId", rootNodeId);
        queryParams.addProperty("selector", iframeSelector);
        JsonObject queryRes = cdp.send("DOM.querySelector", queryParams);
        int iframeNodeId = queryRes.has("nodeId") ? queryRes.get("nodeId").getAsInt() : 0;
        if (iframeNodeId == 0) {
            throw new RuntimeException("Iframe not found: " + iframeSelector);
        }
        JsonObject describeParams = new JsonObject();
        describeParams.addProperty("nodeId", iframeNodeId);
        JsonObject describeRes = cdp.send("DOM.describeNode", describeParams);
        if (!describeRes.has("node") || !describeRes.getAsJsonObject("node").has("frameId")) {
            throw new RuntimeException("Element is not an iframe or has no frame: " + iframeSelector);
        }
        JsonObject node = describeRes.getAsJsonObject("node");
        String resolvedFrameId = node.get("frameId").getAsString();
        Integer childDocRootNodeId = null;
        if (node.has("contentDocument") && !node.get("contentDocument").isJsonNull()) {
            JsonObject contentDoc = node.getAsJsonObject("contentDocument");
            if (contentDoc.has("nodeId")) childDocRootNodeId = contentDoc.get("nodeId").getAsInt();
        }
        JsonObject noParams = new JsonObject();
        for (int i = 0; i < 50; i++) {
            Integer contextId = FRAME_ID_TO_CONTEXT_ID.get(resolvedFrameId);
            if (contextId != null)
                return new CSTestingBrowserCDP(cdp, chromeProcess, contextId, resolvedFrameId, childDocRootNodeId);
            if (i == 0) {
                cdp.send("Runtime.disable", noParams);
                cdp.send("Runtime.enable", noParams);
            } else {
                JsonObject noOp = new JsonObject();
                noOp.addProperty("expression", "1");
                noOp.addProperty("returnByValue", true);
                cdp.send("Runtime.evaluate", noOp);
            }
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
        }
        throw new RuntimeException("Execution context for iframe not ready after 10s; ensure iframe has loaded (e.g. waitForSelector then waitForTime for slow embeds): " + iframeSelector);
    }

    @Override
    public void close() {
        cdp.close();
        if (chromeProcess.isAlive()) chromeProcess.destroyForcibly();
    }
}
