package de.aivot.prosuna.backend.plugins.core.v1.communication;

import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import de.aivot.prosuna.backend.communication.entities.CommunicationProviderBindingEntity;
import de.aivot.prosuna.backend.communication.entities.CommunicationProviderEntity;
import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.communication.models.CommunicationMessageCallToAction;
import de.aivot.prosuna.backend.communication.models.CommunicationProviderContext;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SecretSelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import de.aivot.prosuna.backend.identity.enums.IdentityProviderType;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.secrets.entities.SecretEntity;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import de.aivot.prosuna.backend.storage.services.StorageService;
import dev.fitko.fitconnect.zbp.model.AuthenticationLevel;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FitConnectZbpCommunicationProviderV1Test {
    private final SecretService secretService = mock(SecretService.class);
    private final FitConnectZbpCommunicationProviderV1 definition = new FitConnectZbpCommunicationProviderV1(
            mock(StorageService.class),
            secretService
    );

    @Test
    void rendersCallToActionsAsEscapedHtmlLinks() throws Exception {
        var message = CommunicationMessage.of(
                "Subject",
                "Body",
                "<p>Body</p>",
                List.of(
                        new CommunicationMessageCallToAction(
                                "Open <portal>",
                                "https://example.test/action?x=1&y=2"
                        ),
                        new CommunicationMessageCallToAction("Show status", "https://example.test/status")
                ),
                List.of()
        );

        var html = FitConnectZbpCommunicationProviderV1.renderMessageHtml(message);

        assertEquals(
                """
                        <p>Body</p>
                        <p><a href="https://example.test/action?x=1&amp;y=2">Open &lt;portal&gt;</a></p>
                        <p><a href="https://example.test/status">Show status</a></p>""",
                html
        );
    }

    @Test
    void rejectsIncompleteCallToActions() {
        var message = CommunicationMessage.of(
                "Subject",
                "Body",
                "<p>Body</p>",
                List.of(new CommunicationMessageCallToAction("Open", " ")),
                List.of()
        );

        assertThrows(
                CommunicationException.class,
                () -> FitConnectZbpCommunicationProviderV1.renderMessageHtml(message)
        );
    }

    @Test
    void configLayoutUsesSecretSelectionWithoutLoadingSecretOptions() throws Exception {
        var layout = definition.getConfigLayout();

        assertTrue(layout.findChild(
                FitConnectZbpCommunicationProviderV1.Config.SENDER_CLIENT_SECRET_KEY_FIELD_ID,
                SecretSelectInputElement.class
        ).isPresent());
        assertTrue(layout.findChild(
                FitConnectZbpCommunicationProviderV1.Config.SENDER_DESTINATION_ID_FIELD_ID,
                TextInputElement.class
        ).orElseThrow().getRequired());
        verifyNoInteractions(secretService);
    }

    @Test
    void testingLayoutRequestsAValidPostfachId() throws Exception {
        var layout = definition.getTestingLayout();
        var postfachId = layout
                .findChild(FitConnectZbpCommunicationProviderV1.TEST_POSTFACH_ID_FIELD_ID, TextInputElement.class)
                .orElseThrow();

        assertEquals("fit-connect-zbp-testing-config", layout.getId());
        assertEquals(
                List.of(FitConnectZbpCommunicationProviderV1.TEST_POSTFACH_ID_FIELD_ID),
                layout.getChildren().stream().map(element -> element.getId()).toList()
        );
        assertEquals("Postfach-ID", postfachId.getLabel());
        assertTrue(postfachId.getRequired());
        assertNotNull(postfachId.getPattern());
        assertDoesNotThrow(() -> postfachId.validate("123e4567-e89b-12d3-a456-426614174000"));
        assertThrows(ValidationException.class, () -> postfachId.validate(""));
        assertThrows(ValidationException.class, () -> postfachId.validate("not-a-uuid"));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void testSendBuildsTemporaryIdentityAndDelegatesToSendMessage() throws Exception {
        var testDefinition = spy(new FitConnectZbpCommunicationProviderV1(
                mock(StorageService.class),
                mock(SecretService.class)
        ));
        var provider = provider();
        var config = config("sender-client", UUID.randomUUID().toString());
        var postfachId = UUID.randomUUID();
        doReturn(Map.of()).when(testDefinition).sendMessage(any(), any(), any());

        testDefinition.handleTest(provider, config, testInputs("  " + postfachId.toString().toUpperCase() + "  "));

        var contextCaptor = ArgumentCaptor.forClass(CommunicationProviderContext.class);
        var identityCaptor = ArgumentCaptor.forClass(IdentityData.class);
        var messageCaptor = ArgumentCaptor.forClass(CommunicationMessage.class);
        verify(testDefinition).sendMessage(contextCaptor.capture(), identityCaptor.capture(), messageCaptor.capture());

        CommunicationProviderContext<FitConnectZbpCommunicationProviderV1.Config,
                FitConnectZbpCommunicationProviderV1.IdentityBinding> context = contextCaptor.getValue();
        var identity = identityCaptor.getValue();
        var message = messageCaptor.getValue();

        assertSame(provider, context.communicationProvider());
        assertSame(config, context.communicationProviderConfiguration());
        assertEquals(IdentityProviderType.Custom, context.identityProvider().getType());
        assertEquals(context.identityProvider().getKey(), context.binding().getIdentityProviderKey());
        assertEquals(provider.getId(), context.binding().getCommunicationProviderId());
        assertEquals(-1, context.binding().getId());
        assertEquals(
                FitConnectZbpCommunicationProviderV1.TEST_POSTFACH_ID_FIELD_ID,
                context.identityProviderBindingConfiguration().bpk2Attribute
        );
        assertNull(context.identityProviderBindingConfiguration().storkQaaLevel);
        assertEquals(context.identityProvider().getKey(), identity.providerKey());
        assertEquals(-1, identity.communicationProviderBindingId());
        assertEquals(
                postfachId.toString(),
                identity.attributes().get(FitConnectZbpCommunicationProviderV1.TEST_POSTFACH_ID_FIELD_ID)
        );
        assertEquals(AuthenticationLevel.ONE, mapAuthenticationLevel(context, identity));
        assertEquals("Testnachricht", message.subject());
        assertEquals("Dies ist eine Testnachricht.", message.body());
        assertEquals("<p>Dies ist eine Testnachricht.</p>", message.htmlBody());
        assertNotNull(message.timestamp());
        assertEquals(List.of(), message.attachments());
    }

    @Test
    void testSendRejectsMissingInvalidAndNonStringPostfachIds() throws Exception {
        var testDefinition = spy(new FitConnectZbpCommunicationProviderV1(
                mock(StorageService.class),
                mock(SecretService.class)
        ));
        var provider = provider();
        var config = config("sender-client", UUID.randomUUID().toString());

        var missing = assertThrows(
                CommunicationException.class,
                () -> testDefinition.handleTest(provider, config, new AuthoredElementValues())
        );
        var blank = assertThrows(
                CommunicationException.class,
                () -> testDefinition.handleTest(provider, config, testInputs("   "))
        );
        var invalid = assertThrows(
                CommunicationException.class,
                () -> testDefinition.handleTest(provider, config, testInputs("not-a-uuid"))
        );
        var nonCanonical = assertThrows(
                CommunicationException.class,
                () -> testDefinition.handleTest(provider, config, testInputs("1-1-1-1-1"))
        );
        var wrongType = assertThrows(
                CommunicationException.class,
                () -> testDefinition.handleTest(provider, config, testInputs(42))
        );

        assertEquals("Die Postfach-ID des Testnutzers ist erforderlich.", missing.getMessage());
        assertEquals("Die Postfach-ID des Testnutzers ist erforderlich.", blank.getMessage());
        assertEquals("Die Postfach-ID des Testnutzers muss eine gültige UUID sein.", invalid.getMessage());
        assertEquals("Die Postfach-ID des Testnutzers muss eine gültige UUID sein.", nonCanonical.getMessage());
        assertEquals("Die Postfach-ID des Testnutzers ist erforderlich.", wrongType.getMessage());
        verify(testDefinition, never()).sendMessage(any(), any(), any());
    }

    @Test
    void testSendPropagatesCommunicationFailure() throws Exception {
        var testDefinition = spy(new FitConnectZbpCommunicationProviderV1(
                mock(StorageService.class),
                mock(SecretService.class)
        ));
        var failure = new CommunicationException("FIT-Connect test failed");
        doThrow(failure).when(testDefinition).sendMessage(any(), any(), any());

        var result = assertThrows(
                CommunicationException.class,
                () -> testDefinition.handleTest(
                        provider(),
                        config("sender-client", UUID.randomUUID().toString()),
                        testInputs(UUID.randomUUID().toString())
                )
        );

        assertSame(failure, result);
    }

    @Test
    void resolvesConfiguredClientSecret() throws Exception {
        var secretKey = UUID.randomUUID();
        var secretEntity = mock(SecretEntity.class);
        var config = config("sender-client", secretKey.toString());
        when(secretService.retrieve(secretKey)).thenReturn(Optional.of(secretEntity));
        when(secretService.decrypt(secretEntity)).thenReturn("decrypted-secret");

        var clientSecret = resolveSenderClientSecret(config);

        assertEquals("decrypted-secret", clientSecret);
        verify(secretService).retrieve(secretKey);
        verify(secretService).decrypt(secretEntity);
    }

    @Test
    void secretResolutionRejectsInvalidSecretKey() {
        var config = config("sender-client", "not-a-uuid");

        var exception = assertThrows(CommunicationException.class, () -> resolveSenderClientSecret(config));

        assertEquals("Failed to parse sender client secret key as UUID: not-a-uuid", exception.getMessage());
        verifyNoInteractions(secretService);
    }

    @Test
    void secretResolutionRejectsMissingSecret() {
        var secretKey = UUID.randomUUID();
        var config = config("sender-client", secretKey.toString());
        when(secretService.retrieve(secretKey)).thenReturn(Optional.empty());

        var exception = assertThrows(CommunicationException.class, () -> resolveSenderClientSecret(config));

        assertEquals("Sender client secret not found: " + secretKey, exception.getMessage());
        verify(secretService).retrieve(secretKey);
    }

    @Test
    void secretResolutionWrapsDecryptionFailure() throws Exception {
        var secretKey = UUID.randomUUID();
        var secretEntity = mock(SecretEntity.class);
        var config = config("sender-client", secretKey.toString());
        var cause = new Exception("decryption failed");
        when(secretService.retrieve(secretKey)).thenReturn(Optional.of(secretEntity));
        when(secretService.decrypt(secretEntity)).thenThrow(cause);

        var exception = assertThrows(CommunicationException.class, () -> resolveSenderClientSecret(config));

        assertEquals("Failed to decrypt sender client secret: " + secretKey, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void authenticationLevelUsesConfiguredAttributeForCustomIdentityProvider() {
        var attributes = Map.of(
                "configured_qaa", "level3",
                "trust_level_authentication", "level4"
        );

        var authenticationLevel = mapAuthenticationLevel("configured_qaa", attributes);

        assertEquals(AuthenticationLevel.THREE, authenticationLevel);
    }

    @Test
    void authenticationLevelMapsKnownValuesAndFallsBackForUnknownValue() {
        assertEquals(AuthenticationLevel.ONE, mapAuthenticationLevel("qaa", Map.of("qaa", "level1")));
        assertEquals(AuthenticationLevel.TWO, mapAuthenticationLevel("qaa", Map.of("qaa", "level2")));
        assertEquals(AuthenticationLevel.THREE, mapAuthenticationLevel("qaa", Map.of("qaa", "level3")));
        assertEquals(AuthenticationLevel.FOUR, mapAuthenticationLevel("qaa", Map.of("qaa", "level4")));
        assertEquals(AuthenticationLevel.ONE, mapAuthenticationLevel("qaa", Map.of("qaa", "unknown")));
        assertEquals(AuthenticationLevel.ONE, mapAuthenticationLevel("qaa", Map.of("qaa", "")));
    }

    @Test
    void authenticationLevelFallsBackWhenBindingAttributeIsMissing() {
        var attributes = Map.of("qaa", "level4");

        assertEquals(AuthenticationLevel.ONE, mapAuthenticationLevel(null, attributes));
        assertEquals(AuthenticationLevel.ONE, mapAuthenticationLevel("", attributes));
        assertEquals(AuthenticationLevel.ONE, mapAuthenticationLevel(" ", attributes));
    }

    @Test
    void authenticationLevelFallsBackWhenIdentityAttributeIsMissing() {
        assertEquals(AuthenticationLevel.ONE, mapAuthenticationLevel("qaa", Map.of()));
    }

    private String resolveSenderClientSecret(FitConnectZbpCommunicationProviderV1.Config config) throws CommunicationException {
        try {
            return ReflectionTestUtils.invokeMethod(definition, "resolveSenderClientSecret", config);
        } catch (UndeclaredThrowableException e) {
            if (e.getUndeclaredThrowable() instanceof CommunicationException communicationException) {
                throw communicationException;
            }
            throw e;
        }
    }

    private AuthenticationLevel mapAuthenticationLevel(String attributeKey, Map<String, String> attributes) {
        var bindingConfig = new FitConnectZbpCommunicationProviderV1.IdentityBinding();
        bindingConfig.storkQaaLevel = attributeKey;
        var context = new CommunicationProviderContext<>(
                mock(CommunicationProviderEntity.class),
                new IdentityProviderEntity().setType(IdentityProviderType.Custom),
                mock(CommunicationProviderBindingEntity.class),
                new FitConnectZbpCommunicationProviderV1.Config(),
                bindingConfig
        );
        var identity = new IdentityData(
                "session-id",
                "identity-id",
                IdentityType.IdentityProvider,
                UUID.randomUUID(),
                "custom",
                "provider-user-123",
                null,
                attributes,
                null,
                Map.of()
        );

        return ReflectionTestUtils.invokeMethod(definition, "mapAuthenticationLevel", context, identity);
    }

    private AuthenticationLevel mapAuthenticationLevel(
            CommunicationProviderContext<FitConnectZbpCommunicationProviderV1.Config, FitConnectZbpCommunicationProviderV1.IdentityBinding> context,
            IdentityData identity
    ) {
        return ReflectionTestUtils.invokeMethod(definition, "mapAuthenticationLevel", context, identity);
    }

    private static FitConnectZbpCommunicationProviderV1.Config config(String clientId, String secretKey) {
        var config = new FitConnectZbpCommunicationProviderV1.Config();
        config.senderClientId = clientId;
        config.senderClientSecret = secretKey;
        return config;
    }

    private static CommunicationProviderEntity provider() {
        var provider = new CommunicationProviderEntity();
        provider.setId(7);
        provider.setCommunicationProviderDefinitionKey("de.aivot.core.fit_connect_zbp_communication_provider");
        provider.setCommunicationProviderDefinitionVersion(1);
        provider.setName("FIT-Connect ZBP");
        provider.setDescription("FIT-Connect ZBP");
        provider.setConfiguration(new AuthoredElementValues());
        provider.setEnabled(true);
        provider.setTestProvider(true);
        return provider;
    }

    private static AuthoredElementValues testInputs(Object postfachId) {
        var inputs = new AuthoredElementValues();
        inputs.put(FitConnectZbpCommunicationProviderV1.TEST_POSTFACH_ID_FIELD_ID, postfachId);
        return inputs;
    }
}
