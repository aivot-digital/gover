package de.aivot.prosuna.backend.utils;

import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class HttpClientUtils {
    private HttpClientUtils() {
    }

    /**
     * Sends the request and reads the complete body as a string within the client's connect timeout plus the request timeout.
     * {@link HttpRequest#timeout()} alone stops applying once the response headers arrive, so a stalled body would block forever.
     */
    @Nonnull
    public static HttpResponse<String> sendWithDeadline(
            @Nonnull HttpClient client,
            @Nonnull HttpRequest request
    ) throws IOException, InterruptedException {
        var deadline = client.connectTimeout().orElse(Duration.ZERO)
                .plus(request.timeout().orElseThrow(() -> new IllegalArgumentException("The request needs a timeout")));

        var future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        try {
            return future.get(deadline.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new HttpTimeoutException("The response was not completely received within " + deadline);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IOException(e.getCause());
        } finally {
            // No-op for a completed exchange; otherwise aborts the underlying request so it does not keep the connection busy.
            future.cancel(true);
        }
    }
}
