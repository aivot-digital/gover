package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.identity.entities.IdentityProviderEntity;
import de.aivot.GoverBackend.identity.enums.IdentityProviderType;
import de.aivot.GoverBackend.identity.services.IdentityProviderService;
import de.aivot.GoverBackend.utils.SpringContext;
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
import static org.mockito.Mockito.when;

class IdentityConfigElementTest {
    private static final String MISSING_OPTION_MESSAGE = "Für jede Identität muss mindestens ein Identitätsanbieter ausgewählt werden.";

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
        var applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBean(IdentityProviderService.class)).thenReturn(identityProviderService);
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
