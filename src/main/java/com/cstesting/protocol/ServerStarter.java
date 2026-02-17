package com.cstesting.protocol;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Starts the CSTesting Node server (npx cstesting server --port=...) and waits until it is listening.
 */
public final class ServerStarter {

    public static ServerConnection start(int port, boolean headless) {
        ProcessBuilder pb = new ProcessBuilder(nodeCommand(port, headless));
        pb.redirectErrorStream(true);
        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start CSTesting server. Is Node.js installed and on PATH?", e);
        }
        String wsUrl = "ws://localhost:" + port;
        waitForServer(port);
        WebSocketServerConnection conn = new WebSocketServerConnection(URI.create(wsUrl));
        conn.connect();
        return conn;
    }

    private static List<String> nodeCommand(int port, boolean headless) {
        List<String> cmd = new ArrayList<>();
        cmd.add("npx");
        cmd.add("cstesting");
        cmd.add("server");
        cmd.add("--port=" + port);
        if (!headless) {
            cmd.add("--no-headless");
        }
        return cmd;
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
