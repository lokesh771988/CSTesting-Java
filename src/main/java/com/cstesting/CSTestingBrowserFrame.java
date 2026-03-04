package com.cstesting;

import com.cstesting.protocol.ServerConnection;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Frame handle: same API as browser but scoped to an iframe.
 * Sends commands with frameId so the server runs them in the frame context.
 */
final class CSTestingBrowserFrame implements CSTestingBrowser {

    private final ServerConnection connection;
    private final String frameId;

    CSTestingBrowserFrame(ServerConnection connection, String frameId) {
        this.connection = connection;
        this.frameId = frameId;
    }

    private JsonObject send(String method, Map<String, Object> params) {
        JsonObject req = new JsonObject();
        req.addProperty("id", System.nanoTime());
        req.addProperty("method", method);
        req.addProperty("frameId", frameId);
        if (params != null && !params.isEmpty()) {
            req.add("params", new com.google.gson.Gson().toJsonTree(params));
        }
        return connection.send(req);
    }

    @Override
    public void gotoUrl(String url) {
        send("goto", Map.of("url", url));
    }

    @Override
    public void back() {
        send("back", Map.of());
    }

    @Override
    public void forward() {
        send("forward", Map.of());
    }

    @Override
    public void refresh() {
        send("refresh", Map.of());
    }

    @Override
    public void click(String selector) {
        send("click", Map.of("selector", selector));
    }

    @Override
    public void click(Locator locator) {
        send("click", locatorParams(locator));
    }

    @Override
    public void hover(String selector) {
        send("hover", Map.of("selector", selector));
    }

    @Override
    public void hover(Locator locator) {
        send("hover", locatorParams(locator));
    }

    @Override
    public void doubleClick(String selector) {
        send("doubleClick", Map.of("selector", selector));
    }

    @Override
    public void doubleClick(Locator locator) {
        send("doubleClick", locatorParams(locator));
    }

    @Override
    public void rightClick(String selector) {
        send("rightClick", Map.of("selector", selector));
    }

    @Override
    public void rightClick(Locator locator) {
        send("rightClick", locatorParams(locator));
    }

    @Override
    public void dragAndDrop(String fromSelector, String toSelector) {
        send("dragAndDrop", Map.of("fromSelector", fromSelector, "toSelector", toSelector));
    }

    @Override
    public void dragAndDrop(Locator from, Locator to) {
        Map<String, Object> params = new HashMap<>();
        params.put("fromSelector", from.getResolvedSelector());
        if (from.getIndex() != null) params.put("fromIndex", from.getIndex());
        if (from.isXPath()) params.put("fromIsXPath", true);
        params.put("toSelector", to.getResolvedSelector());
        if (to.getIndex() != null) params.put("toIndex", to.getIndex());
        if (to.isXPath()) params.put("toIsXPath", true);
        send("dragAndDrop", params);
    }

    @Override
    public void type(String selector, String text) {
        send("type", Map.of("selector", selector, "text", text));
    }

    @Override
    public void type(Locator locator, String text) {
        Map<String, Object> params = locatorParams(locator);
        params.put("text", text);
        send("type", params);
    }

    @Override
    public void pressKey(String key) {
        send("pressKey", Map.of("key", key != null ? key : ""));
    }

    @Override
    public void select(String selector, Object option) {
        send("select", Map.of("selector", selector, "option", option));
    }

    @Override
    public void select(Locator locator, Object option) {
        Map<String, Object> params = locatorParams(locator);
        params.put("option", option);
        send("select", params);
    }

    @Override
    public void selectOptions(String selector, Object... options) {
        if (options == null || options.length == 0) return;
        Map<String, Object> params = new HashMap<>();
        params.put("selector", selector);
        params.put("options", options.length == 1 ? List.of(options[0]) : java.util.Arrays.asList(options));
        send("selectOptions", params);
    }

    @Override
    public void selectOptions(Locator locator, Object... options) {
        if (options == null || options.length == 0) return;
        Map<String, Object> params = locatorParams(locator);
        params.put("options", options.length == 1 ? List.of(options[0]) : java.util.Arrays.asList(options));
        send("selectOptions", params);
    }

