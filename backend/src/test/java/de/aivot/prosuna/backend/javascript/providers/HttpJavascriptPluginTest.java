package de.aivot.prosuna.backend.javascript.providers;

import de.aivot.prosuna.backend.plugins.core.v1.javascript.HttpJavascriptV1;
import de.aivot.prosuna.backend.core.models.HttpServiceHeaders;
import de.aivot.prosuna.backend.core.services.HttpService;
import de.aivot.prosuna.backend.javascript.models.JavascriptCode;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HttpJavascriptPluginTest {

    private HttpService httpService;

    @BeforeEach
    void setUp() {
        httpService = mock(HttpService.class);
    }

    @Test
    void get() {
        try (var jsService = new JavascriptEngine(new HttpJavascriptV1(httpService))) {
            HttpResponse<String> mockResponse = mock(HttpResponse.class);
            when(mockResponse.statusCode())
                    .thenReturn(200);
            when(mockResponse.body())
                    .thenReturn("{}");
            when(httpService.get(any(URI.class)))
                    .thenReturn(mockResponse);
            when(httpService.get(any(URI.class), any(HttpServiceHeaders.class)))
                    .thenReturn(mockResponse);

            var result = jsService.evaluateCode(new JavascriptCode().setCode("_http_v1.get('https://postman-echo.com/get?test=value', {'x-test-header': 'header-value'}).body;"));

            assertEquals("{}", result.asString());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void post() {
        try (var jsService = new JavascriptEngine(new HttpJavascriptV1(httpService))) {
            HttpResponse<String> mockResponse = mock(HttpResponse.class);
            when(mockResponse.statusCode())
                    .thenReturn(200);
            when(mockResponse.body())
                    .thenReturn("{}");
            when(httpService.post(any(URI.class), any()))
                    .thenReturn(mockResponse);
            when(httpService.post(any(URI.class), any(), any(HttpServiceHeaders.class)))
                    .thenReturn(mockResponse);

            var result = jsService.evaluateCode(new JavascriptCode().setCode("_http_v1.post('https://postman-echo.com/post?test=value', JSON.stringify({'test-field': 'field-value'}), {'x-test-header': 'header-value'}).body;"));

            assertEquals("{}", result.asString());
        } catch (Exception e) {
            fail(e);
        }
    }
}