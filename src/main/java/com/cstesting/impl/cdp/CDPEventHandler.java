package com.cstesting.impl.cdp;

import com.google.gson.JsonObject;

/**
 * Handler for CDP events (messages with "method", no "id").
 */
interface CDPEventHandler {
    void onEvent(CDPConnection connection, JsonObject event);
}
