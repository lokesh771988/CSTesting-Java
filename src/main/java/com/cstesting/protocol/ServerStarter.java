package com.cstesting.protocol;

import com.cstesting.impl.driver.Driver;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * Starts the CSTesting server using the configured Driver (Playwright-style).
 * Uses bundled/preinstalled driver if cstesting.cli.dir is set, otherwise system npx.
 */
public final class ServerStarter {

    public static ServerConnection start(int port, boolean headless) {
        ProcessBuilder pb = Driver.ensureDriver().createProcessBuilder(port, headless);
        pb.redirectErrorStream(true);
        try {
            pb.start();
        } catch (IOException e) {
            throw new RuntimeException(
                "Failed to start CSTesting server. "
                + "If using system driver: is Node.js installed and on PATH? Set cstesting.cli.dir to use a bundled driver. "
                + "Or use CSTestingOptions.serverUrl() to connect to an existing server.",
                e);
        }
        String wsUrl = "ws://localhost:" + port;
        waitForServer(port);
        WebSocketServerConnection conn = new WebSocketServerConnection(URI.create(wsUrl));
        conn.connect();
        return conn;
    }

    private static void waitForServer(int port) {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            try {
                new java.net.Socket("localhost", port).close();
                return;
            } catch (IOException ignored) {
                try {
                    TimeUnit.MILLISECONDS.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for server", e);
                }
            }
        }
        throw new RuntimeException("CSTesting server did not start on port " + port + " within 30s");
    }
}
