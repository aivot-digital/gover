package de.aivot.prosuna.backend.plugins.core.v1.communication;

import de.aivot.prosuna.backend.communication.entities.CommunicationProviderBindingEntity;
import de.aivot.prosuna.backend.communication.entities.CommunicationProviderEntity;
import de.aivot.prosuna.backend.communication.models.CommunicationProviderContext;
import de.aivot.prosuna.backend.communication.services.DefaultMailCommunicationService;
import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class MailCommunicationProviderV1Test {
    private final MailCommunicationProviderV1 definition = new MailCommunicationProviderV1(
            mock(DefaultMailCommunicationService.class)
    );

    @Test
    void mappedEmailSkipsCustomerInput() {
        var bindingConfig = new MailCommunicationProviderV1.IdentityBinding();
        bindingConfig.emailAttribute = "mail";
        var context = context(bindingConfig);
        var identity = identity(Map.of("mail", "customer@example.test"));

        assertNull(definition.getCustomerLayout(context, identity));
    }

    @Test
    void missingOrInvalidMappedEmailRequestsCustomerInput() {
        var bindingConfig = new MailCommunicationProviderV1.IdentityBinding();
        bindingConfig.emailAttribute = "mail";
        var context = context(bindingConfig);

        assertNotNull(definition.getCustomerLayout(context, identity(Map.of())));
        assertNotNull(definition.getCustomerLayout(context, identity(Map.of("mail", "invalid"))));
    }

    @Test
    void attributeMappingIsOptional() {
        var bindingConfig = new MailCommunicationProviderV1.IdentityBinding();

        assertNotNull(definition.getCustomerLayout(context(bindingConfig), identity(Map.of("email", "customer@example.test"))));
    }

    private static CommunicationProviderContext<MailCommunicationProviderV1.Config, MailCommunicationProviderV1.IdentityBinding> context(
            MailCommunicationProviderV1.IdentityBinding bindingConfig
    ) {
        return new CommunicationProviderContext<>(
                new CommunicationProviderEntity(),
                new IdentityProviderEntity(),
                new CommunicationProviderBindingEntity(),
                new MailCommunicationProviderV1.Config(),
                bindingConfig
        );
    }

    private static IdentityData identity(Map<String, String> attributes) {
        return new IdentityData(
                "session", "applicant", IdentityType.IdentityProvider, UUID.randomUUID(), "metadata", null,
                attributes, 1, Map.of()
        );
    }
}
