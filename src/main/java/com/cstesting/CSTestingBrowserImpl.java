package com.cstesting;

import com.cstesting.protocol.ServerConnection;
import com.cstesting.protocol.ServerStarter;
import com.cstesting.protocol.WebSocketServerConnection;
import com.cstesting.Locator;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation that sends JSON commands to the CSTesting Node server.
 * Server must be running (e.g. npx cstesting server --port=9274) or started by startServer().
 */
final class CSTestingBrowserImpl implements CSTestingBrowser {

    private static final Gson GSON = new Gson();
    private final ServerConnection connection;
    private final AtomicLong requestId = new AtomicLong(0);

    CSTestingBrowserImpl(ServerConnection connection) {
        this.connection = connection;
    }

    static CSTestingBrowser connect(String serverUrl) {
        ServerConnection conn = WebSocketServerConnection.connect(serverUrl);
        return new CSTestingBrowserImpl(conn);
    }

    static CSTestingBrowser startServer(CSTestingOptions options) {
        ServerConnection conn = ServerStarter.start(options.getPort(), options.isHeadless());
        return new CSTestingBrowserImpl(conn);
    }

    private JsonObject send(String method, Map<String, Object> params) {
        long id = requestId.incrementAndGet();
        JsonObject req = new JsonObject();
        req.addProperty("id", id);
        req.addProperty("method", method);
        if (params != null && !params.isEmpty()) {
            req.add("params", GSON.toJsonTree(params));
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
        sendLocator("click", locator, Map.of());
    }

    @Override
    public void hover(String selector) {
        send("hover", Map.of("selector", selector));
    }

    @Override
    public void hover(Locator locator) {
        sendLocator("hover", locator, Map.of());
    }

    @Override
    public void doubleClick(String selector) {
        send("doubleClick", Map.of("selector", selector));
    }

    @Override
    public void doubleClick(Locator locator) {
        sendLocator("doubleClick", locator, Map.of());
    }

    @Override
    public void rightClick(String selector) {
        send("rightClick", Map.of("selector", selector));
    }

    @Override
    public void rightClick(Locator locator) {
        sendLocator("rightClick", locator, Map.of());
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
        Map<String, Object> params = new HashMap<>();
        params.put("text", text);
        sendLocator("type", locator, params);
    }

    @Override
    public void select(String selector, Object option) {
        send("select", Map.of("selector", selector, "option", option));
    }

    @Override
    public void select(Locator locator, Object option) {
        sendLocator("select", locator, Map.of("option", option));
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
        Map<String, Object> params = new HashMap<>(locatorParams(locator));
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
        Map<String, Object> params = new HashMap<>(locatorParams(locator));
        params.put("options", options.length == 1 ? List.of(options[0]) : java.util.Arrays.asList(options));
        send("deselectOptions", params);
    }

    private Map<String, Object> locatorParams(Locator locator) {
        Map<String, Object> params = new HashMap<>();
        params.put("selector", locator.getResolvedSelector());
        if (locator.getIndex() != null) params.put("index", locator.getIndex());
        if (locator.isXPath()) params.put("isXPath", true);
        return params;
    }

    private List<String> parseStringList(JsonObject res) {
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
        JsonObject res = send("getSelectedValues", Map.of("selector", selector));
        return parseStringList(res);
    }

    @Override
    public List<String> getSelectedValues(Locator locator) {
        JsonObject res = sendLocatorResult("getSelectedValues", locator);
        return parseStringList(res);
    }

    @Override
    public List<String> getSelectedLabels(String selector) {
        JsonObject res = send("getSelectedLabels", Map.of("selector", selector));
        return parseStringList(res);
    }

    @Override
    public List<String> getSelectedLabels(Locator locator) {
        JsonObject res = sendLocatorResult("getSelectedLabels", locator);
        return parseStringList(res);
    }

    @Override
    public void check(String selector) {
        send("check", Map.of("selector", selector));
    }

    @Override
    public void check(Locator locator) {
        sendLocator("check", locator, Map.of());
    }

    @Override
    public void uncheck(String selector) {
        send("uncheck", Map.of("selector", selector));
    }

    @Override
    public void uncheck(Locator locator) {
        sendLocator("uncheck", locator, Map.of());
    }

    private void sendLocator(String method, Locator locator, Map<String, Object> extra) {
        Map<String, Object> params = new HashMap<>(extra);
        params.put("selector", locator.getResolvedSelector());
        if (locator.getIndex() != null) params.put("index", locator.getIndex());
        if (locator.isXPath()) params.put("isXPath", true);
        send(method, params);
    }

    @Override
    public void waitForSelector(String selector, Integer timeoutMs) {
        Map<String, Object> params = new HashMap<>();
        params.put("selector", selector);
        if (timeoutMs != null) params.put("timeout", timeoutMs);
        send("waitForSelector", params);
    }

    @Override
    public void waitForSelector(Locator locator, Integer timeoutMs) {
        Map<String, Object> params = new HashMap<>();
        params.put("selector", locator.getResolvedSelector());
        if (timeoutMs != null) params.put("timeout", timeoutMs);
        if (locator.getIndex() != null) params.put("index", locator.getIndex());
        if (locator.isXPath()) params.put("isXPath", true);
        send("waitForSelector", params);
    }

    @Override
    public void waitForURL(String urlOrPattern, Integer timeoutMs) {
        Map<String, Object> params = new HashMap<>();
        params.put("urlOrPattern", urlOrPattern);
        if (timeoutMs != null) params.put("timeout", timeoutMs);
        send("waitForURL", params);
    }

    @Override
    public void waitForLoad() {
        send("waitForLoad", Map.of());
    }

    @Override
    public void waitForLoad(Integer timeoutMs) {
        Map<String, Object> params = new HashMap<>();
        if (timeoutMs != null) params.put("timeout", timeoutMs);
        send("waitForLoad", params);
    }

    @Override
    public void waitForPage(Integer timeoutMs) {
        Map<String, Object> params = new HashMap<>();
        if (timeoutMs != null) params.put("timeout", timeoutMs);
        send("waitForPage", params);
    }

    @Override
    public void waitForNetworkLoad(Integer timeoutMs) {
        Map<String, Object> params = new HashMap<>();
        if (timeoutMs != null) params.put("timeout", timeoutMs);
        send("waitForNetworkLoad", params);
    }

    @Override
    public void waitForTime(long millis) {
        Map<String, Object> params = new HashMap<>();
        params.put("millis", millis);
        send("waitForTime", params);
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
        sendLocator("scrollToSelector", locator, Map.of());
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
        JsonObject res = sendLocatorResult("isVisible", locator);
        return res.has("value") && res.get("value").getAsBoolean();
    }

    @Override
    public boolean isDisabled(String selector) {
        JsonObject res = send("isDisabled", Map.of("selector", selector));
        return res.has("value") && res.get("value").getAsBoolean();
    }

    @Override
    public boolean isDisabled(Locator locator) {
        JsonObject res = sendLocatorResult("isDisabled", locator);
        return res.has("value") && res.get("value").getAsBoolean();
    }

    @Override
    public boolean isSelected(String selector) {
        JsonObject res = send("isSelected", Map.of("selector", selector));
        return res.has("value") && res.get("value").getAsBoolean();
    }

    @Override
    public boolean isSelected(Locator locator) {
        JsonObject res = sendLocatorResult("isSelected", locator);
        return res.has("value") && res.get("value").getAsBoolean();
    }

    @Override
    public String getTextContent(String selector) {
        JsonObject res = send("getTextContent", Map.of("selector", selector));
        return res.has("value") ? res.get("value").getAsString() : "";
    }

    @Override
    public String getTextContent(Locator locator) {
        JsonObject res = sendLocatorResult("getTextContent", locator);
        return res.has("value") ? res.get("value").getAsString() : "";
    }

    @Override
    public String getValue(String selector) {
        JsonObject res = send("getValue", Map.of("selector", selector));
        return res.has("value") ? res.get("value").getAsString() : "";
    }

    @Override
    public String getValue(Locator locator) {
        JsonObject res = sendLocatorResult("getValue", locator);
        return res.has("value") ? res.get("value").getAsString() : "";
    }

    @Override
    public String getAttribute(String selector, String attrName) {
        JsonObject res = send("getAttribute", Map.of("selector", selector, "name", attrName));
        return res.has("value") && !res.get("value").isJsonNull() ? res.get("value").getAsString() : null;
    }

    @Override
    public String getAttribute(Locator locator, String attrName) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("name", attrName);
        JsonObject res = sendLocatorResult("getAttribute", locator, extra);
        return res.has("value") && !res.get("value").isJsonNull() ? res.get("value").getAsString() : null;
    }

