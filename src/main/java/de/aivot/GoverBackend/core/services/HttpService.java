package de.aivot.GoverBackend.core.services;

import de.aivot.GoverBackend.core.models.HttpServiceHeaders;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class HttpService {
    private final static Duration CONNECT_TIMEOUT = Duration.ofSeconds(45);
    private final static Duration GET_TIMEOUT = Duration.ofSeconds(15);
    private final static Duration POST_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;

    public HttpService() {
        this.httpClient = HttpClient
                .newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    // region HTTP-Get Sync

    @Nonnull
    public HttpResponse<String> get(@Nonnull URI uri) throws IOException, InterruptedException {
        return get(uri, null);
    }

    @Nonnull
    public HttpResponse<String> get(@Nonnull URI uri, @Nullable HttpServiceHeaders headers) throws IOException, InterruptedException {
        return getAsync(uri, headers)
                .join();
    }

    // endregion
    // region HTTP-Get Async

    @Nonnull
    public CompletableFuture<HttpResponse<String>> getAsync(@Nonnull URI uri) {
        return getAsync(uri, null);
    }

    @Nonnull
    public CompletableFuture<HttpResponse<String>> getAsync(@Nonnull URI uri, @Nullable HttpServiceHeaders headers) {
        var requestBuilder = HttpRequest
                .newBuilder()
                .timeout(GET_TIMEOUT)
                .uri(uri)
                .GET();

        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }

        var request = requestBuilder
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    // endregion
    // region HTTP-Post Sync

    @Nonnull
    public HttpResponse<String> post(@Nonnull URI uri, @Nonnull String body) throws IOException, InterruptedException {
        return post(uri, body, null);
    }

    @Nonnull
    public HttpResponse<String> post(@Nonnull URI uri, @Nonnull String body, @Nullable HttpServiceHeaders headers) {
        return postAsync(uri, body, headers)
                .join();
    }

    // endregion
    // region HTTP-Post Async

    @Nonnull
    public CompletableFuture<HttpResponse<String>> postAsync(@Nonnull URI uri, @Nonnull String body) {
        return postAsync(uri, body, null);
    }

    @Nonnull
    public CompletableFuture<HttpResponse<String>> postAsync(@Nonnull URI uri, @Nonnull String body, @Nullable HttpServiceHeaders headers) {
        var requestBuilder = HttpRequest
                .newBuilder()
                .timeout(POST_TIMEOUT)
                .uri(uri)
                .POST(HttpRequest.BodyPublishers.ofString(body));

        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }

        var request = requestBuilder
                .build();

        return httpClient
                .sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    // endregion
    // region HTTP-Post Form-UrlEncoded Sync

    @Nonnull
    public HttpResponse<String> postFormUrlEncoded(
            @Nonnull URI uri,
            @Nonnull Map<String, String> body
    ) throws IOException, InterruptedException {
        return postFormUrlEncoded(uri, body, null);
    }

    @Nonnull
    public HttpResponse<String> postFormUrlEncoded(
            @Nonnull URI uri,
            @Nonnull Map<String, String> body,
            @Nullable HttpServiceHeaders headers
    ) throws IOException, InterruptedException {
        return postFormUrlEncodedAsync(uri, body, headers)
                .join();
    }

    // endregion
    // region HTTP-Post Form-UrlEncoded Async

    @Nonnull
    public CompletableFuture<HttpResponse<String>> postFormUrlEncodedAsync(
            @Nonnull URI uri,
            @Nonnull Map<String, String> body
    ) {
        return postFormUrlEncodedAsync(uri, body, null);
    }

    @Nonnull
    public CompletableFuture<HttpResponse<String>> postFormUrlEncodedAsync(
            @Nonnull URI uri,
            @Nonnull Map<String, String> body,
            @Nullable HttpServiceHeaders headers
    ) {
        var formUrlEncodedBody = body
                .entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        var extendedHeaders = HttpServiceHeaders
                .create()
                .with(headers)
                .with("Content-Type", HttpServiceHeaders.APPLICATION_X_WWW_FORM_URLENCODED);

        return postAsync(uri, formUrlEncodedBody, extendedHeaders);
    }

    // endregion
}
