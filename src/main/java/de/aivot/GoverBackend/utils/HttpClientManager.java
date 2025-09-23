package de.aivot.GoverBackend.utils;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpClientManager {
    // ❗️ Reuse the same client across the application. It's thread-safe.
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10)) // Set a default connection timeout
            .build();

    // ❗️ Create a dedicated thread pool to run blocking network calls.
    // This prevents your main server threads from being blocked.
    private static final ExecutorService VIRTUAL_THREAD_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    public static HttpClient getClient() {
        return HTTP_CLIENT;
    }

    public static ExecutorService getExecutor() {
        return VIRTUAL_THREAD_EXECUTOR;
    }
}