    @Override
    public boolean isEditable(String selector) {
        JsonObject res = send("isEditable", Map.of("selector", selector));
        return res.has("value") && res.get("value").getAsBoolean();
    }

    @Override
    public boolean isEditable(Locator locator) {
        JsonObject res = sendLocatorResult("isEditable", locator);
        return res.has("value") && res.get("value").getAsBoolean();
    }

    @Override
    public int locatorCount(Locator locator) {
        JsonObject res = sendLocatorResult("locatorCount", locator);
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

    private JsonObject sendLocatorResult(String method, Locator locator) {
        Map<String, Object> params = new HashMap<>();
        params.put("selector", locator.getResolvedSelector());
        if (locator.getIndex() != null) params.put("index", locator.getIndex());
        if (locator.isXPath()) params.put("isXPath", true);
        return send(method, params);
    }

    private JsonObject sendLocatorResult(String method, Locator locator, Map<String, Object> extra) {
        Map<String, Object> params = new HashMap<>(extra);
        params.put("selector", locator.getResolvedSelector());
        if (locator.getIndex() != null) params.put("index", locator.getIndex());
        if (locator.isXPath()) params.put("isXPath", true);
        return send(method, params);
    }

    @Override
    public Locator locator(String selector) {
        return new Locator(this, selector);
    }

    @Override
    public CSTestingBrowser frame(String iframeSelector) {
        JsonObject res = send("frame", Map.of("iframeSelector", iframeSelector));
        String frameId = res.has("frameId") ? res.get("frameId").getAsString() : null;
        return new CSTestingBrowserFrame(this.connection, frameId);
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
    public void close() {
        send("close", Map.of());
        connection.close();
    }
}
