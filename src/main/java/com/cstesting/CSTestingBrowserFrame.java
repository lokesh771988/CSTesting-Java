package com.cstesting;

import com.cstesting.protocol.ServerConnection;
import com.google.gson.JsonObject;

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
    public void click(String selector) {
        send("click", Map.of("selector", selector));
    }

    @Override
    public void type(String selector, String text) {
        send("type", Map.of("selector", selector, "text", text));
    }

    @Override
    public void select(String selector, Object option) {
        send("select", Map.of("selector", selector, "option", option));
    }

    @Override
    public void check(String selector) {
        send("check", Map.of("selector", selector));
    }

    @Override
    public void uncheck(String selector) {
        send("uncheck", Map.of("selector", selector));
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
    public boolean isDisabled(String selector) {
        JsonObject res = send("isDisabled", Map.of("selector", selector));
        return res.has("value") && res.get("value").getAsBoolean();
    }

    @Override
    public boolean isSelected(String selector) {
        JsonObject res = send("isSelected", Map.of("selector", selector));
        return res.has("value") && res.get("value").getAsBoolean();
    }

    @Override
    public String getTextContent(String selector) {
        JsonObject res = send("getTextContent", Map.of("selector", selector));
        return res.has("value") ? res.get("value").getAsString() : "";
    }

    @Override
    public CSTestingBrowser frame(String iframeSelector) {
        JsonObject res = send("frame", Map.of("iframeSelector", iframeSelector));
        String newFrameId = res.has("frameId") ? res.get("frameId").getAsString() : null;
        return new CSTestingBrowserFrame(connection, newFrameId);
    }

    @Override
    public void close() {
        send("close", Map.of());
    }
}
