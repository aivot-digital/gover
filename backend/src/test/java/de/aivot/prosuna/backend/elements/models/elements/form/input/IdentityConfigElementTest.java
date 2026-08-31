package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.communication.entities.CommunicationProviderBindingEntity;
import de.aivot.prosuna.backend.communication.services.CommunicationService;
import de.aivot.prosuna.backend.elements.models.elements.form.input.IdentityConfigElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.IdentityConfigElementOption;
import de.aivot.prosuna.backend.elements.models.elements.form.input.IdentityConfigElementSlot;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import de.aivot.prosuna.backend.identity.enums.IdentityProviderType;
import de.aivot.prosuna.backend.identity.services.IdentityProviderService;
import de.aivot.prosuna.backend.utils.SpringContext;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class IdentityConfigElementTest {
    private static final String MISSING_OPTION_MESSAGE = "Für jede Identität muss mindestens ein Nutzerkontenanbieter oder die direkte E-Mail-Eingabe aktiviert werden.";

    @Test
    void shouldRejectSlotWithoutSelectedOption() {
        var element = new IdentityConfigElement();
        var slot = new IdentityConfigElementSlot()
                .setOptions(List.of());

        var exception = assertThrows(ValidationException.class, () -> element.performValidation(List.of(slot)));

        assertEquals(MISSING_OPTION_MESSAGE, exception.getMessage());
    }

    @Test
    void shouldRejectSlotWithNullOptions() {
        var element = new IdentityConfigElement();
        var slot = new IdentityConfigElementSlot();

        var exception = assertThrows(ValidationException.class, () -> element.performValidation(List.of(slot)));

        assertEquals(MISSING_OPTION_MESSAGE, exception.getMessage());
    }

    @Test
    void shouldRejectSlotWithoutIdentityProviderKey() {
        var element = new IdentityConfigElement();
        var slot = new IdentityConfigElementSlot()
                .setOptions(List.of(new IdentityConfigElementOption()));

        var exception = assertThrows(ValidationException.class, () -> element.performValidation(List.of(slot)));

        assertEquals(MISSING_OPTION_MESSAGE, exception.getMessage());
    }

    @Test
    void shouldAcceptEmailOnlySlot() {
        var element = new IdentityConfigElement();
        var slot = new IdentityConfigElementSlot()
                .setAllowsMail(true)
                .setOptions(List.of());

        assertDoesNotThrow(() -> element.performValidation(List.of(slot)));
    }

    @Test
    void shouldAcceptSlotWithSelectedCustomProviderWithoutTrustLevel() throws Exception {
        var providerKey = UUID.randomUUID();
        var identityProviderService = mockIdentityProviderService(
                identityProvider(providerKey, IdentityProviderType.Custom, "Custom")
        );

        var element = new IdentityConfigElement();
        var slot = new IdentityConfigElementSlot()
                .setOptions(List.of(new IdentityConfigElementOption()
                        .setIdentityProviderKey(providerKey)));

        var previousContext = setIdentityProviderService(identityProviderService);
        try {
            assertDoesNotThrow(() -> element.performValidation(List.of(slot)));
        } finally {
            setSpringContext(previousContext);
        }
    }

    @Test
    void shouldAcceptSlotWithSelectedMukProviderWithoutTrustLevel() throws Exception {
        var providerKey = UUID.randomUUID();
        var identityProviderService = mockIdentityProviderService(
                identityProvider(providerKey, IdentityProviderType.MUK, "Mein Unternehmenskonto")
        );

        var element = new IdentityConfigElement();
        var slot = new IdentityConfigElementSlot()
                .setOptions(List.of(new IdentityConfigElementOption()
                        .setIdentityProviderKey(providerKey)));

        var previousContext = setIdentityProviderService(identityProviderService);
        try {
            assertDoesNotThrow(() -> element.performValidation(List.of(slot)));
        } finally {
            setSpringContext(previousContext);
        }
    }

    @Test
    void shouldRejectTrustLevelProviderWithoutTrustLevel() throws Exception {
        var providerKey = UUID.randomUUID();
        var identityProviderService = mockIdentityProviderService(
                identityProvider(providerKey, IdentityProviderType.BundId, "BundID")
        );

        var element = new IdentityConfigElement();
        var slot = new IdentityConfigElementSlot()
                .setOptions(List.of(new IdentityConfigElementOption()
                        .setIdentityProviderKey(providerKey)
                        .setAdditionalScopes(List.of())));

        var previousContext = setIdentityProviderService(identityProviderService);
        try {
            var exception = assertThrows(ValidationException.class, () -> element.performValidation(List.of(slot)));

            assertEquals("Für den Identitätsanbieter \"BundID\" muss ein Mindest-Vertrauensniveau ausgewählt werden.", exception.getMessage());
        } finally {
            setSpringContext(previousContext);
        }
    }

    @Test
    void shouldRejectTrustLevelProviderWithBlankTrustLevel() throws Exception {
        var providerKey = UUID.randomUUID();
        var identityProviderService = mockIdentityProviderService(
                identityProvider(providerKey, IdentityProviderType.BayernId, "BayernID")
        );

        var element = new IdentityConfigElement();
        var slot = new IdentityConfigElementSlot()
                .setOptions(List.of(new IdentityConfigElementOption()
                        .setIdentityProviderKey(providerKey)
                        .setAdditionalScopes(List.of("  "))));

        var previousContext = setIdentityProviderService(identityProviderService);
        try {
            var exception = assertThrows(ValidationException.class, () -> element.performValidation(List.of(slot)));

            assertEquals("Für den Identitätsanbieter \"BayernID\" muss ein Mindest-Vertrauensniveau ausgewählt werden.", exception.getMessage());
        } finally {
            setSpringContext(previousContext);
        }
    }

    @Test
    void shouldRejectAllTrustLevelProvidersWithoutTrustLevel() throws Exception {
        var bayernIdProviderKey = UUID.randomUUID();
        var bundIdProviderKey = UUID.randomUUID();
        var identityProviderService = mockIdentityProviderService(
                identityProvider(bayernIdProviderKey, IdentityProviderType.BayernId, "BayernID"),
                identityProvider(bundIdProviderKey, IdentityProviderType.BundId, "BundID")
        );

        var element = new IdentityConfigElement();
        var slot = new IdentityConfigElementSlot()
                .setOptions(List.of(
                        new IdentityConfigElementOption()
                                .setIdentityProviderKey(bayernIdProviderKey)
                                .setAdditionalScopes(List.of()),
                        new IdentityConfigElementOption()
                                .setIdentityProviderKey(bundIdProviderKey)
                                .setAdditionalScopes(List.of())
                ));

        var previousContext = setIdentityProviderService(identityProviderService);
        try {
            var exception = assertThrows(ValidationException.class, () -> element.performValidation(List.of(slot)));

            assertEquals(
                    "Für die Identitätsanbieter \"BayernID\", \"BundID\" muss ein Mindest-Vertrauensniveau ausgewählt werden.",
                    exception.getMessage()
            );
        } finally {
            setSpringContext(previousContext);
        }
    }

    @Test
    void shouldAcceptTrustLevelProviderWithSelectedTrustLevel() throws Exception {
        var providerKey = UUID.randomUUID();
        var identityProviderService = mockIdentityProviderService(
                identityProvider(providerKey, IdentityProviderType.ShId, "Servicekonto Schleswig-Holstein")
        );

        var element = new IdentityConfigElement();
        var slot = new IdentityConfigElementSlot()
                .setOptions(List.of(new IdentityConfigElementOption()
                        .setIdentityProviderKey(providerKey)
                        .setAdditionalScopes(List.of("urn:example:trust-level"))));

        var previousContext = setIdentityProviderService(identityProviderService);
        try {
            assertDoesNotThrow(() -> element.performValidation(List.of(slot)));
        } finally {
            setSpringContext(previousContext);
        }
    }

    @Test
    void shouldRejectUnknownIdentityProvider() throws Exception {
        var providerKey = UUID.randomUUID();
        var identityProviderService = mock(IdentityProviderService.class);
        when(identityProviderService.retrieve(providerKey)).thenReturn(Optional.empty());

        var element = new IdentityConfigElement();
        var slot = new IdentityConfigElementSlot()
                .setOptions(List.of(new IdentityConfigElementOption()
                        .setIdentityProviderKey(providerKey)));

        var previousContext = setIdentityProviderService(identityProviderService);
        try {
            var exception = assertThrows(ValidationException.class, () -> element.performValidation(List.of(slot)));

            assertEquals("Ein ausgewählter Identitätsanbieter konnte nicht gefunden werden.", exception.getMessage());
        } finally {
            setSpringContext(previousContext);
        }
    }

    @Test
    void shouldRejectProviderWithoutCommunicationBinding() throws Exception {
        var providerKey = UUID.randomUUID();
        var identityProvider = identityProvider(providerKey, IdentityProviderType.Custom, "Custom");
        var identityProviderService = mockIdentityProviderService(identityProvider);
        var communicationService = mock(CommunicationService.class);
        when(communicationService.getUsableBindings(identityProvider)).thenReturn(List.of());
        var slot = new IdentityConfigElementSlot()
                .setOptions(List.of(new IdentityConfigElementOption().setIdentityProviderKey(providerKey)));

        var previousContext = setServices(identityProviderService, communicationService);
        try {
            var exception = assertThrows(
                    ValidationException.class,
                    () -> new IdentityConfigElement().performValidation(List.of(slot))
            );
            assertEquals(
                    "Für den Identitätsanbieter \"Custom\" ist keine verwendbare Kommunikationsanbindung konfiguriert.",
                    exception.getMessage()
            );
        } finally {
            setSpringContext(previousContext);
        }
    }

    private static IdentityProviderService mockIdentityProviderService(IdentityProviderEntity... identityProviders) throws Exception {
        var identityProviderService = mock(IdentityProviderService.class);
        for (var identityProvider : identityProviders) {
            when(identityProviderService.retrieve(identityProvider.getKey())).thenReturn(Optional.of(identityProvider));
        }
        return identityProviderService;
    }

    private static IdentityProviderEntity identityProvider(UUID key, IdentityProviderType type, String name) {
        return new IdentityProviderEntity()
                .setKey(key)
                .setType(type)
                .setName(name);
    }

    private static ApplicationContext setIdentityProviderService(IdentityProviderService identityProviderService) throws Exception {
        var communicationService = mock(CommunicationService.class);
        when(communicationService.getUsableBindings(any())).thenReturn(List.of(mock(CommunicationProviderBindingEntity.class)));
        return setServices(identityProviderService, communicationService);
    }

    private static ApplicationContext setServices(IdentityProviderService identityProviderService,
                                                  CommunicationService communicationService) throws Exception {
        var applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBean(IdentityProviderService.class)).thenReturn(identityProviderService);
        when(applicationContext.getBean(CommunicationService.class)).thenReturn(communicationService);
        return setSpringContext(applicationContext);
    }

    private static ApplicationContext setSpringContext(ApplicationContext applicationContext) throws Exception {
        Field field = SpringContext.class.getDeclaredField("context");
        field.setAccessible(true);
        var previousContext = (ApplicationContext) field.get(null);
        field.set(null, applicationContext);
        return previousContext;
    }
}
