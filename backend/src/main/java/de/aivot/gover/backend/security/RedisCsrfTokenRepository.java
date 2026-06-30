package de.aivot.gover.backend.security;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;

@Component
public class RedisCsrfTokenRepository implements CsrfTokenRepository {
    public static final String HEADER_NAME = "X-CSRF-TOKEN";
    public static final String PARAMETER_NAME = "_csrf";
    public static final String COOKIE_NAME = "csrf";

    private static final String COOKIE_PATH = "/api/";
    private static final String REDIS_PREFIX = "csrf:token:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;
    private final Duration ttl;

    public RedisCsrfTokenRepository(
            StringRedisTemplate redis,
            @Value("${gover.security.csrfTtlSeconds:86400}") long ttlSeconds
    ) {
        this.redis = redis;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        return new DefaultCsrfToken(HEADER_NAME, PARAMETER_NAME, generateOpaqueValue());
    }

    @Override
    public void saveToken(@Nullable CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
        var csrfId = resolveCsrfId(request);

        if (token == null) {
            if (csrfId != null) {
                redis.delete(getRedisKey(csrfId));
            }

            response.addCookie(createExpiredCookie());
            return;
        }

        if (csrfId == null) {
            csrfId = generateOpaqueValue();
        }

        redis.opsForValue().set(getRedisKey(csrfId), token.getToken(), ttl);
        response.addCookie(createCookie(csrfId));
    }

    @Override
    @Nullable
    public CsrfToken loadToken(HttpServletRequest request) {
        var csrfId = resolveCsrfId(request);
        if (csrfId == null) {
            return null;
        }

        var tokenValue = redis.opsForValue().get(getRedisKey(csrfId));
        if (tokenValue == null) {
            return null;
        }

        return new DefaultCsrfToken(HEADER_NAME, PARAMETER_NAME, tokenValue);
    }

    @Nullable
    private static String resolveCsrfId(HttpServletRequest request) {
        var cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        return Arrays
                .stream(cookies)
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private String getRedisKey(String csrfId) {
        return REDIS_PREFIX + csrfId;
    }

    private Cookie createCookie(String csrfId) {
        var cookie = new Cookie(COOKIE_NAME, csrfId);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setPath(COOKIE_PATH);
        cookie.setMaxAge((int) ttl.toSeconds());
        cookie.setAttribute("SameSite", "Strict");
        return cookie;
    }

    private static Cookie createExpiredCookie() {
        var cookie = new Cookie(COOKIE_NAME, "");
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setPath(COOKIE_PATH);
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Strict");
        return cookie;
    }

    private static String generateOpaqueValue() {
        var bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
