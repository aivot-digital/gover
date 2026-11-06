package de.aivot.prosuna.backend.plugins.core.v1.communication;

import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import de.aivot.prosuna.backend.communication.entities.CommunicationProviderBindingEntity;
import de.aivot.prosuna.backend.communication.entities.CommunicationProviderEntity;
import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.communication.models.CommunicationMessageCallToAction;
import de.aivot.prosuna.backend.communication.models.CommunicationProviderContext;
import de.aivot.prosuna.backend.department.entities.DepartmentEntity;
import de.aivot.prosuna.backend.department.entities.VDepartmentShadowedEntity;
import de.aivot.prosuna.backend.asset.services.AssetContentResolverService;
import de.aivot.prosuna.backend.elements.enums.AssetVisibility;
import de.aivot.prosuna.backend.elements.enums.ValidationFunctionType;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.form.content.AlertContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.AssetSelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SecretSelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import de.aivot.prosuna.backend.enums.AlertType;
import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import de.aivot.prosuna.backend.identity.enums.IdentityProviderType;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.plugins.core.v1.operators.text.NoCodeRegexMatchOperator;
import de.aivot.prosuna.backend.secrets.entities.SecretEntity;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElementOption;
import dev.fitko.fitconnect.rest.client.config.FitConnectEnvironment;
import dev.fitko.fitconnect.rest.model.event.Event;
import dev.fitko.fitconnect.rest.model.submission.PublicService;
import dev.fitko.fitconnect.rest.model.submission.SentSubmission;
import dev.fitko.fitconnect.sdk.FitConnectSdk;
import dev.fitko.fitconnect.sdk.api.OutgoingSubmission;
import dev.fitko.fitconnect.sdk.api.event.CaseEvent;
import dev.fitko.fitconnect.sdk.api.event.TransferLog;
import dev.fitko.fitconnect.sdk.clients.OnlineService;
import dev.fitko.fitconnect.sdk.clients.OnlineServiceCases;
import dev.fitko.fitconnect.sdk.clients.Organisation;
import dev.fitko.fitconnect.sdk.clients.OrganisationCases;
import dev.fitko.fitconnect.sdk.exceptions.FitConnectConfigurationException;
import dev.fitko.fitconnect.zbp.model.AuthenticationLevel;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FitConnectZbpCommunicationProviderV1Test {
    private static final UUID DESTINATION_ID = UUID.randomUUID();
    private static final UUID SENDER_DESTINATION_ID = UUID.randomUUID();
    private static final SentSubmission SENT_SUBMISSION = new SentSubmission(
            DESTINATION_ID,
            SENDER_DESTINATION_ID,
            UUID.randomUUID(),
            UUID.randomUUID(),
            List.of(),
            new PublicService("ZBP Message Forwarding", "urn:schema-fitko-de:fit-connect:id.bund.de:message_v6"),
            null,
            null,
            null
    );

    private final SecretService secretService = mock(SecretService.class);
    private final AssetContentResolverService assetContentResolverService = mock(AssetContentResolverService.class);
    private final FitConnectZbpCommunicationProviderV1 definition = new FitConnectZbpCommunicationProviderV1(
            assetContentResolverService,
            secretService
    );

    @Test
    void rendersCallToActionsAsEscapedHtmlLinks() throws Exception {
        var message = new CommunicationMessage.Builder()
                .subject("Subject")
                .body("Body")
                .htmlBody("<p>Body</p>")
                .callToActions(List.of(
                        new CommunicationMessageCallToAction(
                                "Open <portal>",
                                "https://example.test/action?x=1&y=2"
                        ),
                        new CommunicationMessageCallToAction("Show status", "https://example.test/status")
                ))
                .build();

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
    void appendsEscapedDepartmentSignatureAfterCallToActions() throws Exception {
        var message = new CommunicationMessage.Builder()
                .subject("Subject")
                .body("Body")
                .htmlBody("<p>Body</p>")
                .callToActions(List.of(new CommunicationMessageCallToAction("Open", "https://example.test")))
                .signatureDepartment(new VDepartmentShadowedEntity()
                        .setId(17)
                        .setDefaultMailSignature("Viele <Grüße>\nBürgerbüro"))
                .build();

        var html = FitConnectZbpCommunicationProviderV1.renderMessageHtml(message);

        assertEquals(
                """
                        <p>Body</p>
                        <p><a href="https://example.test">Open</a></p>
                        <div>Viele &lt;Gr&uuml;&szlig;e&gt;<br>B&uuml;rgerb&uuml;ro</div>""",
                html
        );
    }

    @Test
    void rejectsIncompleteCallToActions() {
        var message = new CommunicationMessage.Builder()
                .subject("Subject")
                .body("Body")
                .htmlBody("<p>Body</p>")
                .callToActions(List.of(new CommunicationMessageCallToAction("Open", " ")))
                .build();

        assertThrows(
                CommunicationException.class,
                () -> FitConnectZbpCommunicationProviderV1.renderMessageHtml(message)
        );
    }

    @Test
    void createsZbpMessageWithDepartmentAsSenderAndService() throws Exception {
        var department = new DepartmentEntity()
                .setId(17)
                .setName(" Fachbereich Leistungen ");
        var message = new CommunicationMessage.Builder()
                .subject("Subject")
                .body("Body")
                .htmlBody("<p>Body</p>")
                .sendingDepartment(department)
                .build();
        var mailboxId = UUID.randomUUID();

        var zbpMessage = FitConnectZbpCommunicationProviderV1.createZbpMessage(
                message,
                mailboxId,
                AuthenticationLevel.THREE,
                List.of()
        );

        assertEquals("Fachbereich Leistungen", zbpMessage.sender());
        assertEquals(" Fachbereich Leistungen ", zbpMessage.service());
        assertEquals("<p>Body</p>", zbpMessage.content());
        assertEquals("Subject", zbpMessage.title());
        assertEquals(mailboxId, zbpMessage.mailboxUuid());
        assertEquals(AuthenticationLevel.THREE, zbpMessage.stork_qaa_level());
    }

    @Test
    void createsZbpMessageWithProsunaSenderAndNoServiceWhenOnlySendingUserIsSet() throws Exception {
        var user = new UserEntity().setId("user-1").setFullName("Sender User");
        var message = new CommunicationMessage.Builder()
                .subject("Subject")
                .body("Body")
                .htmlBody("<p>Body</p>")
                .sendingUser(user)
                .build();

        var zbpMessage = FitConnectZbpCommunicationProviderV1.createZbpMessage(
                message,
                UUID.randomUUID(),
                AuthenticationLevel.ONE,
                List.of()
        );

        assertEquals("Prosuna", zbpMessage.sender());
        assertNull(zbpMessage.service());
        assertEquals("<p>Body</p>", zbpMessage.content());
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
        var privateKey = layout.findChild(
                FitConnectZbpCommunicationProviderV1.Config.ZBP_CERTIFICATE_PRIVATE_KEY_ASSET_KEY_FIELD_ID,
                AssetSelectInputElement.class
        ).orElseThrow();
        var clientCertificate = layout.findChild(
                FitConnectZbpCommunicationProviderV1.Config.ZBP_CERTIFICATE_CLIENT_CERT_ASSET_KEY_FIELD_ID,
                AssetSelectInputElement.class
        ).orElseThrow();
        assertEquals(List.of("application/pkcs8"), privateKey.getAllowedMimeTypes());
        assertEquals(List.of("application/x-x509-ca-cert"), clientCertificate.getAllowedMimeTypes());
        assertEquals("Privater Schlüssel", privateKey.getLabel());
        assertEquals("BundID-Postfach-Zertifikat", clientCertificate.getLabel());
        assertTrue(layout.getChildren().indexOf(clientCertificate) < layout.getChildren().indexOf(privateKey));
        assertEquals(AssetVisibility.Private, privateKey.getAssetVisibility());
        assertEquals(AssetVisibility.Private, clientCertificate.getAssetVisibility());
        assertTrue(privateKey.getRequired());
        assertTrue(clientCertificate.getRequired());
        verifyNoInteractions(secretService);
    }

    @Test
    void configLayoutOffersOptionalSenderDestinationType() throws Exception {
        var field = definition.getConfigLayout().findChild(
                FitConnectZbpCommunicationProviderV1.Config.SENDER_DESTINATION_TYPE_FIELD_ID,
                SelectInputElement.class
        ).orElseThrow();

        assertNotEquals(Boolean.TRUE, field.getRequired());
        assertEquals(
                List.of(
                        FitConnectZbpCommunicationProviderV1.Config.SENDER_DESTINATION_TYPE_ADMINISTRATION,
                        FitConnectZbpCommunicationProviderV1.Config.SENDER_DESTINATION_TYPE_ONLINE_SERVICE
                ),
                field.getOptions().stream().map(SelectInputElementOption::getValue).toList()
        );
    }

    @Test
    void sendsAsOrganisationForAdministrationSenderDestination() throws Exception {
        var fixture = sendFixture(FitConnectZbpCommunicationProviderV1.Config.SENDER_DESTINATION_TYPE_ADMINISTRATION);
        var organisation = mock(Organisation.class);
        var cases = mock(OrganisationCases.class);
        when(fixture.sdk().organisation(SENDER_DESTINATION_ID)).thenReturn(organisation);
        when(organisation.send(any(OutgoingSubmission.class))).thenReturn(SENT_SUBMISSION);
        when(organisation.cases()).thenReturn(cases);
        when(cases.logOf(SENT_SUBMISSION)).thenReturn(transferLog(Event.ACCEPT_SUBMISSION));

        var result = fixture.send();

        assertEquals(SENT_SUBMISSION.submissionId().toString(), result.get("submissionId"));
        assertEquals("ACCEPTED", result.get("status"));
        verify(fixture.sdk(), never()).onlineService(any(UUID.class));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = FitConnectZbpCommunicationProviderV1.Config.SENDER_DESTINATION_TYPE_ONLINE_SERVICE)
    void sendsAsOnlineServiceForOnlineServiceOrUnsetSenderDestinationType(String senderDestinationType) throws Exception {
        var fixture = sendFixture(senderDestinationType);
        var onlineService = mock(OnlineService.class);
        var cases = mock(OnlineServiceCases.class);
        when(fixture.sdk().onlineService(SENDER_DESTINATION_ID)).thenReturn(onlineService);
        when(onlineService.send(any(OutgoingSubmission.class))).thenReturn(SENT_SUBMISSION);
        when(onlineService.cases()).thenReturn(cases);
        when(cases.logOf(SENT_SUBMISSION)).thenReturn(transferLog(Event.ACCEPT_SUBMISSION));

        var result = fixture.send();

        assertEquals(SENT_SUBMISSION.submissionId().toString(), result.get("submissionId"));
        verify(fixture.sdk(), never()).organisation(any(UUID.class));
    }

    @ParameterizedTest
    @CsvSource(value = {"NULL, TEST", "TEST, TEST", "STAGE, STAGE", "PROD, PROD"}, nullValues = "NULL")
    void sendsInConfiguredEnvironmentAndFallsBackToTest(String environment, String expectedEnvironmentName) throws Exception {
        var expectedEnvironment = Map.of(
                "TEST", FitConnectEnvironment.TEST,
                "STAGE", FitConnectEnvironment.STAGE,
                "PROD", FitConnectEnvironment.PROD
        ).get(expectedEnvironmentName);
        var fixture = sendFixture(FitConnectZbpCommunicationProviderV1.Config.SENDER_DESTINATION_TYPE_ONLINE_SERVICE);
        fixture.context().communicationProviderConfiguration().environment = environment;
        var onlineService = mock(OnlineService.class);
        var cases = mock(OnlineServiceCases.class);
        when(fixture.sdk().onlineService(SENDER_DESTINATION_ID)).thenReturn(onlineService);
        when(onlineService.send(any(OutgoingSubmission.class))).thenReturn(SENT_SUBMISSION);
        when(onlineService.cases()).thenReturn(cases);
        when(cases.logOf(SENT_SUBMISSION)).thenReturn(transferLog(Event.ACCEPT_SUBMISSION));

        fixture.send();

        verify(fixture.definition()).createSdk("sender-client", "sender-secret", expectedEnvironment);
    }

    @Test
    void configLayoutOffersOptionalEnvironment() throws Exception {
        var field = definition.getConfigLayout().findChild(
                FitConnectZbpCommunicationProviderV1.Config.ENVIRONMENT_FIELD_ID,
                SelectInputElement.class
        ).orElseThrow();

        assertNotEquals(Boolean.TRUE, field.getRequired());
        assertEquals(
                List.of("TEST", "STAGE", "PROD"),
                field.getOptions().stream().map(SelectInputElementOption::getValue).toList()
        );
    }

    @Test
    void clientCreationFailureKeepsFitConnectMessage() throws Exception {
        var fixture = sendFixture(FitConnectZbpCommunicationProviderV1.Config.SENDER_DESTINATION_TYPE_ONLINE_SERVICE);
        var cause = new FitConnectConfigurationException(
                "Destination %s is type A, not C — a destination's type is fixed at creation.".formatted(SENDER_DESTINATION_ID)
        );
        when(fixture.sdk().onlineService(SENDER_DESTINATION_ID)).thenThrow(cause);

        var exception = assertThrows(CommunicationException.class, fixture::send);

        assertEquals(
                "Die FIT-Connect-Verbindung für den Zustellpunkt (Sender) „%s“ konnte nicht aufgebaut werden: %s"
                        .formatted(SENDER_DESTINATION_ID, cause.getMessage()),
                exception.getMessage()
        );
        assertSame(cause, exception.getCause());
    }

    @Test
    void sendFailureKeepsFitConnectMessage() throws Exception {
        var fixture = sendFixture(FitConnectZbpCommunicationProviderV1.Config.SENDER_DESTINATION_TYPE_ADMINISTRATION);
        var organisation = mock(Organisation.class);
        var cause = new IllegalStateException("Submission rejected by destination");
        when(fixture.sdk().organisation(SENDER_DESTINATION_ID)).thenReturn(organisation);
        when(organisation.send(any(OutgoingSubmission.class))).thenThrow(cause);

        var exception = assertThrows(CommunicationException.class, fixture::send);

        assertEquals("Der Versand über FIT-Connect ist fehlgeschlagen: Submission rejected by destination", exception.getMessage());
        assertSame(cause, exception.getCause());
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
        assertRegexValidation(
                postfachId,
                "Bitte geben Sie eine gültige UUID ein.",
                "123e4567-e89b-12d3-a456-426614174000",
                "not-a-uuid"
        );
        assertThrows(ValidationException.class, () -> postfachId.validate(""));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void testSendBuildsTemporaryIdentityAndDelegatesToSendMessage() throws Exception {
        var testDefinition = spy(new FitConnectZbpCommunicationProviderV1(
                mock(AssetContentResolverService.class),
                mock(SecretService.class)
        ));
        var provider = provider();
        var config = config("sender-client", UUID.randomUUID().toString());
        var postfachId = UUID.randomUUID();
        var submissionId = UUID.randomUUID();
        doReturn(Map.of(
                "postfachId", postfachId.toString(),
                "submissionId", submissionId.toString(),
                "status", "ACCEPTED"
        )).when(testDefinition).sendMessage(any(), any(), any());

        var result = testDefinition.handleTest(
                provider,
                config,
                testInputs("  " + postfachId.toString().toUpperCase() + "  ")
        );

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
        var alert = result
                .findChild("fit-connect-zbp-testing-result-alert", AlertContentElement.class)
                .orElseThrow();
        assertEquals("fit-connect-zbp-testing-result", result.getId());
        assertEquals(AlertType.Success, alert.getAlertType());
        assertEquals("Testnachricht erfolgreich übermittelt", alert.getTitle());
        assertEquals(
                "Postfach-ID: %s\nSubmission-ID: %s\nStatus: ACCEPTED".formatted(postfachId, submissionId),
                alert.getText()
        );
    }

    @Test
    void testSendRejectsMissingInvalidAndNonStringPostfachIds() throws Exception {
        var testDefinition = spy(new FitConnectZbpCommunicationProviderV1(
                mock(AssetContentResolverService.class),
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
                mock(AssetContentResolverService.class),
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

    private SendFixture sendFixture(String senderDestinationType) throws Exception {
        var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        var authorKeyPair = keyPairGenerator.generateKeyPair();
        var privateKeyPem = pem("PRIVATE KEY", authorKeyPair.getPrivate().getEncoded());
        var certificatePem = pem("CERTIFICATE", selfSignedCertificate(authorKeyPair).getEncoded());
        when(assetContentResolverService.resolveContent(eq("private-key"), eq(AssetVisibility.Private), anyString()))
                .thenReturn(privateKeyPem);
        when(assetContentResolverService.resolveContent(eq("client-cert"), eq(AssetVisibility.Private), anyString()))
                .thenReturn(certificatePem);

        var secretKey = UUID.randomUUID();
        var secret = mock(SecretEntity.class);
        when(secretService.retrieve(secretKey)).thenReturn(Optional.of(secret));
        when(secretService.decrypt(secret)).thenReturn("sender-secret");

        var config = config("sender-client", secretKey.toString());
        config.destinationId = DESTINATION_ID.toString();
        config.senderDestinationId = SENDER_DESTINATION_ID.toString();
        config.senderDestinationType = senderDestinationType;
        config.zbpCertificatePrivateKeyAssetKey = "private-key";
        config.zbpCertificateClientCertAssetKey = "client-cert";

        var bindingConfig = new FitConnectZbpCommunicationProviderV1.IdentityBinding();
        bindingConfig.bpk2Attribute = "postfach";
        var context = new CommunicationProviderContext<>(
                provider(),
                new IdentityProviderEntity().setType(IdentityProviderType.Custom),
                mock(CommunicationProviderBindingEntity.class),
                config,
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
                Map.of("postfach", UUID.randomUUID().toString()),
                null,
                Map.of()
        );

        var sdk = mock(FitConnectSdk.class);
        var testDefinition = spy(definition);
        doReturn(sdk).when(testDefinition).createSdk(eq("sender-client"), eq("sender-secret"), any());
        return new SendFixture(testDefinition, sdk, context, identity);
    }

    private record SendFixture(
            FitConnectZbpCommunicationProviderV1 definition,
            FitConnectSdk sdk,
            CommunicationProviderContext<FitConnectZbpCommunicationProviderV1.Config, FitConnectZbpCommunicationProviderV1.IdentityBinding> context,
            IdentityData identity
    ) {
        Map<String, Object> send() throws CommunicationException {
            return definition.sendMessage(context, identity, new CommunicationMessage.Builder()
                    .subject("Subject")
                    .body("Body")
                    .htmlBody("<p>Body</p>")
                    .referenceId("9QXA-D0B8-MR9P")
                    .build());
        }
    }

    private static X509Certificate selfSignedCertificate(KeyPair keyPair) throws Exception {
        var subject = new X500Name("CN=Prosuna Test");
        var now = Instant.now();
        var certificate = new JcaX509v3CertificateBuilder(
                subject,
                BigInteger.ONE,
                Date.from(now.minus(1, ChronoUnit.DAYS)),
                Date.from(now.plus(1, ChronoUnit.DAYS)),
                subject,
                keyPair.getPublic()
        ).build(sha256WithRsaSigner(keyPair.getPrivate()));
        return new JcaX509CertificateConverter().getCertificate(certificate);
    }

    // Signs with the JDK because BouncyCastle's JcaContentSignerBuilder fails to load with the transitively mixed bcprov/bcpkix versions.
    private static ContentSigner sha256WithRsaSigner(PrivateKey privateKey) {
        var content = new ByteArrayOutputStream();
        return new ContentSigner() {
            @Override
            public AlgorithmIdentifier getAlgorithmIdentifier() {
                return new AlgorithmIdentifier(PKCSObjectIdentifiers.sha256WithRSAEncryption, DERNull.INSTANCE);
            }

            @Override
            public OutputStream getOutputStream() {
                return content;
            }

            @Override
            public byte[] getSignature() {
                try {
                    var signature = Signature.getInstance("SHA256withRSA");
                    signature.initSign(privateKey);
                    signature.update(content.toByteArray());
                    return signature.sign();
                } catch (GeneralSecurityException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }

    private static byte[] pem(String type, byte[] der) {
        var body = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(der);
        return "-----BEGIN %s-----\n%s\n-----END %s-----\n".formatted(type, body, type).getBytes(StandardCharsets.UTF_8);
    }

    private static TransferLog transferLog(Event event) {
        return new TransferLog(List.of(
                CaseEvent.builder()
                        .event(event)
                        .issueTime(Instant.now())
                        .problems(List.of())
                        .build()
        ));
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
        inputs.putLiteral(FitConnectZbpCommunicationProviderV1.TEST_POSTFACH_ID_FIELD_ID, postfachId);
        return inputs;
    }

    private static void assertRegexValidation(TextInputElement field,
                                              String expectedMessage,
                                              String acceptedValue,
                                              String rejectedValue) throws Exception {
        var validation = field.getValidation();
        assertNotNull(validation);
        assertEquals(ValidationFunctionType.NoCode, validation.getType());
        assertEquals(List.of(field.getId()), List.copyOf(validation.getReferencedIds()));

        var wrapper = validation.getNoCodeList().getFirst();
        assertEquals(expectedMessage, wrapper.getMessage());
        var expression = assertInstanceOf(NoCodeExpression.class, wrapper.getNoCode());
        assertEquals(NoCodeRegexMatchOperator.OPERATOR_ID, expression.getOperatorIdentifier());

        var operands = List.copyOf(expression.getOperands());
        assertEquals(NoCodeReference.of(field.getId()), operands.getFirst());
        var regex = assertInstanceOf(String.class, assertInstanceOf(NoCodeStaticValue.class, operands.get(1)).getValue());
        var operator = new NoCodeRegexMatchOperator();
        assertTrue(operator.performEvaluation(DerivedRuntimeElementData.empty(), acceptedValue, regex).getValueAsBoolean());
        assertFalse(operator.performEvaluation(DerivedRuntimeElementData.empty(), rejectedValue, regex).getValueAsBoolean());
    }
}
