package de.aivot.prosuna.backend.plugins.core.v1.communication;

import de.aivot.prosuna.backend.communication.entities.CommunicationProviderBindingEntity;
import de.aivot.prosuna.backend.communication.entities.CommunicationProviderEntity;
import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.communication.models.CommunicationProviderContext;
import de.aivot.prosuna.backend.communication.models.MailCommunicationSendOptions;
import de.aivot.prosuna.backend.communication.services.DefaultMailCommunicationService;
import de.aivot.prosuna.backend.elements.models.EffectiveElementValues;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RadioInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.mail.dtos.MailConfigurationResponseDTO;
import de.aivot.prosuna.backend.mail.services.MailConfigurationService;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailCommunicationProviderV1Test {
    private final DefaultMailCommunicationService mailService = mock(DefaultMailCommunicationService.class);
    private final MailConfigurationService mailConfigurationService = mock(MailConfigurationService.class);
    private final MailCommunicationProviderV1 definition = new MailCommunicationProviderV1(
            mailService,
            mailConfigurationService
    );

    @Test
    void configLayoutOffersDefaultAndCustomSendersWithOptionalReplyTo() throws Exception {
        when(mailConfigurationService.getConfiguration()).thenReturn(mailConfiguration("Prosuna", "service@example.test"));

        var layout = definition.getConfigLayout();
        var senderMode = layout.findChild(MailCommunicationProviderV1.SENDER_MODE_FIELD_ID, RadioInputElement.class).orElseThrow();
        var defaultSenderName = layout.findChild(MailCommunicationProviderV1.DEFAULT_SENDER_NAME_FIELD_ID, TextInputElement.class).orElseThrow();
        var defaultSenderAddress = layout.findChild(MailCommunicationProviderV1.DEFAULT_SENDER_ADDRESS_FIELD_ID, TextInputElement.class).orElseThrow();
        var customSenderName = layout.findChild(MailCommunicationProviderV1.CUSTOM_SENDER_NAME_FIELD_ID, TextInputElement.class).orElseThrow();
        var customSenderAddress = layout.findChild(MailCommunicationProviderV1.CUSTOM_SENDER_ADDRESS_FIELD_ID, TextInputElement.class).orElseThrow();
        var replyToAddress = layout.findChild(MailCommunicationProviderV1.REPLY_TO_ADDRESS_FIELD_ID, TextInputElement.class).orElseThrow();

        assertEquals(List.of(
                MailCommunicationProviderV1.SENDER_MODE_FIELD_ID,
                MailCommunicationProviderV1.DEFAULT_SENDER_GROUP_ID,
                MailCommunicationProviderV1.CUSTOM_SENDER_GROUP_ID,
                MailCommunicationProviderV1.REPLY_TO_ADDRESS_FIELD_ID
        ), layout.getChildren().stream().map(element -> element.getId()).toList());
        assertEquals(List.of(
                MailCommunicationProviderV1.SENDER_MODE_DEFAULT,
                MailCommunicationProviderV1.SENDER_MODE_CUSTOM
        ), senderMode.getOptions().stream().map(option -> option.getValue()).toList());
        assertEquals(MailCommunicationProviderV1.SENDER_MODE_DEFAULT, staticValue(senderMode));
        assertEquals("Absender", senderMode.getLabel());
        assertTrue(senderMode.getRequired());
        assertTrue(defaultSenderName.getDisabled());
        assertTrue(defaultSenderAddress.getDisabled());
        assertEquals("Prosuna", staticValue(defaultSenderName));
        assertEquals("service@example.test", staticValue(defaultSenderAddress));
        assertTrue(customSenderName.getRequired());
        assertEquals(255, customSenderName.getMaxCharacters());
        assertTrue(customSenderAddress.getRequired());
        assertEquals("email", customSenderAddress.getAutocomplete());
        assertEquals(254, customSenderAddress.getMaxCharacters());
        assertFalse(replyToAddress.getRequired());
        assertEquals("email", replyToAddress.getAutocomplete());
        assertEquals(254, replyToAddress.getMaxCharacters());
        assertNotNull(customSenderAddress.getPattern());
        assertNotNull(replyToAddress.getPattern());
        assertNotNull(layout.findChild(MailCommunicationProviderV1.DEFAULT_SENDER_GROUP_ID).orElseThrow().getVisibility());
        assertNotNull(layout.findChild(MailCommunicationProviderV1.CUSTOM_SENDER_GROUP_ID).orElseThrow().getVisibility());

        assertThrows(ValidationException.class, () -> customSenderName.validate(" "));
        assertThrows(ValidationException.class, () -> customSenderAddress.validate("first@example.test,second@example.test"));
        assertDoesNotThrow(() -> replyToAddress.validate(""));
        assertThrows(ValidationException.class, () -> replyToAddress.validate("first@example.test,second@example.test"));
    }

    @Test
    void configLayoutMarksMissingDefaultSenderValues() throws Exception {
        when(mailConfigurationService.getConfiguration()).thenReturn(mailConfiguration(null, null));

        var layout = definition.getConfigLayout();

        assertEquals(
                "Nicht konfiguriert",
                layout.findChild(MailCommunicationProviderV1.DEFAULT_SENDER_NAME_FIELD_ID, TextInputElement.class)
                        .map(MailCommunicationProviderV1Test::staticValue).orElseThrow()
        );
        assertEquals(
                "Nicht konfiguriert",
                layout.findChild(MailCommunicationProviderV1.DEFAULT_SENDER_ADDRESS_FIELD_ID, TextInputElement.class)
                        .map(MailCommunicationProviderV1Test::staticValue).orElseThrow()
        );
    }

    @Test
    void mapsAnnotatedCustomSenderConfiguration() throws Exception {
        var values = new EffectiveElementValues();
        values.put(MailCommunicationProviderV1.SENDER_MODE_FIELD_ID, MailCommunicationProviderV1.SENDER_MODE_CUSTOM);
        values.put(MailCommunicationProviderV1.CUSTOM_SENDER_NAME_FIELD_ID, "Custom Service");
        values.put(MailCommunicationProviderV1.CUSTOM_SENDER_ADDRESS_FIELD_ID, "custom@example.test");
        values.put(MailCommunicationProviderV1.REPLY_TO_ADDRESS_FIELD_ID, "replies@example.test");

        var config = ElementPOJOMapper.mapToPOJO(values, MailCommunicationProviderV1.Config.class);

        assertEquals(MailCommunicationProviderV1.SENDER_MODE_CUSTOM, config.senderMode);
        assertNotNull(config.customSender);
        assertEquals("Custom Service", config.customSender.name);
        assertEquals("custom@example.test", config.customSender.address);
        assertEquals("replies@example.test", config.replyToAddress);
    }

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

    @Test
    void sendsWithTheDefaultSenderAndConfiguredReplyTo() throws Exception {
        var config = new MailCommunicationProviderV1.Config();
        config.senderMode = MailCommunicationProviderV1.SENDER_MODE_DEFAULT;
        config.replyToAddress = "replies@example.test";
        var bindingConfig = new MailCommunicationProviderV1.IdentityBinding();
        bindingConfig.emailAttribute = "mail";
        var message = message();

        definition.sendMessage(context(config, bindingConfig), identity(Map.of("mail", "customer@example.test")), message);

        verify(mailService).sendMessage(
                eq("customer@example.test"),
                same(message),
                argThat(options -> options.senderName() == null
                        && options.senderAddress() == null
                        && options.replyToAddress().equals("replies@example.test"))
        );
    }

    @Test
    void sendsWithCustomSenderAndReplyTo() throws Exception {
        var config = new MailCommunicationProviderV1.Config();
        config.senderMode = MailCommunicationProviderV1.SENDER_MODE_CUSTOM;
        config.customSender = new MailCommunicationProviderV1.CustomSenderConfig();
        config.customSender.name = "Custom Service";
        config.customSender.address = "custom@example.test";
        config.replyToAddress = "replies@example.test";
        var bindingConfig = new MailCommunicationProviderV1.IdentityBinding();
        bindingConfig.emailAttribute = "mail";
        var message = message();

        definition.sendMessage(context(config, bindingConfig), identity(Map.of("mail", "customer@example.test")), message);

        verify(mailService).sendMessage(
                eq("customer@example.test"),
                same(message),
                argThat(options -> options.equals(MailCommunicationSendOptions.customSender(
                        "Custom Service",
                        "custom@example.test",
                        "replies@example.test"
                )))
        );
    }

    private static CommunicationProviderContext<MailCommunicationProviderV1.Config, MailCommunicationProviderV1.IdentityBinding> context(
            MailCommunicationProviderV1.IdentityBinding bindingConfig
    ) {
        return context(new MailCommunicationProviderV1.Config(), bindingConfig);
    }

    private static CommunicationProviderContext<MailCommunicationProviderV1.Config, MailCommunicationProviderV1.IdentityBinding> context(
            MailCommunicationProviderV1.Config config,
            MailCommunicationProviderV1.IdentityBinding bindingConfig
    ) {
        return new CommunicationProviderContext<>(
                new CommunicationProviderEntity(),
                new IdentityProviderEntity(),
                new CommunicationProviderBindingEntity(),
                config,
                bindingConfig
        );
    }

    private static CommunicationMessage message() {
        return new CommunicationMessage("Subject", "Body", "Body", Instant.now(), List.of());
    }

    private static Object staticValue(de.aivot.prosuna.backend.elements.models.elements.BaseInputElement<?> input) {
        return ((NoCodeStaticValue) input.getValue().getNoCode()).getValue();
    }

    private static MailConfigurationResponseDTO mailConfiguration(String senderName, String senderAddress) {
        return new MailConfigurationResponseDTO(
                true,
                "smtp.example.test",
                587,
                true,
                "u******",
                true,
                true,
                senderName,
                senderAddress,
                List.of()
        );
    }

    private static IdentityData identity(Map<String, String> attributes) {
        return new IdentityData(
                "session", "applicant", IdentityType.IdentityProvider, UUID.randomUUID(), "metadata", null,
                attributes, 1, Map.of()
        );
    }
}
