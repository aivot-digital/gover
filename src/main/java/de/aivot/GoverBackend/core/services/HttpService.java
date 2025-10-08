package de.aivot.GoverBackend.core.services;

import de.aivot.GoverBackend.core.exceptions.HttpConnectionException;
import de.aivot.GoverBackend.core.models.HttpResponseImpl;
import de.aivot.GoverBackend.core.models.HttpServiceHeaders;
import de.aivot.GoverBackend.utils.MultipartUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HttpService {
    private final RestClient httpClient;

    public HttpService() {
        this.httpClient = RestClient
                .builder()
                .build();
    }

    // region HTTP-Get

    @Nonnull
    public HttpResponse<String> get(@Nonnull URI uri) throws HttpConnectionException {
        return get(uri, null);
    }

    @Nonnull
    public HttpResponse<String> get(@Nonnull URI uri, @Nullable HttpServiceHeaders headers) throws HttpConnectionException {
        String responseBody;
        try {
            responseBody = httpClient
                    .get()
                    .uri(uri)
                    .headers(_headers -> {
                        if (headers != null) {
                            headers.forEach(_headers::add);
                        }
                    })
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            return new HttpResponseImpl<>(
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString()
            );
        }

        return new HttpResponseImpl<>(200, responseBody);
    }

    // endregion
    // region HTTP-Post

    @Nonnull
    public HttpResponse<String> post(@Nonnull URI uri, @Nonnull String body) throws HttpConnectionException {
        return post(uri, body, null);
    }

    @Nonnull
    public HttpResponse<String> post(@Nonnull URI uri, @Nonnull String body, @Nullable HttpServiceHeaders headers) throws  HttpConnectionException {
        String responseBody;
        try {
            responseBody = httpClient
                    .post()
                    .uri(uri)
                    .body(body)
                    .headers(_headers -> {
                        if (headers != null) {
                            headers.forEach(_headers::add);
                        }
                    })
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            return new HttpResponseImpl<>(
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString()
            );
        }

        return new HttpResponseImpl<>(200, responseBody);
    }

    // endregion
    // region HTTP-Post Form-UrlEncoded

    @Nonnull
    public HttpResponse<String> postFormUrlEncoded(
            @Nonnull URI uri,
            @Nonnull Map<String, String> body
    ) throws HttpConnectionException {
        return postFormUrlEncoded(uri, body, null);
    }

    @Nonnull
    public HttpResponse<String> postFormUrlEncoded(
            @Nonnull URI uri,
            @Nonnull Map<String, String> body,
            @Nullable HttpServiceHeaders headers
    ) throws HttpConnectionException {
        var formUrlEncodedBody = body
                .entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        var extendedHeaders = HttpServiceHeaders
                .create()
                .with(headers)
                .with("Content-Type", HttpServiceHeaders.APPLICATION_X_WWW_FORM_URLENCODED);

        return post(uri, formUrlEncodedBody, extendedHeaders);
    }

    // endregion
    // region HTTP-Post Multipart

    @Nonnull
    public HttpResponse<InputStream> postMultipart(@Nonnull URI uri, @Nonnull MultipartUtils.MultipartBodyPublisher body)  throws HttpConnectionException {
        return postMultipart(uri, body, null);
    }

    @Nonnull
    public HttpResponse<InputStream> postMultipart(@Nonnull URI uri, @Nonnull MultipartUtils.MultipartBodyPublisher body, @Nullable HttpServiceHeaders headers)  throws HttpConnectionException {
        InputStream responseBody;
        try {
            responseBody = httpClient
                    .post()
                    .uri(uri)
                    .body(body)
                    .headers(_headers -> {
                        if (headers != null) {
                            headers.forEach(_headers::add);
                        }
                    })
                    .header("Content-Type", HttpServiceHeaders.MULTIPART_FORM_DATA + "; boundary=" + body.getBoundary())
                    .retrieve()
                    .body(InputStream.class);
        } catch (RestClientResponseException e) {
            return new HttpResponseImpl<>(
                    e.getStatusCode().value(),
                    null
            );
        }

        return new HttpResponseImpl<>(200, responseBody);
    }

    // endregion
}
