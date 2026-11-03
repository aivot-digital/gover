package de.aivot.prosuna.backend.security;

import de.aivot.prosuna.backend.user.repositories.UserApiKeyRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Lets a user API key stand in for the Keycloak access token. Tokens with the API key prefix become a {@link Jwt} whose
 * subject is the key's user ID, so the existing {@code @AuthenticationPrincipal Jwt} consumers resolve the same user
 * without changes. All other tokens go to the Keycloak decoder.
 */
public class ApiKeyJwtDecoder implements JwtDecoder {
    // Keycloak access tokens are JWTs and always start with "eyJ", so they can never carry this prefix
    public static final String API_KEY_PREFIX = "prosuna_";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtDecoder delegate;
    private final UserApiKeyRepository userApiKeyRepository;

    public ApiKeyJwtDecoder(
            @Nonnull JwtDecoder delegate,
            @Nonnull UserApiKeyRepository userApiKeyRepository
    ) {
        this.delegate = delegate;
        this.userApiKeyRepository = userApiKeyRepository;
    }

    /**
     * Returns the API key of the {@code Authorization: Bearer} header, or {@code null} if the request carries none.
     * API keys are only accepted from this header, which browsers never attach on their own, so these requests are
     * exempt from CSRF protection.
     */
    @Nullable
    public static String resolveApiKey(@Nonnull HttpServletRequest request) {
        var authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }

        var token = authorization.substring(BEARER_PREFIX.length());
        return token.startsWith(API_KEY_PREFIX) ? token : null;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        if (!token.startsWith(API_KEY_PREFIX)) {
            return delegate.decode(token);
        }

        var apiKey = userApiKeyRepository
                .findByKeyHashOfActiveUser(hash(token))
                .filter(key -> key.getExpiration().isAfter(Instant.now()))
                .orElseThrow(() -> new BadJwtException("Invalid API key"));

        return Jwt
                .withTokenValue(token)
                .header("alg", "none")
                .subject(apiKey.getUserId())
                .expiresAt(apiKey.getExpiration())
                .build();
    }

    @Nonnull
    static String hash(@Nonnull String apiKey) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(apiKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
