package de.aivot.GoverBackend.core.services;

import de.aivot.GoverBackend.core.exceptions.HttpConnectionException;
import de.aivot.GoverBackend.core.models.HttpServiceHeaders;
import de.aivot.GoverBackend.core.properties.HttpServiceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class HttpServiceTest {
    @Test
    void clientHttpRequestFactoryShouldUseJdkHttpClientWithHttp11() throws Exception {
        var requestFactory = assertInstanceOf(
                JdkClientHttpRequestFactory.class,
                HttpService.clientHttpRequestFactory(httpServiceProperties())
        );

        var httpClient = getHttpClient(requestFactory);

        assertEquals(HttpClient.Version.HTTP_1_1, httpClient.version());
    }

    @Test
    void requestShouldPreserveSuccessfulStatusHeadersAndBody() throws Exception {
        var builder = restClientBuilder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var httpService = new HttpService(builder.build());

        server.expect(requestTo("https://gover.test/http"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Test-Request", "header-value"))
                .andExpect(content().string("request-body"))
                .andRespond(
                        withStatus(HttpStatus.CREATED)
                                .contentType(MediaType.TEXT_PLAIN)
                                .header("X-Test-Response", "response-value")
                                .body("created")
                );

        var response = httpService.request(
                HttpMethod.POST,
                URI.create("https://gover.test/http"),
                "request-body",
                HttpServiceHeaders.create().with("X-Test-Request", "header-value")
        );

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("created", new String(response.getBody(), StandardCharsets.UTF_8));
        assertEquals("response-value", response.getHeaders().getFirst("X-Test-Response"));
        server.verify();
    }

    @Test
    void requestShouldPreserveErrorStatusHeadersAndBody() throws Exception {
        var builder = restClientBuilder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var httpService = new HttpService(builder.build());

        server.expect(requestTo("https://gover.test/http"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                                .contentType(MediaType.TEXT_PLAIN)
                                .header("X-Error-Code", "unprocessable")
                                .body("invalid")
                );

        var response = httpService.request(HttpMethod.GET, URI.create("https://gover.test/http"));

        assertEquals(422, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("invalid", new String(response.getBody(), StandardCharsets.UTF_8));
        assertEquals("unprocessable", response.getHeaders().getFirst("X-Error-Code"));
        server.verify();
    }

    @Test
    void requestShouldWrapTransportFailures() {
        var builder = restClientBuilder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var httpService = new HttpService(builder.build());

        server.expect(requestTo("https://gover.test/http"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withException(new IOException("connection failed")));

        assertThrows(
                HttpConnectionException.class,
                () -> httpService.request(HttpMethod.GET, URI.create("https://gover.test/http"))
        );
        server.verify();
    }

    private HttpClient getHttpClient(JdkClientHttpRequestFactory requestFactory) throws NoSuchFieldException, IllegalAccessException {
        Field field = JdkClientHttpRequestFactory.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        return (HttpClient) field.get(requestFactory);
    }

    private RestClient.Builder restClientBuilder() {
        return RestClient
                .builder()
                .requestFactory(HttpService.clientHttpRequestFactory(httpServiceProperties()));
    }

    private HttpServiceProperties httpServiceProperties() {
        var properties = new HttpServiceProperties();
        properties.setConnectionTimeoutSeconds(2);
        properties.setReadTimeoutSeconds(30);
        return properties;
    }
}