    @Override
    public void deselectOptions(String selector, Object... options) {
        if (options == null || options.length == 0) return;
        Map<String, Object> params = new HashMap<>();
        params.put("selector", selector);
        params.put("options", options.length == 1 ? List.of(options[0]) : java.util.Arrays.asList(options));
        send("deselectOptions", params);
    }

    @Override
    public void deselectOptions(Locator locator, Object... options) {
        if (options == null || options.length == 0) return;
        Map<String, Object> params = locatorParams(locator);
        params.put("options", options.length == 1 ? List.of(options[0]) : java.util.Arrays.asList(options));
        send("deselectOptions", params);
    }

    private List<String> parseStringList(com.google.gson.JsonObject res) {
        if (!res.has("value") || res.get("value").isJsonNull()) return Collections.emptyList();
        var v = res.get("value");
        if (v.isJsonArray()) {
            List<String> list = new ArrayList<>();
            for (var e : v.getAsJsonArray()) list.add(e.isJsonNull() ? "" : e.getAsString());
            return list;
        }
        return Collections.emptyList();
    }

    @Override
    public List<String> getSelectedValues(String selector) {
        return parseStringList(send("getSelectedValues", Map.of("selector", selector)));
    }

    @Override
    public List<String> getSelectedValues(Locator locator) {
        return parseStringList(send("getSelectedValues", locatorParams(locator)));
    }

    @Override
    public List<String> getSelectedLabels(String selector) {
        return parseStringList(send("getSelectedLabels", Map.of("selector", selector)));
    }

    @Override
    public List<String> getSelectedLabels(Locator locator) {
        return parseStringList(send("getSelectedLabels", locatorParams(locator)));
    }

    @Override
    public void check(String selector) {
        send("check", Map.of("selector", selector));
    }

    @Override
    public void check(Locator locator) {
        send("check", locatorParams(locator));
    }

    @Override
    public void uncheck(String selector) {
        send("uncheck", Map.of("selector", selector));
    }

    @Override
    public void uncheck(Locator locator) {
        send("uncheck", locatorParams(locator));
    }

    private Map<String, Object> locatorParams(Locator locator) {
        Map<String, Object> params = new HashMap<>();
        params.put("selector", locator.getResolvedSelector());
        if (locator.getIndex() != null) params.put("index", locator.getIndex());
        if (locator.isXPath()) params.put("isXPath", true);
        return params;
    }

    @Override
    public void waitForSelector(String selector, Integer timeoutMs) {
        if (timeoutMs != null) {
            send("waitForSelector", Map.of("selector", selector, "timeout", timeoutMs));
        } else {
            send("waitForSelector", Map.of("selector", selector));
        }
    }

    @Override
    public void waitForSelector(Locator locator, Integer timeoutMs) {
        Map<String, Object> params = locatorParams(locator);
        if (timeoutMs != null) params.put("timeout", timeoutMs);
        send("waitForSelector", params);
    }

    @Override
    public void waitForURL(String urlOrPattern, Integer timeoutMs) {
        if (timeoutMs != null) {
            send("waitForURL", Map.of("urlOrPattern", urlOrPattern, "timeout", timeoutMs));
        } else {
            send("waitForURL", Map.of("urlOrPattern", urlOrPattern));
        }
    }

    @Override
    public void waitForLoad() {
        send("waitForLoad", Map.of());
    }

    @Override
    public void waitForLoad(Integer timeoutMs) {
        if (timeoutMs != null) {
            send("waitForLoad", Map.of("timeout", timeoutMs));
        } else {
            send("waitForLoad", Map.of());
        }
    }

    @Override
    public void waitForPage(Integer timeoutMs) {
        if (timeoutMs != null) {
            send("waitForPage", Map.of("timeout", timeoutMs));
        } else {
            send("waitForPage", Map.of());
        }
    }

    @Override
    public void waitForNetworkLoad(Integer timeoutMs) {
        if (timeoutMs != null) {
            send("waitForNetworkLoad", Map.of("timeout", timeoutMs));
        } else {
            send("waitForNetworkLoad", Map.of());
        }
    }

    @Override
    public void waitForTime(long millis) {
        send("waitForTime", Map.of("millis", millis));
    }

