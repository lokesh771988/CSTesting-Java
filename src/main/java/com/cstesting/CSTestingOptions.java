package com.cstesting;

/**
 * Options for creating a CSTesting browser session.
 * Either start the Node server automatically or connect to an existing server.
 */
public final class CSTestingOptions {

    private final boolean headless;
    private final int port;
    private final String serverUrl;

    private CSTestingOptions(Builder b) {
        this.headless = b.headless;
        this.port = b.port;
        this.serverUrl = b.serverUrl;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isHeadless() {
        return headless;
    }

    public int getPort() {
        return port;
    }

    /** If set, connect to this URL instead of starting the server (e.g. "ws://localhost:9274"). */
    public String getServerUrl() {
        return serverUrl;
    }

    public static final class Builder {
        private boolean headless = true;
        private int port = 9274;
        private String serverUrl;

        public Builder headless(boolean headless) {
            this.headless = headless;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder serverUrl(String serverUrl) {
            this.serverUrl = serverUrl;
            return this;
        }

        public CSTestingOptions build() {
            return new CSTestingOptions(this);
        }
    }
}
