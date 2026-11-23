package de.aivot.prosuna.backend.seeding.seeds;

import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import de.aivot.prosuna.backend.identity.enums.IdentityProviderType;
import de.aivot.prosuna.backend.identity.repositories.IdentityProviderRepository;
import de.aivot.prosuna.backend.models.config.KeycloakConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityProvidersSeedTest {
    @Test
    void shouldSeedOnlyWhileASystemIdentityProviderIsMissing() {
        var repository = mock(IdentityProviderRepository.class);
        var seed = new IdentityProvidersSeed(repository, keycloakConfig());

        when(repository.existsByTypeAndIsTestProvider(any(), anyBoolean())).thenReturn(true);
        assertFalse(seed.shouldSeed());

        when(repository.existsByTypeAndIsTestProvider(IdentityProviderType.MUK, false)).thenReturn(false);
        assertTrue(seed.shouldSeed());
    }

    @Test
    void seedShouldCreateStageAndProductionVariantOfEachSystemIdentityProvider() {
        var repository = mock(IdentityProviderRepository.class);
        var seed = new IdentityProvidersSeed(repository, keycloakConfig());

        seed.seed();

        var providers = ArgumentCaptor.forClass(IdentityProviderEntity.class);
        verify(repository, times(8)).save(providers.capture());
        assertEquals(
                List.of(
                        "BayernID (Vorproduktion)", "BayernID",
                        "BundID (Vorproduktion)", "BundID",
                        "Servicekonto Schleswig-Holstein (Vorproduktion)", "Servicekonto Schleswig-Holstein",
                        "Mein Unternehmenskonto (Vorproduktion)", "Mein Unternehmenskonto"
                ),
                providers.getAllValues().stream().map(IdentityProviderEntity::getName).toList()
        );

        var bayernIdStage = providers.getAllValues().getFirst();
        assertEquals(IdentityProviderType.BayernId, bayernIdStage.getType());
        assertEquals("bayern_id", bayernIdStage.getMetadataIdentifier());
        assertEquals("Konfiguration für die Vorproduktionsumgebung der BayernID.", bayernIdStage.getDescription());
        assertTrue(bayernIdStage.getIsTestProvider());
        assertEquals("bayernid-stage", hint(bayernIdStage));
        assertEquals(List.of("email", "profile", "openid", "bayernid"), bayernIdStage.getDefaultScopes());
        assertEquals("bPK2", bayernIdStage.getUniqueIdAttribute());
        assertEquals(25, bayernIdStage.getAttributes().size());

        var mukProduction = providers.getAllValues().getLast();
        assertEquals(IdentityProviderType.MUK, mukProduction.getType());
        assertEquals("Konfiguration für die Produktionsumgebung des Mein Unternehmenskonto (MUK).", mukProduction.getDescription());
        assertFalse(mukProduction.getIsTestProvider());
        assertEquals("muk", hint(mukProduction));
        assertEquals("preferred_username", mukProduction.getUniqueIdAttribute());

        for (var provider : providers.getAllValues()) {
            assertFalse(provider.getIsEnabled());
            assertEquals("S256", provider.getPkceMethod());
            assertEquals("app", provider.getClientId());
            assertEquals("https://auth.example.com/kc/realms/customer/protocol/openid-connect/auth", provider.getAuthorizationEndpoint());
            assertEquals("https://auth.example.com/kc/realms/customer/protocol/openid-connect/token", provider.getTokenEndpoint());
            assertEquals("https://auth.example.com/kc/realms/customer/protocol/openid-connect/userinfo", provider.getUserinfoEndpoint());
            assertEquals("https://auth.example.com/kc/realms/customer/protocol/openid-connect/logout", provider.getEndSessionEndpoint());
        }
    }

    @Test
    void seedShouldMoveExistingSystemProvidersToTheConfiguredKeycloakHostname() {
        var repository = mock(IdentityProviderRepository.class);
        when(repository.existsByTypeAndIsTestProvider(any(), anyBoolean())).thenReturn(true);
        var seed = new IdentityProvidersSeed(repository, keycloakConfig());

        var current = new IdentityProviderEntity()
                .setAuthorizationEndpoint("https://auth.example.com/kc/realms/customer/protocol/openid-connect/auth")
                .setTokenEndpoint("https://auth.example.com/kc/realms/customer/protocol/openid-connect/token")
                .setUserinfoEndpoint("https://auth.example.com/kc/realms/customer/protocol/openid-connect/userinfo")
                .setEndSessionEndpoint("https://auth.example.com/kc/realms/customer/protocol/openid-connect/logout");
        when(repository.findAllByTypeNot(IdentityProviderType.Custom)).thenReturn(List.of(current));
        assertFalse(seed.shouldSeed());

        // Installations seeded before the hostname was configurable assumed Keycloak below /idp of the Prosuna host.
        var outdated = new IdentityProviderEntity()
                .setAuthorizationEndpoint("/idp/realms/customer/protocol/openid-connect/auth")
                .setTokenEndpoint("/idp/realms/customer/protocol/openid-connect/token")
                .setUserinfoEndpoint("/idp/realms/customer/protocol/openid-connect/userinfo")
                .setEndSessionEndpoint("/idp/realms/customer/protocol/openid-connect/logout");
        when(repository.findAllByTypeNot(IdentityProviderType.Custom)).thenReturn(List.of(current, outdated));
        assertTrue(seed.shouldSeed());

        seed.seed();

        verify(repository).save(outdated);
        verify(repository, times(1)).save(any());
        assertEquals(current.getAuthorizationEndpoint(), outdated.getAuthorizationEndpoint());
        assertEquals(current.getTokenEndpoint(), outdated.getTokenEndpoint());
        assertEquals(current.getUserinfoEndpoint(), outdated.getUserinfoEndpoint());
        assertEquals(current.getEndSessionEndpoint(), outdated.getEndSessionEndpoint());
    }

    @Test
    void seedShouldOnlyCreateMissingVariants() {
        var repository = mock(IdentityProviderRepository.class);
        when(repository.existsByTypeAndIsTestProvider(any(), anyBoolean())).thenReturn(true);
        when(repository.existsByTypeAndIsTestProvider(IdentityProviderType.ShId, true)).thenReturn(false);
        var seed = new IdentityProvidersSeed(repository, keycloakConfig());

        seed.seed();

        var providers = ArgumentCaptor.forClass(IdentityProviderEntity.class);
        verify(repository).save(providers.capture());
        assertEquals("Servicekonto Schleswig-Holstein (Vorproduktion)", providers.getValue().getName());
    }

    private static String hint(IdentityProviderEntity provider) {
        var parameter = provider.getAdditionalParams().getFirst();
        assertEquals("kc_idp_hint", parameter.getKey());
        return parameter.getValue();
    }

    private static KeycloakConfig keycloakConfig() {
        var config = new KeycloakConfig();
        config.setHostname("https://auth.example.com/kc/");
        return config;
    }
}