    @Override
    public void scrollToPageTop() {
        send("scrollToPageTop", Map.of());
    }

    @Override
    public void scrollToPageBottom() {
        send("scrollToPageBottom", Map.of());
    }

    @Override
    public void scrollUp() {
        send("scrollUp", Map.of());
    }

    @Override
    public void scrollDown() {
        send("scrollDown", Map.of());
    }

    @Override
    public void scrollBy(int deltaX, int deltaY) {
        send("scrollBy", Map.of("deltaX", deltaX, "deltaY", deltaY));
    }

    @Override
    public void scrollToSelector(String selector) {
        send("scrollToSelector", Map.of("selector", selector));
    }

    @Override
    public void scrollToSelector(Locator locator) {
        send("scrollToSelector", locatorParams(locator));
    }

    @Override
    public void acceptNextAlert() {
        send("acceptNextAlert", Map.of());
    }

    @Override
    public void acceptNextAlert(String promptText) {
        send("acceptNextAlert", Map.of("promptText", promptText != null ? promptText : ""));
    }

    @Override
    public void dismissNextAlert() {
        send("dismissNextAlert", Map.of());
    }

    @Override
    public String getLastAlertMessage() {
        JsonObject res = send("getLastAlertMessage", Map.of());
        return res.has("value") ? res.get("value").getAsString() : "";
    }

    @Override
    public String url() {
        JsonObject res = send("url", Map.of());
        return res.has("value") ? res.get("value").getAsString() : "";
    }

    @Override
    public String content() {
        JsonObject res = send("content", Map.of());
        return res.has("value") ? res.get("value").getAsString() : "";
    }

    @Override
    public Object evaluate(String expression) {
        JsonObject res = send("evaluate", Map.of("expression", expression));
        return res.has("value") ? res.get("value") : null;
    }

    @Override
    public boolean isVisible(String selector) {
        JsonObject res = send("isVisible", Map.of("selector", selector));
        return res.has("value") && res.get("value").getAsBoolean();
    }

    @Override
    public boolean isVisible(Locator locator) {
        JsonObject res = send("isVisible", locatorParams(locator));
        return res.has("value") && res.get("value").getAsBoolean();
    }

    @Override
    public boolean isDisabled(String selector) {
        JsonObject res = send("isDisabled", Map.of("selector", selector));
        return res.has("value") && res.get("value").getAsBoolean();
    }

    @Override
    public boolean isDisabled(Locator locator) {
        JsonObject res = send("isDisabled", locatorParams(locator));
        return res.has("value") && res.get("value").getAsBoolean();
    }

    @Override
    public boolean isSelected(String selector) {
        JsonObject res = send("isSelected", Map.of("selector", selector));
        return res.has("value") && res.get("value").getAsBoolean();
    }

    @Override
    public boolean isSelected(Locator locator) {
        JsonObject res = send("isSelected", locatorParams(locator));
        return res.has("value") && res.get("value").getAsBoolean();
    }

    @Override
    public String getTextContent(String selector) {
        JsonObject res = send("getTextContent", Map.of("selector", selector));
        return res.has("value") ? res.get("value").getAsString() : "";
    }

    @Override
    public String getTextContent(Locator locator) {
        JsonObject res = send("getTextContent", locatorParams(locator));
        return res.has("value") ? res.get("value").getAsString() : "";
    }

    @Override
    public String getValue(String selector) {
        JsonObject res = send("getValue", Map.of("selector", selector));
        return res.has("value") ? res.get("value").getAsString() : "";
    }

    @Override
    public String getValue(Locator locator) {
        JsonObject res = send("getValue", locatorParams(locator));
        return res.has("value") ? res.get("value").getAsString() : "";
    }

    @Override
    public String getAttribute(String selector, String attrName) {
        JsonObject res = send("getAttribute", Map.of("selector", selector, "name", attrName));
        return res.has("value") && !res.get("value").isJsonNull() ? res.get("value").getAsString() : null;
    }

