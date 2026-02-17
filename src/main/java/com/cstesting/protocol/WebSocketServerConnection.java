package com.cstesting.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket implementation of ServerConnection.
 * Connects to ws://host:port and sends JSON requests; responses are matched by id.
 */
final class WebSocketServerConnection implements ServerConnection {

    private static final Gson GSON = new Gson();
    private static final long RESPONSE_TIMEOUT_MS = 60_000;

    private final WebSocketClient client;
    private final BlockingQueue<JsonObject> responseQueue = new LinkedBlockingQueue<>();

    /**
     * Connect to the given WebSocket URL (e.g. ws://localhost:9274).
     */
    static ServerConnection connect(String serverUrl) {
        URI uri = URI.create(serverUrl.startsWith("ws") ? serverUrl : "ws://" + serverUrl);
        WebSocketServerConnection conn = new WebSocketServerConnection(uri);
        conn.connect();
        return conn;
    }

    WebSocketServerConnection(URI serverUri) {
        this.client = new WebSocketClient(serverUri) {
            @Override
            public void onMessage(String message) {
                JsonObject obj = GSON.fromJson(message, JsonObject.class);
                responseQueue.offer(obj);
            }

            @Override
            public void onOpen(ServerHandshake handshake) {}

            @Override
            public void onClose(int code, String reason, boolean remote) {}

            @Override
            public void onError(Exception ex) {}
        };
    }

    void connect() {
        try {
            client.connectBlocking();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while connecting to CSTesting server", e);
        }
    }

    @Override
    public JsonObject send(JsonObject request) {
        long id = request.has("id") ? request.get("id").getAsLong() : 0;
        client.send(request.toString());
        try {
            JsonObject response = responseQueue.poll(RESPONSE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (response == null) {
                throw new RuntimeException("Timeout waiting for response to method " + request.get("method").getAsString());
            }
            if (response.has("error")) {
                throw new RuntimeException("CSTesting server error: " + response.get("error").getAsString());
            }
            return response.has("result") ? response.getAsJsonObject("result") : new JsonObject();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted waiting for response", e);
        }
    }

    @Override
    public void close() {
        client.close();
    }
}
