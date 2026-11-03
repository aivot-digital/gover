package de.aivot.prosuna.backend.security;

import de.aivot.prosuna.backend.user.entities.UserApiKeyEntity;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.repositories.UserApiKeyRepository;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
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
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ApiKeyJwtDecoder decoder = new ApiKeyJwtDecoder(keycloakDecoder, userApiKeyRepository, userRepository);

    @Test
    void hashesAsLowercaseHexSha256() {
        // Matches encode(sha256('abc'), 'hex') in PostgreSQL, so keys can be stored by SQL
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", ApiKeyJwtDecoder.hash("abc"));
    }

    @Test
    void resolvesTheKeyUserAsSubject() {
        var expiration = Instant.now().plusSeconds(60);
        givenApiKey(expiration);
        var user = givenUser(true, false);

        var jwt = decoder.decode(API_KEY);

        assertSame(user, assertInstanceOf(ApiKeyJwtDecoder.ApiKeyPrincipal.class, jwt).getUser());
        assertEquals(USER_ID, jwt.getSubject());
        assertEquals(expiration, jwt.getExpiresAt());
        verifyNoInteractions(keycloakDecoder);
    }

    @Test
    void delegatesOtherTokensToKeycloak() {
        var keycloakJwt = mock(Jwt.class);
        when(keycloakDecoder.decode("keycloak-token")).thenReturn(keycloakJwt);

        assertSame(keycloakJwt, decoder.decode("keycloak-token"));
        verifyNoInteractions(userApiKeyRepository);
    }

    @Test
    void rejectsUnknownKey() {
        when(userApiKeyRepository.findByKeyHash(anyString())).thenReturn(Optional.empty());

        assertThrows(BadJwtException.class, () -> decoder.decode(API_KEY));
    }

    @Test
    void rejectsExpiredKey() {
        givenApiKey(Instant.now().minusSeconds(1));
        givenUser(true, false);

        assertThrows(BadJwtException.class, () -> decoder.decode(API_KEY));
    }

    @Test
    void rejectsKeyOfDisabledUser() {
        givenApiKey(Instant.now().plusSeconds(60));
        givenUser(false, false);

        assertThrows(BadJwtException.class, () -> decoder.decode(API_KEY));
    }

    @Test
    void rejectsKeyOfUserDeletedInIdp() {
        givenApiKey(Instant.now().plusSeconds(60));
        givenUser(true, true);

        assertThrows(BadJwtException.class, () -> decoder.decode(API_KEY));
    }

    @Test
    void resolvesOnlyPrefixedBearerTokensFromTheAuthorizationHeader() {
        assertEquals(API_KEY, ApiKeyJwtDecoder.resolveApiKey(requestWithAuthorization("Bearer " + API_KEY)));
        assertEquals(API_KEY, ApiKeyJwtDecoder.resolveApiKey(requestWithAuthorization("bearer " + API_KEY)));
        assertNull(ApiKeyJwtDecoder.resolveApiKey(requestWithAuthorization("Bearer keycloak-token")));
        assertNull(ApiKeyJwtDecoder.resolveApiKey(requestWithAuthorization("Basic " + API_KEY)));
        assertNull(ApiKeyJwtDecoder.resolveApiKey(new MockHttpServletRequest()));
    }

    private void givenApiKey(Instant expiration) {
        when(userApiKeyRepository.findByKeyHash(ApiKeyJwtDecoder.hash(API_KEY))).thenReturn(Optional.of(new UserApiKeyEntity()
                .setId(1)
                .setUserId(USER_ID)
                .setExpiration(expiration)
                .setKeyHash(ApiKeyJwtDecoder.hash(API_KEY))));
    }

    private UserEntity givenUser(boolean enabled, boolean deletedInIdp) {
        var user = new UserEntity()
                .setId(USER_ID)
                .setEnabled(enabled)
                .setDeletedInIdp(deletedInIdp);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        return user;
    }

    private static MockHttpServletRequest requestWithAuthorization(String authorization) {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", authorization);
        return request;
    }
}
