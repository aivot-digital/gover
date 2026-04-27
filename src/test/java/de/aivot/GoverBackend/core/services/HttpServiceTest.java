package de.aivot.GoverBackend.core.services;

import de.aivot.GoverBackend.core.properties.HttpServiceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.lang.reflect.Field;
import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

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

    private HttpClient getHttpClient(JdkClientHttpRequestFactory requestFactory) throws NoSuchFieldException, IllegalAccessException {
        Field field = JdkClientHttpRequestFactory.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        return (HttpClient) field.get(requestFactory);
    }

    private HttpServiceProperties httpServiceProperties() {
        var properties = new HttpServiceProperties();
        properties.setConnectionTimeoutSeconds(2);
        properties.setReadTimeoutSeconds(30);
        return properties;
    }
}
