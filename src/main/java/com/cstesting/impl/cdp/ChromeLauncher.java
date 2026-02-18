package com.cstesting.impl.cdp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.io.File;
import com.google.gson.JsonArray;

/**
 * Launches Chrome with remote debugging and returns the CDP WebSocket URL.
 * Uses system Chrome (no Playwright, no npx). Set cstesting.chrome.path to override Chrome location.
 */
final class ChromeLauncher {

    private static final Gson GSON = new Gson();
    private static final int DEBUG_PORT = 9222;

    static LaunchResult launch(boolean headless) {
        String chromePath = System.getProperty("cstesting.chrome.path");
        if (chromePath == null || chromePath.isEmpty()) {
            chromePath = findChrome();
        }
        if (chromePath == null) {
            throw new RuntimeException(
                "Chrome not found. Install Chrome or set -Dcstesting.chrome.path=/path/to/chrome");
        }

        List<String> args = new ArrayList<>();
        args.add(chromePath);
        args.add("--remote-debugging-port=" + DEBUG_PORT);
        args.add("--no-first-run");
        args.add("--no-default-browser-check");
        args.add("--disable-background-networking");
        args.add("--disable-sync");
        args.add("--disable-translate");
        args.add("--disable-extensions");
        args.add("--disable-popup-blocking");   // allow new tabs/windows opened by script or clicks
        args.add("--metrics-recording-only");
        args.add("--mute-audio");
        try {
            File tmpDir = File.createTempFile("cstesting-chrome-", "");
            tmpDir.delete();
            tmpDir.mkdirs();
            args.add("--user-data-dir=" + tmpDir.getAbsolutePath());
        } catch (Exception ignored) {}
        if (headless) {
            args.add("--headless=new");
            args.add("--disable-gpu");
            args.add("--window-size=1280,720");
        }

        ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectErrorStream(true);
        Process process;
        try {
            process = pb.start();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start Chrome: " + e.getMessage(), e);
        }

        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new RuntimeException("Interrupted", e);
        }
        String wsUrl = waitForWebSocketDebuggerUrl(DEBUG_PORT);
        return new LaunchResult(process, URI.create(wsUrl));
    }

    private static String findChrome() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            String[] paths = {
                System.getenv("LOCALAPPDATA") + "\\Google\\Chrome\\Application\\chrome.exe",
                "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe"
            };
            for (String p : paths) {
                if (p != null && Files.isRegularFile(Paths.get(p))) return p;
            }
        } else if (os.contains("mac")) {
            Path p = Paths.get("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
            if (Files.isRegularFile(p)) return p.toString();
        } else {
            for (String name : new String[]{"google-chrome", "google-chrome-stable", "chromium", "chromium-browser"}) {
                try {
                    Process pr = new ProcessBuilder("which", name).start();
                    if (pr.waitFor() == 0) {
                        try (BufferedReader r = new BufferedReader(new InputStreamReader(pr.getInputStream(), StandardCharsets.UTF_8))) {
                            String line = r.readLine();
                            if (line != null && !line.isEmpty()) return line.trim();
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private static String waitForWebSocketDebuggerUrl(int port) {
        long deadline = System.currentTimeMillis() + 25_000;
        while (System.currentTimeMillis() < deadline) {
            try {
                String pageWsUrl = getPageWebSocketUrl(port);
                if (pageWsUrl != null) return pageWsUrl;
            } catch (Exception ignored) {}
            try {
                TimeUnit.MILLISECONDS.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted", e);
            }
        }
        throw new RuntimeException(
            "Chrome did not expose CDP at port " + port + " in time. "
            + "Ensure Chrome is installed. Override with -Dcstesting.chrome.path=/path/to/chrome");
    }

    /** Returns all page targets' WebSocket URLs from /json/list (for window/tab handling). */
    static List<String> getAllPageWebSocketUrls(int port) {
        List<String> out = new ArrayList<>();
        for (JsonObject t : getPageTargets(port)) {
            if (t.has("webSocketDebuggerUrl"))
                out.add(t.get("webSocketDebuggerUrl").getAsString());
        }
        return out;
    }

    /** Returns page targets from /json/list (each has id, webSocketDebuggerUrl, type, etc.). */
    static List<JsonObject> getPageTargets(int port) {
        List<JsonObject> out = new ArrayList<>();
        try {
            String listUrl = "http://127.0.0.1:" + port + "/json/list";
            HttpURLConnection conn = (HttpURLConnection) new URL(listUrl).openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() != 200) return out;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
                JsonArray arr = GSON.fromJson(sb.toString(), JsonArray.class);
                if (arr != null) {
                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject target = arr.get(i).getAsJsonObject();
                        String type = target.has("type") ? target.get("type").getAsString() : "";
                        if ("page".equals(type)) out.add(target);
                    }
                }
            }
        } catch (Exception ignored) {}
        return out;
    }

    /** Prefer page target from /json/list so Page.navigate works. Fallback to /json/version. */
    private static String getPageWebSocketUrl(int port) throws Exception {
        String listUrl = "http://127.0.0.1:" + port + "/json/list";
        HttpURLConnection conn = (HttpURLConnection) new URL(listUrl).openConnection();
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(2000);
        conn.setRequestMethod("GET");
        if (conn.getResponseCode() != 200) return null;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            JsonArray arr = GSON.fromJson(sb.toString(), JsonArray.class);
            if (arr != null) {
                for (int i = 0; i < arr.size(); i++) {
                    JsonObject target = arr.get(i).getAsJsonObject();
                    String type = target.has("type") ? target.get("type").getAsString() : "";
                    if ("page".equals(type) && target.has("webSocketDebuggerUrl")) {
                        return target.get("webSocketDebuggerUrl").getAsString();
                    }
                }
                if (arr.size() > 0 && arr.get(0).getAsJsonObject().has("webSocketDebuggerUrl")) {
                    return arr.get(0).getAsJsonObject().get("webSocketDebuggerUrl").getAsString();
                }
            }
        }
        String versionUrl = "http://127.0.0.1:" + port + "/json/version";
        conn = (HttpURLConnection) new URL(versionUrl).openConnection();
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(2000);
        conn.setRequestMethod("GET");
        if (conn.getResponseCode() != 200) return null;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            JsonObject obj = GSON.fromJson(sb.toString(), JsonObject.class);
            return obj != null && obj.has("webSocketDebuggerUrl") ? obj.get("webSocketDebuggerUrl").getAsString() : null;
        }
    }

    static final class LaunchResult {
        final Process process;
        final URI webSocketUrl;

        LaunchResult(Process process, URI webSocketUrl) {
            this.process = process;
            this.webSocketUrl = webSocketUrl;
        }
    }
}
