package com.cstesting.protocol;

import com.google.gson.JsonObject;

/**
 * Low-level connection to the CSTesting Node server (WebSocket or HTTP).
 * Sends JSON request, returns JSON result or throws on error.
 */
public interface ServerConnection {

    /**
     * Send a request and wait for the response. Request must have "id" and "method".
     * Returns the "result" object, or throws if the response has "error".
     */
    JsonObject send(JsonObject request);

    void close();
}