    @Override
    public String getAttribute(Locator locator, String attrName) {
        Map<String, Object> p = locatorParams(locator);
        p.put("name", attrName);
        JsonObject res = send("getAttribute", p);
        return res.has("value") && !res.get("value").isJsonNull() ? res.get("value").getAsString() : null;
    }

    @Override
    public boolean isEditable(String selector) {
        JsonObject res = send("isEditable", Map.of("selector", selector));
        return res.has("value") && res.get("value").getAsBoolean();
    }

    @Override
    public boolean isEditable(Locator locator) {
        JsonObject res = send("isEditable", locatorParams(locator));
        return res.has("value") && res.get("value").getAsBoolean();
    }

    @Override
    public int locatorCount(Locator locator) {
        JsonObject res = send("locatorCount", locatorParams(locator));
        return res.has("value") ? res.get("value").getAsInt() : 0;
    }

    @Override
    public String title() {
        JsonObject res = send("title", Map.of());
        return res.has("value") ? res.get("value").getAsString() : "";
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

    @Override
    public CSTestingBrowser frame(String iframeSelector) {
        JsonObject res = send("frame", Map.of("iframeSelector", iframeSelector));
        String newFrameId = res.has("frameId") ? res.get("frameId").getAsString() : null;
        return new CSTestingBrowserFrame(connection, newFrameId);
    }

    @Override
    public List<String> getWindowHandles() {
        JsonObject res = send("getWindowHandles", Map.of());
        if (!res.has("value")) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (var e : res.getAsJsonArray("value")) out.add(e.getAsString());
        return out;
    }

    @Override
    public String getCurrentWindowHandle() {
        JsonObject res = send("getCurrentWindowHandle", Map.of());
        return res.has("value") ? res.get("value").getAsString() : "";
    }

    @Override
    public void switchToWindow(String handle) {
        send("switchToWindow", Map.of("handle", handle != null ? handle : ""));
    }

    @Override
    public CSTestingBrowser newTab() {
        send("newTab", Map.of());
        return this;
    }

    @Override
    public java.util.List<CSTestingBrowser> getPages() {
        return java.util.Collections.singletonList(this);
    }

    @Override
    public CSTestingBrowser waitForNewTab(Integer timeoutMs) {
        int timeout = timeoutMs != null ? timeoutMs : 30_000;
        long deadline = System.currentTimeMillis() + timeout;
        java.util.Set<String> initial = new java.util.HashSet<>(getWindowHandles());
        while (System.currentTimeMillis() < deadline) {
            List<String> current = getWindowHandles();
            for (String h : current) {
                if (!initial.contains(h)) {
                    switchToWindow(h);
                    return this;
                }
            }
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
        }
        throw new RuntimeException("Timeout waiting for new tab within " + timeout + "ms");
    }

    @Override
    public byte[] getScreenshot() {
        return getScreenshot((ScreenshotOptions) null);
    }

    @Override
    public byte[] getScreenshot(ScreenshotOptions options) {
        Map<String, Object> params = new HashMap<>();
        if (options != null) {
            if (options.getPath() != null) params.put("path", options.getPath());
            if (options.isFullPage()) params.put("fullPage", true);
            if (options.getSelector() != null) params.put("selector", options.getSelector());
            if (options.getLocator() != null) {
                params.put("selector", options.getLocator().getResolvedSelector());
                if (options.getLocator().getIndex() != null) params.put("index", options.getLocator().getIndex());
            }
            if (options.getFormat() != null) params.put("format", options.getFormat());
            if (options.getQuality() != null) params.put("quality", options.getQuality());
        }
        JsonObject res = send("getScreenshot", params);
        String base64 = res.has("value") ? res.get("value").getAsString() : (res.has("bytes") ? res.get("bytes").getAsString() : null);
        if (base64 == null) throw new RuntimeException("getScreenshot did not return value or bytes");
        byte[] bytes = java.util.Base64.getDecoder().decode(base64);
        if (options != null && options.getPath() != null) {
            try { java.nio.file.Files.write(java.nio.file.Paths.get(options.getPath()), bytes); } catch (Exception e) { throw new RuntimeException("Failed to write screenshot to " + options.getPath(), e); }
        }
        return bytes;
    }

    @Override
    public void close() {
        send("close", Map.of());
    }
}
