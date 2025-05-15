package de.aivot.GoverBackend.core.services;

import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for handling HTTP requests and responses.
 *
 * <p>This service provides utility methods for sending HTTP GET and POST requests,
 * including support for form URL-encoded POST requests. It simplifies the process
 * of building and sending HTTP requests, as well as handling responses and testing
 * classes relying on HTTP requests.</p>
 *
 * <p>Key functionalities:</p>
 * <ul>
 *     <li>Sends HTTP GET requests with optional headers.</li>
 *     <li>Sends HTTP POST requests with a raw string body and optional headers.</li>
 *     <li>Sends HTTP POST requests with form URL-encoded data and optional headers.</li>
 *     <li>Uses Java's {@link HttpClient} for handling HTTP communication.</li>
 *     <li>Handles exceptions during the HTTP request process by propagating them to the caller.</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 *     HttpService httpService = new HttpService();
 *     URI uri = new URI("https://example.com/api");
 *     HttpResponse&lt;String&gt; response = httpService.get(uri, Map.of("Authorization", "Bearer token"));
 * </pre>
 *
 * @see HttpClient
 * @see HttpRequest
 * @see HttpResponse
 */
@Service
public class HttpService {
    /**
     * Sends an HTTP GET request to the specified URI without any headers.
     *
     * @param uri The target URI for the GET request.
     * @return The HTTP response as a {@link HttpResponse} object.
     * @throws IOException          If an I/O error occurs during the request.
     * @throws InterruptedException If the operation is interrupted.
     */
    @Nonnull
    public HttpResponse<String> get(
            @Nonnull URI uri
    ) throws IOException, InterruptedException {
        return get(uri, null);
    }

    /**
     * Sends an HTTP GET request to the specified URI with optional headers.
     *
     * @param uri     The target URI for the GET request.
     * @param headers A map of headers to include in the request, or null if no headers are needed.
     * @return The HTTP response as a {@link HttpResponse} object.
     * @throws IOException          If an I/O error occurs during the request.
     * @throws InterruptedException If the operation is interrupted.
     */
    @Nonnull
    public HttpResponse<String> get(
            @Nonnull URI uri,
            @Nullable Map<String, String> headers
    ) throws IOException, InterruptedException {
        var requestBuilder = HttpRequest
                .newBuilder()
                .uri(uri)
                .GET();

        if (headers != null) {
            headers
                    .forEach(requestBuilder::header);
        }

        var request = requestBuilder
                .build();

        HttpResponse<String> response;
        try (var client = HttpClient.newHttpClient()) {
            response = client
                    .send(request, HttpResponse.BodyHandlers.ofString());
        }

        return response;
    }

    /**
     * Sends an HTTP POST request with form URL-encoded data to the specified URI without any headers.
     *
     * @param uri  The target URI for the POST request.
     * @param body A map representing the form data to be sent in the request body.
     * @return The HTTP response as a {@link HttpResponse} object.
     * @throws IOException          If an I/O error occurs during the request.
     * @throws InterruptedException If the operation is interrupted.
     */
    @Nonnull
    public HttpResponse<String> postFormUrlEncoded(
            @Nonnull URI uri,
            @Nonnull Map<String, String> body
    ) throws IOException, InterruptedException {
        return postFormUrlEncoded(uri, body, null);
    }

    /**
     * Sends an HTTP POST request with form URL-encoded data to the specified URI with optional headers.
     *
     * @param uri     The target URI for the POST request.
     * @param body    A map representing the form data to be sent in the request body.
     * @param headers A map of headers to include in the request, or null if no headers are needed.
     * @return The HTTP response as a {@link HttpResponse} object.
     * @throws IOException          If an I/O error occurs during the request.
     * @throws InterruptedException If the operation is interrupted.
     */
    @Nonnull
    public HttpResponse<String> postFormUrlEncoded(
            @Nonnull URI uri,
            @Nonnull Map<String, String> body,
            @Nullable Map<String, String> headers
    ) throws IOException, InterruptedException {
        var formUrlEncodedBody = body
                .entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        var extendedHeaders = new HashMap<String, String>();
        if (headers != null) {
            extendedHeaders.putAll(headers);
        }
        extendedHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        return post(uri, formUrlEncodedBody, extendedHeaders);
    }

    /**
     * Sends an HTTP POST request with a raw string body to the specified URI without any headers.
     *
     * @param uri  The target URI for the POST request.
     * @param body The raw string data to be sent in the request body.
     * @return The HTTP response as a {@link HttpResponse} object.
     * @throws IOException          If an I/O error occurs during the request.
     * @throws InterruptedException If the operation is interrupted.
     */
    @Nonnull
    public HttpResponse<String> post(
            @Nonnull URI uri,
            @Nonnull String body
    ) throws IOException, InterruptedException {
        return post(uri, body, null);
    }

    /**
     * Sends an HTTP POST request with a raw string body to the specified URI with optional headers.
     *
     * @param uri     The target URI for the POST request.
     * @param body    The raw string data to be sent in the request body.
     * @param headers A map of headers to include in the request, or null if no headers are needed.
     * @return The HTTP response as a {@link HttpResponse} object.
     * @throws IOException          If an I/O error occurs during the request.
     * @throws InterruptedException If the operation is interrupted.
     */
    @Nonnull
    public HttpResponse<String> post(
            @Nonnull URI uri,
            @Nonnull String body,
            @Nullable Map<String, String> headers
    ) throws IOException, InterruptedException {
        var requestBuilder = HttpRequest
                .newBuilder()
                .uri(uri)
                .POST(HttpRequest.BodyPublishers.ofString(body));

        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }

        var request = requestBuilder
                .build();

        HttpResponse<String> response;
        try (var client = HttpClient.newHttpClient()) {
            response = client
                    .send(request, HttpResponse.BodyHandlers.ofString());
        }

        return response;
    }
}
