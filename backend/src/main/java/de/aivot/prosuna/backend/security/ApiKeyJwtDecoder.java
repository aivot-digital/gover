package de.aivot.prosuna.backend.security;

import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.repositories.UserApiKeyRepository;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

/**
 * Lets a user API key stand in for the Keycloak access token. Tokens with the API key prefix are resolved to their
 * user, which becomes the principal as an {@link ApiKeyPrincipal}. All other tokens go to the Keycloak decoder.
 */
public class ApiKeyJwtDecoder implements JwtDecoder {
    public static final String API_KEY_PREFIX = "prosuna_api_";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtDecoder delegate;
    private final UserApiKeyRepository userApiKeyRepository;
    private final UserRepository userRepository;

    public ApiKeyJwtDecoder(
            @Nonnull JwtDecoder delegate,
            @Nonnull UserApiKeyRepository userApiKeyRepository,
            @Nonnull UserRepository userRepository
    ) {
        this.delegate = delegate;
        this.userApiKeyRepository = userApiKeyRepository;
        this.userRepository = userRepository;
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
                .findByKeyHash(hash(token))
                .filter(key -> key.getExpiration().isAfter(Instant.now()))
                .orElseThrow(() -> new BadJwtException("Invalid API key"));

        // Keycloak refuses tokens for disabled users, so the API key has to enforce this itself
        var user = userRepository
                .findById(apiKey.getUserId())
                .filter(u -> Boolean.TRUE.equals(u.getEnabled()) && !Boolean.TRUE.equals(u.getDeletedInIdp()))
                .orElseThrow(() -> new BadJwtException("Invalid API key"));

        return new ApiKeyPrincipal(token, apiKey.getExpiration(), user);
    }

    /**
     * The principal of an API key request: the user the key belongs to. It is a {@link Jwt} whose subject is the user
     * ID, so the existing {@code @AuthenticationPrincipal Jwt} consumers resolve the same user without changes.
     */
    public static class ApiKeyPrincipal extends Jwt {
        private final UserEntity user;

        private ApiKeyPrincipal(@Nonnull String apiKey, @Nonnull Instant expiration, @Nonnull UserEntity user) {
            super(
                    apiKey,
                    null,
                    expiration,
                    Map.of("alg", "none"),
                    Map.of(JwtClaimNames.SUB, user.getId(), JwtClaimNames.EXP, expiration)
            );
            this.user = user;
        }

        @Nonnull
        public UserEntity getUser() {
            return user;
        }
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
