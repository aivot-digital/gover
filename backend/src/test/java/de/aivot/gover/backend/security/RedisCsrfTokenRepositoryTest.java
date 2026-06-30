package de.aivot.gover.backend.security;

import de.aivot.gover.backend.security.RedisCsrfTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;

import java.time.Duration;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RedisCsrfTokenRepositoryTest {
    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOperations;
    private RedisCsrfTokenRepository repository;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOperations);
        repository = new RedisCsrfTokenRepository(redis, 3600);
    }

    @Test
    void saveTokenShouldStoreValueInRedisAndSetCookie() {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        CsrfToken token = repository.generateToken(request);

        repository.saveToken(token, request, response);

        verify(valueOperations).set(anyString(), eq(token.getToken()), eq(Duration.ofHours(1)));

        var cookie = Arrays
                .stream(response.getCookies())
                .filter(candidate -> RedisCsrfTokenRepository.COOKIE_NAME.equals(candidate.getName()))
                .findFirst()
                .orElseThrow();

        assertTrue(cookie.getSecure());
        assertTrue(cookie.isHttpOnly());
        assertEquals("/api/", cookie.getPath());
        assertEquals(3600, cookie.getMaxAge());
        assertNotNull(cookie.getValue());
        assertFalse(cookie.getValue().isBlank());
        assertEquals("Strict", cookie.getAttribute("SameSite"));
    }

    @Test
    void loadTokenShouldReturnStoredTokenForCookieId() {
        var request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie(RedisCsrfTokenRepository.COOKIE_NAME, "csrf-id"));

        when(valueOperations.get("csrf:token:csrf-id")).thenReturn("stored-token");

        var token = repository.loadToken(request);

        assertNotNull(token);
        assertEquals(RedisCsrfTokenRepository.HEADER_NAME, token.getHeaderName());
        assertEquals(RedisCsrfTokenRepository.PARAMETER_NAME, token.getParameterName());
        assertEquals("stored-token", token.getToken());
    }

    @Test
    void saveTokenShouldDeleteStoredTokenAndExpireCookieWhenClearing() {
        var request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie(RedisCsrfTokenRepository.COOKIE_NAME, "csrf-id"));
        var response = new MockHttpServletResponse();

        repository.saveToken(null, request, response);

        verify(redis).delete("csrf:token:csrf-id");

        var cookie = Arrays
                .stream(response.getCookies())
                .filter(candidate -> RedisCsrfTokenRepository.COOKIE_NAME.equals(candidate.getName()))
                .findFirst()
                .orElseThrow();

        assertEquals("", cookie.getValue());
        assertEquals(0, cookie.getMaxAge());
        assertEquals("/api/", cookie.getPath());
        assertTrue(cookie.getSecure());
        assertTrue(cookie.isHttpOnly());
        assertEquals("Strict", cookie.getAttribute("SameSite"));
    }
}
