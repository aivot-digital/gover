package de.aivot.prosuna.backend.utils;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpClientUtilsTest {
    private static final String HEADERS = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: 13\r\n\r\n";

    @Test
    void returnsTheCompleteResponse() throws Exception {
        try (var server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            serve(server, HEADERS + "{\"ok\": true }", null);

            var response = HttpClientUtils.sendWithDeadline(client(), request(server));

            assertEquals(200, response.statusCode());
            assertEquals("{\"ok\": true }", response.body());
        }
    }

    @Test
    void givesUpAndAbortsWhenTheBodyStallsAfterTheHeaders() throws Exception {
        // HttpRequest.timeout() alone ends once the headers arrive, so this exchange would block forever.
        try (var server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            var connectionClosed = new CountDownLatch(1);
            serve(server, HEADERS + "{\"ok\"", connectionClosed);

            assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
                    assertThrows(HttpTimeoutException.class, () -> HttpClientUtils.sendWithDeadline(client(), request(server))));
            assertTrue(connectionClosed.await(5, TimeUnit.SECONDS), "The stalled request was not aborted");
        }
    }

    private static HttpClient client() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
    }

    private static HttpRequest request(ServerSocket server) {
        return HttpRequest
                .newBuilder(URI.create("http://127.0.0.1:" + server.getLocalPort() + "/"))
                .timeout(Duration.ofSeconds(1))
                .GET()
                .build();
    }

    /**
     * Answers one request with the given raw response and then keeps the connection open until the client closes it.
     */
    private static void serve(ServerSocket server, String rawResponse, CountDownLatch connectionClosed) {
        var thread = new Thread(() -> {
            try (var socket = server.accept()) {
                var in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
                String line;
                while ((line = in.readLine()) != null && !line.isEmpty()) {
                    // Consume the request headers.
                }
                socket.getOutputStream().write(rawResponse.getBytes(StandardCharsets.ISO_8859_1));
                socket.getOutputStream().flush();
                // Ends with -1 or a reset once the client aborts the exchange and closes the connection.
                try {
                    while (in.read() != -1) {
                        // Ignore anything else the client sends.
                    }
                } catch (java.io.IOException ignored) {
                    // A reset also means the client closed the connection.
                }
                if (connectionClosed != null) {
                    connectionClosed.countDown();
                }
            } catch (Exception ignored) {
                // The server socket closes when the test ends.
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
}
