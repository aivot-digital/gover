package de.aivot.prosuna.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeycloakJwtAuthenticationConverterTest {
    private final JwtAuthenticationConverter converter = new SecurityConfiguration(null, null)
            .jwtAuthenticationConverterForKeycloak();

    @Test
    void convertShouldMapRealmRolesToAuthorities() {
        var jwt = jwtBuilder()
                .claim("realm_access", Map.of("roles", List.of("offline_access", "uma_authorization")))
                .build();

        assertEquals(Set.of("ROLE_OFFLINE_ACCESS", "ROLE_UMA_AUTHORIZATION"), roleAuthorities(jwt));
    }

    @Test
    void convertShouldAcceptTokenWithoutRealmAccess() {
        var jwt = jwtBuilder().build();

        assertTrue(roleAuthorities(jwt).isEmpty());
    }

    @Test
    void convertShouldAcceptRealmAccessWithoutRoles() {
        var jwt = jwtBuilder()
                .claim("realm_access", Map.of())
                .build();

        assertTrue(roleAuthorities(jwt).isEmpty());
    }

    private Set<String> roleAuthorities(Jwt jwt) {
        return converter.convert(jwt)
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .collect(Collectors.toSet());
    }

    private Jwt.Builder jwtBuilder() {
        return Jwt.withTokenValue("staff-access-token")
                .header("alg", "none")
                .subject("staff-user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60));
    }
}
