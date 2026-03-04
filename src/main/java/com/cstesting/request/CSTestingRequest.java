package com.cstesting.request;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

/**
 * Fluent API for HTTP requests (e.g. API tests). Use {@link #get(String)} or {@link #post(String, String)}
 * then chain {@link #expectStatus(int)}, {@link #expectHeader(String, String)}, {@link #expectBodyContains(String)},
 * and optionally {@link #body()} to get the response body.
 * <p>
 * Example:
 * <pre>
 * CSTestingRequest.get("https://api.example.com/users")
 *     .expectStatus(200)
 *     .expectHeader("Content-Type", "application/json")
 *     .expectBodyContains("\"id\"");
 * String body = CSTestingRequest.get("https://api.example.com/health").expectStatus(200).body();
 * </pre>
 */
public final class CSTestingRequest {

    private static final HttpClient DEFAULT_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private final HttpClient client;
    private final HttpRequest request;
    private HttpResponse<String> response;
    private String body;

    private CSTestingRequest(HttpClient client, HttpRequest request) {
        this.client = client != null ? client : DEFAULT_CLIENT;
        this.request = request;
    }

    /**
     * Start a GET request. Call {@link #expectStatus(int)} and/or {@link #body()} to execute and assert.
     */
    public static CSTestingRequest get(String url) {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();
        return new CSTestingRequest(null, req);
    }

    /**
     * Start a POST request with a body (e.g. JSON). Call {@link #expectStatus(int)} and/or {@link #body()} to execute and assert.
     */
    public static CSTestingRequest post(String url, String body) {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body != null ? body : ""))
            .build();
        return new CSTestingRequest(null, req);
    }

    /**
     * Use a custom HTTP client (e.g. with custom timeouts or redirect policy).
     */
    public CSTestingRequest withClient(HttpClient client) {
        return new CSTestingRequest(client, request);
    }

    private HttpResponse<String> getResponse() {
        if (response == null) {
            try {
                response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
                body = response.body();
            } catch (Exception e) {
                throw new RuntimeException("Request failed: " + e.getMessage(), e);
            }
        }
        return response;
    }

    /**
     * Assert response status code. Throws AssertionError if not equal. Executes the request if not yet sent.
     */
    public CSTestingRequest expectStatus(int expected) {
        int actual = getResponse().statusCode();
        if (actual != expected) {
            throw new AssertionError("Expected status " + expected + " but got " + actual + ". Body: " + (body != null && body.length() > 500 ? body.substring(0, 500) + "..." : body));
        }
        return this;
    }

    /**
     * Assert that a response header has the given value (after trimming). Case-insensitive header name.
     */
    public CSTestingRequest expectHeader(String name, String expectedValue) {
        String actual = getResponse().headers().firstValue(name).map(String::trim).orElse(null);
        if (!Objects.equals(expectedValue, actual)) {
            throw new AssertionError("Expected header " + name + " = \"" + expectedValue + "\" but got \"" + actual + "\"");
        }
        return this;
    }

    /**
     * Assert that the response body contains the given substring. Executes the request if not yet sent.
     */
    public CSTestingRequest expectBodyContains(String substring) {
        String b = getResponse().body();
        if (b == null || !b.contains(substring)) {
            throw new AssertionError("Expected body to contain \"" + substring + "\" but got: " + (b != null && b.length() > 200 ? b.substring(0, 200) + "..." : b));
        }
        return this;
    }

    /**
     * Return the response body. Executes the request if not yet sent.
     */
    public String body() {
        return getResponse().body();
    }

    /**
     * Return the response status code. Executes the request if not yet sent.
     */
    public int statusCode() {
        return getResponse().statusCode();
    }
}
