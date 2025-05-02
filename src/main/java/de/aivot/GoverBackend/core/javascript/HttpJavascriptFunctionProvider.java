package de.aivot.GoverBackend.core.javascript;

import de.aivot.GoverBackend.javascript.providers.JavascriptFunctionProvider;
import org.graalvm.polyglot.HostAccess;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * SPI for providing HTTP functions to the Javascript environment.
 */
@Service
public class HttpJavascriptFunctionProvider implements JavascriptFunctionProvider {
    @Override
    public String getPackageName() {
        return "http";
    }

    @Override
    public String getLabel() {
        return "HTTP";
    }

    @Override
    public String getDescription() {
        return "Dieses Paket enthält Funktionen für HTTP-Anfragen.";
    }

    /**
     * Sends a GET request to the given URL with the given headers.
     *
     * @param url     The URL to send the request to.
     * @param headers The headers to send with the request.
     * @return The response of the request.
     * @throws IOException          If an I/O error occurs.
     * @throws InterruptedException If the request is interrupted.
     */
    @HostAccess.Export
    public HttpResult get(String url, Map<String, String> headers) throws IOException, InterruptedException {
        var uri = URI.create(url);

        var client = HttpClient
                .newBuilder()
                .build();

        var requestBuilder = HttpRequest
                .newBuilder(uri);
        for (var entry : headers.entrySet()) {
            requestBuilder.header(entry.getKey(), entry.getValue());
        }
        var request = requestBuilder
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        var response = client
                .send(request, HttpResponse.BodyHandlers.ofString());

        client.close();

        return new HttpResult(response.statusCode(), response.body());
    }

    /**
     * Sends a POST request to the given URL with the given body and headers.
     *
     * @param url     The URL to send the request to.
     * @param body    The body of the request as a String.
     * @param headers The headers to send with the request.
     * @return The response of the request.
     * @throws IOException          If an I/O error occurs.
     * @throws InterruptedException If the request is interrupted.s
     */
    @HostAccess.Export
    public HttpResult post(String url, String body, Map<String, String> headers) throws IOException, InterruptedException {
        var uri = URI.create(url);

        var client = HttpClient
                .newBuilder()
                .build();

        var requestBuilder = HttpRequest
                .newBuilder(uri);
        for (var entry : headers.entrySet()) {
            requestBuilder.header(entry.getKey(), entry.getValue());
        }
        var request = requestBuilder
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(30))
                .build();

        var response = client
                .send(request, HttpResponse.BodyHandlers.ofString());

        client.close();

        return new HttpResult(response.statusCode(), response.body());
    }

    /**
     * Represents the result of an HTTP request.
     */
    public static class HttpResult {
        @HostAccess.Export
        public final int statusCode;

        @HostAccess.Export
        public final String body;

        public HttpResult(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }
    }
}
