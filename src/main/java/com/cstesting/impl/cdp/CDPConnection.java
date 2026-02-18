package com.cstesting.impl.cdp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Chrome DevTools Protocol connection over WebSocket.
 * Sends CDP commands and returns the matching response by id.
 */
final class CDPConnection {

    private static final Gson GSON = new Gson();
    private static final long TIMEOUT_MS = 30_000;

    private final WebSocketClient client;
    private final BlockingQueue<JsonObject> responseQueue = new LinkedBlockingQueue<>();
    private final AtomicLong idGen = new AtomicLong(1);

    CDPConnection(URI wsUri) throws InterruptedException {
        this.client = new WebSocketClient(wsUri) {
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
        client.connectBlocking();
    }

    JsonObject send(String method, JsonObject params) {
        long id = idGen.incrementAndGet();
        JsonObject req = new JsonObject();
        req.addProperty("id", id);
        req.addProperty("method", method);
        if (params != null && !params.entrySet().isEmpty()) {
            req.add("params", params);
        }
        client.send(req.toString());
        try {
            long deadline = System.currentTimeMillis() + TIMEOUT_MS;
            java.util.List<JsonObject> pending = new java.util.ArrayList<>();
            while (System.currentTimeMillis() < deadline) {
                JsonObject response = responseQueue.poll(2, TimeUnit.SECONDS);
                if (response != null && response.has("id") && response.get("id").getAsLong() == id) {
                    pending.forEach(responseQueue::offer);
                    if (response.has("error")) {
                        throw new RuntimeException("CDP error: " + response.get("error").getAsJsonObject().get("message").getAsString());
                    }
                    return response.has("result") ? response.getAsJsonObject("result") : new JsonObject();
                }
                if (response != null) pending.add(response);
            }
            pending.forEach(responseQueue::offer);
            throw new RuntimeException("CDP timeout for " + method);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted", e);
        }
    }

    void close() {
        client.close();
    }
}
