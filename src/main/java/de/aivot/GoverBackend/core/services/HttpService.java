package de.aivot.GoverBackend.core.services;

import de.aivot.GoverBackend.core.exceptions.HttpConnectionException;
import de.aivot.GoverBackend.core.models.HttpResponseImpl;
import de.aivot.GoverBackend.core.models.HttpServiceHeaders;
import de.aivot.GoverBackend.utils.MultipartUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

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

    @Nonnull
    public <T> ResponseEntity<T> getEntity(@Nonnull URI uri,
                                           @Nonnull Class<T> clazz) throws RestClientResponseException {
        return getEntity(uri, null, clazz);
    }

    @Nonnull
    public <T> ResponseEntity<T> getEntity(@Nonnull URI uri,
                                           @Nullable HttpServiceHeaders headers,
                                           @Nonnull Class<T> clazz) throws RestClientResponseException {
        return httpClient
                .get()
                .uri(uri)
                .headers(_headers -> {
                    if (headers != null) {
                        headers.forEach(_headers::add);
                    }
                })
                .retrieve()
                .toEntity(clazz);
    }

    // endregion

    // region HTTP-Post
    @Nonnull
    public HttpResponse<String> post(@Nonnull URI uri, @Nonnull String body) throws HttpConnectionException {
        return post(uri, body, null);
    }

    @Nonnull
    public HttpResponse<String> post(@Nonnull URI uri, @Nonnull String body, @Nullable HttpServiceHeaders headers) throws HttpConnectionException {
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

    @Nonnull
    public <T> ResponseEntity<T> postEntity(@Nonnull URI uri,
                                            @Nonnull String body,
                                            @Nonnull Class<T> clazz) throws RestClientResponseException {
        return postEntity(uri, body, null, clazz);
    }

    @Nonnull
    public <T> ResponseEntity<T> postEntity(@Nonnull URI uri,
                                            @Nonnull String body,
                                            @Nullable HttpServiceHeaders headers,
                                            @Nonnull Class<T> clazz) throws RestClientResponseException {
        return httpClient
                .post()
                .uri(uri)
                .body(body)
                .headers(_headers -> {
                    if (headers != null) {
                        headers.forEach(_headers::add);
                    }
                })
                .retrieve()
                .toEntity(clazz);
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

    @Nonnull
    public <T> ResponseEntity<T> postFormUrlEncodedEntity(@Nonnull URI uri,
                                                    @Nonnull Map<String, String> body,
                                                    @Nonnull Class<T> clazz) throws RestClientResponseException {
        return postFormUrlEncodedEntity(uri, body, null, clazz);
    }

    @Nonnull
    public <T> ResponseEntity<T> postFormUrlEncodedEntity(@Nonnull URI uri,
                                                    @Nonnull Map<String, String> body,
                                                    @Nullable HttpServiceHeaders headers,
                                                    @Nonnull Class<T> clazz) throws RestClientResponseException {
        var formUrlEncodedBody = body
                .entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        var extendedHeaders = HttpServiceHeaders
                .create()
                .with(headers)
                .with("Content-Type", HttpServiceHeaders.APPLICATION_X_WWW_FORM_URLENCODED);

        return postEntity(uri, formUrlEncodedBody, extendedHeaders, clazz);
    }

    // endregion

    // region HTTP-Post Multipart

    @Nonnull
    public HttpResponse<byte[]> postMultipart(@Nonnull URI uri, @Nonnull MultipartUtils.MultipartBodyPublisher body) throws HttpConnectionException {
        return postMultipart(uri, body, null);
    }

    @Nonnull
    public HttpResponse<byte[]> postMultipart(@Nonnull URI uri, @Nonnull MultipartUtils.MultipartBodyPublisher body, @Nullable HttpServiceHeaders headers) throws HttpConnectionException {
        byte[] responseBody;
        try {
            responseBody = httpClient
                    .post()
                    .uri(uri)
                    .body(body.build())
                    .headers(_headers -> {
                        if (headers != null) {
                            headers.forEach(_headers::add);
                        }
                    })
                    .retrieve()
                    .body(byte[].class);
        } catch (RestClientResponseException e) {
            return new HttpResponseImpl<>(
                    e.getStatusCode().value(),
                    null
            );
        }

        return new HttpResponseImpl<>(200, responseBody);
    }

    @Nonnull
    public <T> ResponseEntity<T> postMultipartEntity(@Nonnull URI uri,
                                               @Nonnull MultipartUtils.MultipartBodyPublisher body,
                                               @Nonnull Class<T> clazz) throws RestClientResponseException {
        return postMultipartEntity(uri, body, null, clazz);
    }

    @Nonnull
    public <T> ResponseEntity<T> postMultipartEntity(@Nonnull URI uri,
                                               @Nonnull MultipartUtils.MultipartBodyPublisher body,
                                               @Nullable HttpServiceHeaders headers,
                                               @Nonnull Class<T> clazz) throws RestClientResponseException {
        return httpClient
                .post()
                .uri(uri)
                .body(body.build())
                .headers(_headers -> {
                    if (headers != null) {
                        headers.forEach(_headers::add);
                    }
                })
                .retrieve()
                .toEntity(clazz);
    }

    // endregion


    // region HTTP-PUT

    @Nonnull
    public <T> ResponseEntity<T> put(@Nonnull URI uri,
                                     @Nonnull String body,
                                     @Nonnull Class<T> clazz) throws RestClientResponseException {
        return put(uri, body, null, clazz);
    }

    @Nonnull
    public <T> ResponseEntity<T> put(@Nonnull URI uri,
                                     @Nonnull String body,
                                     @Nullable HttpServiceHeaders headers,
                                     @Nonnull Class<T> clazz) throws RestClientResponseException {
        return httpClient
                .put()
                .uri(uri)
                .body(body)
                .headers(_headers -> {
                    if (headers != null) {
                        headers.forEach(_headers::add);
                    }
                })
                .retrieve()
                .toEntity(clazz);
    }

    // endregion

    // region HTTP-DELETE

    public ResponseEntity<Void> delete(@Nonnull URI uri,
                                       @Nullable HttpServiceHeaders httpServiceHeaders) throws RestClientResponseException {
        return httpClient
                .delete()
                .uri(uri)
                .headers(_headers -> {
                    if (httpServiceHeaders != null) {
                        httpServiceHeaders.forEach(_headers::add);
                    }
                })
                .retrieve()
                .toBodilessEntity();
    }

    // endregion
}
