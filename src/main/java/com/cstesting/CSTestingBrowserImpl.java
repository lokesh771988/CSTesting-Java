package com.cstesting;

import com.cstesting.protocol.ServerConnection;
import com.cstesting.protocol.ServerStarter;
import com.cstesting.protocol.WebSocketServerConnection;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.HashMap;
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
        Map<String, Object> params = new HashMap<>();
        params.put("selector", selector);
        if (timeoutMs != null) params.put("timeout", timeoutMs);
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
        String frameId = res.has("frameId") ? res.get("frameId").getAsString() : null;
        return new CSTestingBrowserFrame(this.connection, frameId);
    }

    @Override
    public void close() {
        send("close", Map.of());
        connection.close();
    }
}
