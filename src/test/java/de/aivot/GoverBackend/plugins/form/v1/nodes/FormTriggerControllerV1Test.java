package de.aivot.GoverBackend.plugins.form.v1.nodes;

import de.aivot.GoverBackend.elements.models.elements.form.input.IdentityInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.IdentityInputElementOption;
import de.aivot.GoverBackend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.steps.GenericStepElement;
import de.aivot.GoverBackend.identity.entities.IdentityProviderEntity;
import de.aivot.GoverBackend.identity.enums.IdentityProviderType;
import de.aivot.GoverBackend.identity.services.IdentityProviderService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FormTriggerControllerV1Test {
    @Mock
    private IdentityProviderService identityProviderService;

    private FormTriggerControllerV1 controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        controller = new FormTriggerControllerV1(
                null,
                null,
                null,
                null,
                identityProviderService,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    @Test
    void resolveIdentityProviders_ShouldReturnEnabledProvidersInEncounterOrderAndDeduplicate() throws ResponseException {
        var providerKeyA = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
        var providerKeyB = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
        var providerKeyC = UUID.fromString("00000000-0000-0000-0000-0000000000c3");

        when(identityProviderService.retrieve(providerKeyA))
                .thenReturn(Optional.of(identityProvider(providerKeyA, "BundID", true)));
        when(identityProviderService.retrieve(providerKeyB))
                .thenReturn(Optional.of(identityProvider(providerKeyB, "Disabled Provider", false)));
        when(identityProviderService.retrieve(providerKeyC))
                .thenReturn(Optional.of(identityProvider(providerKeyC, "BayernID", true)));

        var formLayout = new FormLayoutElement()
                .setChildren(List.of(
                        new GenericStepElement()
                                .setChildren(List.of(
                                        new IdentityInputElement()
                                                .setOptions(List.of(
                                                        option(providerKeyA),
                                                        option(providerKeyB),
                                                        option(providerKeyA)
                                                )),
                                        new IdentityInputElement()
                                                .setOptions(List.of(
                                                        option(null),
                                                        option(providerKeyC)
                                                ))
                                ))
                ));

        var result = controller.resolveIdentityProviders(formLayout);

        assertEquals(2, result.size());
        assertEquals(providerKeyA, result.get(0).key());
        assertEquals("BundID", result.get(0).name());
        assertEquals(providerKeyC, result.get(1).key());
        assertEquals("BayernID", result.get(1).name());

        verify(identityProviderService).retrieve(providerKeyA);
        verify(identityProviderService).retrieve(providerKeyB);
        verify(identityProviderService).retrieve(providerKeyC);
    }

    @Test
    void resolveIdentityProviders_ShouldReturnEmptyListForNullFormLayout() throws ResponseException {
        var result = controller.resolveIdentityProviders(null);

        assertTrue(result.isEmpty());
        verifyNoInteractions(identityProviderService);
    }

    private static IdentityInputElementOption option(UUID identityProviderKey) {
        return new IdentityInputElementOption()
                .setIdentityProviderKey(identityProviderKey);
    }

    private static IdentityProviderEntity identityProvider(UUID key, String name, boolean isEnabled) {
        return new IdentityProviderEntity()
                .setKey(key)
                .setName(name)
                .setType(IdentityProviderType.Custom)
                .setMetadataIdentifier(name.toLowerCase().replace(' ', '-'))
                .setIsEnabled(isEnabled);
    }
}
