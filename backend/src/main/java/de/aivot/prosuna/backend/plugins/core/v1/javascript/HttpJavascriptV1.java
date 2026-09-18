package de.aivot.prosuna.backend.plugins.core.v1.javascript;

import de.aivot.prosuna.backend.core.exceptions.HttpConnectionException;
import de.aivot.prosuna.backend.core.models.HttpServiceHeaders;
import de.aivot.prosuna.backend.core.services.HttpService;
import de.aivot.prosuna.backend.javascript.providers.JavascriptFunctionProvider;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import jakarta.annotation.Nonnull;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;

/**
 * SPI for providing HTTP functions to the Javascript environment.
 */
@Service
public class HttpJavascriptV1 implements JavascriptFunctionProvider {
    private final HttpService httpService;

    public HttpJavascriptV1(HttpService httpService) {
        this.httpService = httpService;
    }

    @Nonnull
    @Override
    public String getComponentKey() {
        return "http";
    }

    @Nonnull
    @Override
    public String getComponentVersion() {
        return "1.0.0";
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return CorePlugin.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public String getName() {
        return "HTTP-Funktionen";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Dieses Modul stellt Funktionen zur Durchführung von HTTP-Anfragen bereit.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                Ermöglicht HTTP-Aufrufe aus JavaScript-Ausdrücken, um externe Dienste anzusprechen.

                Das Modul unterstützt GET- und POST-Anfragen mit frei definierbaren Headern. POST-Anfragen können zusätzlich einen Textinhalt übertragen; Status und Antwortinhalt stehen anschließend für die weitere Verarbeitung zur Verfügung.
                """;
    }

    @Override
    public String[] getMethodTypeDefinitions() {
        return new String[]{
                "get(url: string, headers: Record<string, string>): {statusCode: number; body: string};",
                "post(url: string, body: string, headers: Record<string, string>): {statusCode: number; body: string};"
        };
    }

    /**
     * Sends a GET request to the given URL with the given headers.
     *
     * @param url         The URL to send the request to.
     * @param headerValue The headers to send with the request.
     * @return The response of the request.
     * @throws IOException          If an I/O error occurs.
     * @throws InterruptedException If the request is interrupted.
     */
    @HostAccess.Export
    public HttpResult get(String url, Value headerValue) throws IOException, InterruptedException {
        var uri = URI.create(url);

        var headers = polyglotValueToMap(headerValue);

        HttpResponse<String> response;
        try {
            response = httpService
                    .get(uri, HttpServiceHeaders.of(headers));
        } catch (HttpConnectionException e) {
            return new HttpResult(500, e.getMessage());
        }

        return new HttpResult(response.statusCode(), response.body());
    }

    /**
     * Sends a POST request to the given URL with the given body and headers.
     *
     * @param url         The URL to send the request to.
     * @param body        The body of the request as a String.
     * @param headerValue The headers to send with the request.
     * @return The response of the request.
     * @throws IOException          If an I/O error occurs.
     * @throws InterruptedException If the request is interrupted.s
     */
    @HostAccess.Export
    public HttpResult post(String url, String body, Value headerValue) throws IOException, InterruptedException {
        var uri = URI.create(url);

        var headers = polyglotValueToMap(headerValue);

        HttpResponse<String> response;
        try {
            response = httpService
                    .post(uri, body, HttpServiceHeaders.of(headers));
        } catch (HttpConnectionException e) {
            return new HttpResult(500, e.getMessage());
        }

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
