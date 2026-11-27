package de.aivot.prosuna.backend.security;

import de.aivot.prosuna.backend.user.entities.UserApiKeyEntity;
import de.aivot.prosuna.backend.user.repositories.UserApiKeyRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ApiKeyJwtDecoderTest {
    private static final String API_KEY = ApiKeyJwtDecoder.API_KEY_PREFIX + "secret";
    private static final String USER_ID = "00000000-0000-0000-0000-000000000001";

    private final JwtDecoder keycloakDecoder = mock(JwtDecoder.class);
    private final UserApiKeyRepository userApiKeyRepository = mock(UserApiKeyRepository.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final ApiKeyJwtDecoder decoder = new ApiKeyJwtDecoder(keycloakDecoder, userApiKeyRepository, meterRegistry);

    @Test
    void hashesAsLowercaseHexSha256() {
        // Matches encode(sha256('abc'), 'hex') in PostgreSQL, so keys can be stored by SQL
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", ApiKeyJwtDecoder.hash("abc"));
    }

    @Test
    void resolvesTheKeyUserAsSubject() {
        var expiration = Instant.now().plusSeconds(60);
        givenApiKey(expiration);

        var jwt = decoder.decode(API_KEY);

        assertEquals(API_KEY, jwt.getTokenValue());
        assertEquals(USER_ID, jwt.getSubject());
        assertEquals(expiration, jwt.getExpiresAt());
        verifyNoInteractions(keycloakDecoder);

        assertEquals(1, apiKeyRequests("valid"));
        assertEquals(0, apiKeyRequests("invalid"));
    }

    @Test
    void delegatesOtherTokensToKeycloak() {
        var keycloakJwt = mock(Jwt.class);
        when(keycloakDecoder.decode("keycloak-token")).thenReturn(keycloakJwt);

        assertSame(keycloakJwt, decoder.decode("keycloak-token"));
        verifyNoInteractions(userApiKeyRepository);

        assertEquals(0, apiKeyRequests("valid"));
        assertEquals(0, apiKeyRequests("invalid"));
    }

    @Test
    void rejectsUnknownKey() {
        // Also covers keys of disabled users or users deleted in the IdP, which the repository does not return
        when(userApiKeyRepository.findByKeyHashOfActiveUser(anyString())).thenReturn(Optional.empty());

        assertThrows(BadJwtException.class, () -> decoder.decode(API_KEY));

        assertEquals(1, apiKeyRequests("invalid"));
        assertEquals(0, apiKeyRequests("valid"));
    }

    @Test
    void rejectsExpiredKey() {
        givenApiKey(Instant.now().minusSeconds(1));

        assertThrows(BadJwtException.class, () -> decoder.decode(API_KEY));

        assertEquals(1, apiKeyRequests("invalid"));
        assertEquals(0, apiKeyRequests("valid"));
    }

    @Test
    void resolvesOnlyPrefixedBearerTokensFromTheAuthorizationHeader() {
        assertEquals(API_KEY, ApiKeyJwtDecoder.resolveApiKey(requestWithAuthorization("Bearer " + API_KEY)));
        assertEquals(API_KEY, ApiKeyJwtDecoder.resolveApiKey(requestWithAuthorization("bearer " + API_KEY)));
        assertEquals(API_KEY, ApiKeyJwtDecoder.resolveApiKey(requestWithAuthorization("Bearer   " + API_KEY)));
        assertNull(ApiKeyJwtDecoder.resolveApiKey(requestWithAuthorization("Bearer" + API_KEY)));
        assertNull(ApiKeyJwtDecoder.resolveApiKey(requestWithAuthorization("Bearer keycloak-token")));
        assertNull(ApiKeyJwtDecoder.resolveApiKey(requestWithAuthorization("Basic " + API_KEY)));
        assertNull(ApiKeyJwtDecoder.resolveApiKey(new MockHttpServletRequest()));
    }

    private double apiKeyRequests(String outcome) {
        return meterRegistry.counter("prosuna.api.key.requests", "outcome", outcome).count();
    }

    private void givenApiKey(Instant expiration) {
        when(userApiKeyRepository.findByKeyHashOfActiveUser(ApiKeyJwtDecoder.hash(API_KEY))).thenReturn(Optional.of(new UserApiKeyEntity()
                .setId(1)
                .setName("Test")
                .setCreated(Instant.now())
                .setUserId(USER_ID)
                .setExpiration(expiration)
                .setKeyHash(ApiKeyJwtDecoder.hash(API_KEY))));
    }

    private static MockHttpServletRequest requestWithAuthorization(String authorization) {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", authorization);
        return request;
    }
}
