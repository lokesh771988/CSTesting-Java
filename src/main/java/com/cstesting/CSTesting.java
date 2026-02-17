package com.cstesting;

/**
 * Entry point for CSTesting Java client.
 * Creates a browser that either starts the Node server (via npx cstesting server)
 * or connects to an existing server URL.
 */
public final class CSTesting {

    private CSTesting() {}

    /**
     * Create a browser with default options (headless, auto-start server on port 9274).
     */
    public static CSTestingBrowser createBrowser() {
        return createBrowser(CSTestingOptions.builder().build());
    }

    /**
     * Create a browser with the given options.
     * If serverUrl is set, connects to that server; otherwise starts "npx cstesting server --port=&lt;port&gt;".
     */
    public static CSTestingBrowser createBrowser(CSTestingOptions options) {
        if (options.getServerUrl() != null && !options.getServerUrl().isEmpty()) {
            return CSTestingBrowserImpl.connect(options.getServerUrl());
        }
        return CSTestingBrowserImpl.startServer(options);
    }
}
