package de.aivot.prosuna.backend.seeding.seeds;

import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import de.aivot.prosuna.backend.identity.enums.IdentityProviderType;
import de.aivot.prosuna.backend.identity.repositories.IdentityProviderRepository;
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
        var seed = new IdentityProvidersSeed(repository);

        when(repository.existsByTypeAndIsTestProvider(any(), anyBoolean())).thenReturn(true);
        assertFalse(seed.shouldSeed());

        when(repository.existsByTypeAndIsTestProvider(IdentityProviderType.MUK, false)).thenReturn(false);
        assertTrue(seed.shouldSeed());
    }

    @Test
    void seedShouldCreateStageAndProductionVariantOfEachSystemIdentityProvider() {
        var repository = mock(IdentityProviderRepository.class);
        var seed = new IdentityProvidersSeed(repository);

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
            assertEquals("/idp/realms/customer/protocol/openid-connect/auth", provider.getAuthorizationEndpoint());
        }
    }

    @Test
    void seedShouldOnlyCreateMissingVariants() {
        var repository = mock(IdentityProviderRepository.class);
        when(repository.existsByTypeAndIsTestProvider(any(), anyBoolean())).thenReturn(true);
        when(repository.existsByTypeAndIsTestProvider(IdentityProviderType.ShId, true)).thenReturn(false);
        var seed = new IdentityProvidersSeed(repository);

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
}
