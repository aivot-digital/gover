package de.aivot.GoverBackend.plugins.core.v1.javascript;

import de.aivot.GoverBackend.core.exceptions.HttpConnectionException;
import de.aivot.GoverBackend.core.models.HttpServiceHeaders;
import de.aivot.GoverBackend.core.services.HttpService;
import de.aivot.GoverBackend.javascript.providers.JavascriptFunctionProvider;
import de.aivot.GoverBackend.plugins.core.Core;
import jakarta.annotation.Nonnull;
import org.graalvm.polyglot.HostAccess;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.Map;

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
        return Core.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public String getName() {
        return "HTTP-Funktionen";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Dieses Modul stellt Funktionen zur Durchführung von HTTP-Anfragen bereit.";
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
     * @param url     The URL to send the request to.
     * @param headers The headers to send with the request.
     * @return The response of the request.
     * @throws IOException          If an I/O error occurs.
     * @throws InterruptedException If the request is interrupted.
     */
    @HostAccess.Export
    public HttpResult get(String url, Map<String, String> headers) throws IOException, InterruptedException {
        var uri = URI.create(url);

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
