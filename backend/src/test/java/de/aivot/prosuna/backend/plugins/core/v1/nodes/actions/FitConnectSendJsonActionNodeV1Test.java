package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.BaseInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.ProcessDataKeyInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.prosuna.backend.process.entities.*;
import de.aivot.prosuna.backend.process.enums.ProcessNodeConfigurationValidationPhase;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeConfigurationValidationContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.secrets.entities.SecretEntity;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import dev.fitko.fitconnect.core.http.api.RestApiException;
import dev.fitko.fitconnect.rest.client.config.FitConnectEnvironment;
import dev.fitko.fitconnect.rest.model.event.Event;
import dev.fitko.fitconnect.rest.model.event.problems.Problem;
import dev.fitko.fitconnect.rest.model.submission.PublicService;
import dev.fitko.fitconnect.rest.model.submission.SentSubmission;
import dev.fitko.fitconnect.sdk.api.OutgoingSubmission;
import dev.fitko.fitconnect.sdk.api.event.CaseEvent;
import dev.fitko.fitconnect.sdk.api.event.TransferLog;
import dev.fitko.fitconnect.sdk.clients.OnlineService;
import dev.fitko.fitconnect.sdk.clients.OnlineServiceCases;
import dev.fitko.fitconnect.sdk.exceptions.FitConnectSenderException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class FitConnectSendJsonActionNodeV1Test {
    private static final UUID DESTINATION_ID = UUID.fromString("22cc92dc-6145-4d29-ab18-4e8b4baaf3a8");
    private static final UUID SENDER_DESTINATION_ID = UUID.fromString("6396c078-0ab0-4aa6-ac78-386fd8e21dd7");
    private static final UUID SECRET_ID = UUID.fromString("114b3dcc-a8e4-43cc-a7d7-017bfe13b432");

    @Test
    void configurationLayoutExplainsFitConnectSettingsAndKeepsEnvironmentWithoutDefault() throws Exception {
        var node = new FitConnectSendJsonActionNodeV1(mock(SecretService.class), mock(JsonMapper.class));
        var layout = node.getConfigurationLayout(new ProcessNodeDefinitionConfigurationLayoutContext(
                null,
                mock(ProcessEntity.class),
                mock(ProcessVersionEntity.class),
                mock(ProcessNodeEntity.class)
        ));

        var environment = layout
                .findChild(
                        FitConnectSendJsonActionNodeV1.SendDataFitConnectActionNodeV1Config.ENVIRONMENT_FIELD_ID,
                        SelectInputElement.class
                )
                .orElseThrow();

        assertTrue(environment.getRequired());
        assertNull(environment.getValue());
        assertEquals("Daten über FIT-Connect senden", node.getName());
        assertTrue(environment.getHint().contains("Der Prosuna-Testmodus ändert diese Auswahl nicht."));
        assertEquals(
                List.of(
                        SelectInputElementOption.of("TEST", "TEST"),
                        SelectInputElementOption.of("STAGE", "STAGE"),
                        SelectInputElementOption.of("PROD", "PROD")
                ),
                environment.getOptions()
        );

        var serviceName = assertInstanceOf(BaseInputElement.class, layout.findChild("serviceName").orElseThrow());
        assertEquals("Bezeichnung der Verwaltungsleistung", serviceName.getLabel());
        var senderDestination = assertInstanceOf(BaseInputElement.class, layout.findChild("senderDestinationId").orElseThrow());
        assertEquals("Zustellpunkt-ID des Onlinedienstes", senderDestination.getLabel());
        var secret = assertInstanceOf(BaseInputElement.class, layout.findChild("senderClientSecret").orElseThrow());
        assertEquals("Client-Secret", secret.getLabel());
        assertTrue(secret.getHint().contains("in Prosuna hinterlegte Geheimnis"));
        var path = layout.findChild("jsonDatasetProcessKey", ProcessDataKeyInputElement.class).orElseThrow();
        assertEquals("Vorgangsdatenpfad der JSON-Daten", path.getLabel());
        assertTrue(path.getHint().contains("Präfix $. steht für die Vorgangsdatenebene"));
        var schema = assertInstanceOf(BaseInputElement.class, layout.findChild("jsonSchemaLink").orElseThrow());
        assertEquals("JSON-Schema-Referenz (URI)", schema.getLabel());

        var port = node.getPorts(validConfig("TEST")).getFirst();
        assertEquals("success", port.key());
        assertEquals("Einreichung gesendet", port.label());
        assertTrue(port.description().contains("Eingereicht (SUBMITTED)"));
        assertTrue(port.description().contains("Angenommen (ACCEPTED)"));
    }

    @Test
    void configurationValidationAcceptsOnlySupportedEnvironmentsAndValidDestinationIds() {
        var node = new FitConnectSendJsonActionNodeV1(mock(SecretService.class), mock(JsonMapper.class));
        var processNode = mock(ProcessNodeEntity.class);

        for (var value : List.of("TEST", "STAGE", "PROD", " stage ")) {
            assertNull(node.validateConfiguration(new ProcessNodeConfigurationValidationContext<>(processNode, validConfig(value), DerivedRuntimeElementData.empty(), ProcessNodeConfigurationValidationPhase.Authoring)));
        }

        for (var value : new String[]{null, "", "STAGING", "unknown"}) {
            var errors = node.validateConfiguration(new ProcessNodeConfigurationValidationContext<>(processNode, validConfig(value), DerivedRuntimeElementData.empty(), ProcessNodeConfigurationValidationPhase.Authoring));
            assertEquals(
                    List.of("Wählen Sie eine unterstützte FIT-Connect-Umgebung aus."),
                    errors.get(FitConnectSendJsonActionNodeV1.SendDataFitConnectActionNodeV1Config.ENVIRONMENT_FIELD_ID)
            );
        }

        var config = validConfig("TEST");
        config.destinationId = "invalid";
        config.senderDestinationId = "invalid";
        var errors = node.validateConfiguration(new ProcessNodeConfigurationValidationContext<>(processNode, config, DerivedRuntimeElementData.empty(), ProcessNodeConfigurationValidationPhase.Authoring));
        assertEquals(
                List.of("Geben Sie eine gültige Empfänger-Zustellpunkt-ID im UUID-Format an."),
                errors.get(FitConnectSendJsonActionNodeV1.SendDataFitConnectActionNodeV1Config.DESTINATION_ID_FIELD_ID)
        );
        assertEquals(
                List.of("Geben Sie eine gültige Zustellpunkt-ID des Onlinedienstes im UUID-Format an."),
                errors.get(FitConnectSendJsonActionNodeV1.SendDataFitConnectActionNodeV1Config.SENDER_DESTINATION_ID_FIELD_ID)
        );
    }

    @Test
    void usesConfiguredEnvironmentAndCompletesViaSuccessPort() throws Exception {
        var fixture = fixture();
        when(fixture.cases().logOf(fixture.sentSubmission())).thenReturn(
                transferLog(Event.SUBMIT_SUBMISSION),
                transferLog(Event.ACCEPT_SUBMISSION),
                transferLog(Event.ACCEPT_SUBMISSION)
        );
        var createdEnvironments = new ArrayList<FitConnectEnvironment>();
        doAnswer(invocation -> {
            createdEnvironments.add(invocation.getArgument(2));
            assertEquals(SENDER_DESTINATION_ID, invocation.getArgument(3));
            return fixture.onlineService();
        }).when(fixture.node()).createOnlineService(
                anyString(), anyString(), any(FitConnectEnvironment.class), any(UUID.class)
        );

        for (var environment : List.of("TEST", "STAGE", "PROD")) {
            var processData = new ProcessExecutionData().addProcessData("payload", Map.of("name", "Ada"));
            var result = assertInstanceOf(
                    ProcessNodeExecutionResultTaskCompleted.class,
                    fixture.node().init(context(validConfig(environment), processData))
            );

            assertEquals("success", result.getViaPort());
            assertSame(processData.getProcessData(), result.getProcessData());
            assertTrue(result.getNodeData().isEmpty());
        }

        assertEquals(
                List.of(FitConnectEnvironment.TEST, FitConnectEnvironment.STAGE, FitConnectEnvironment.PROD),
                createdEnvironments
        );
    }

    @Test
    void mapsMissingSdkResourceToInvalidConfigurationAndKeepsCause() throws Exception {
        var fixture = fixture();
        var cause = new FitConnectSenderException(
                "Could not load internal resource with private details",
                new RestApiException("Resource does not exist.", 404)
        );
        when(fixture.onlineService().send(any(OutgoingSubmission.class))).thenThrow(cause);

        var exception = assertThrows(
                ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> fixture.node().init(context(validConfig("STAGE"), new ProcessExecutionData()))
        );

        assertEquals(
                "Eine für den FIT-Connect-Versand benötigte Ressource wurde nicht gefunden. Empfänger-Zustellpunkt: „%s“. Umgebung: „STAGE“.".formatted(DESTINATION_ID),
                exception.getMessage()
        );
        assertSame(cause, exception.getCause());
    }

    @Test
    void mapsGeneralSendingFailureToUnknownExecutionError() throws Exception {
        var fixture = fixture();
        var cause = new FitConnectSenderException("Authentifizierung fehlgeschlagen: private client details");
        when(fixture.onlineService().send(any(OutgoingSubmission.class)))
                .thenThrow(cause);

        var exception = assertThrows(
                ProcessNodeExecutionExceptionUnknown.class,
                () -> fixture.node().init(context(validConfig("PROD"), new ProcessExecutionData()))
        );

        assertEquals(
                "Beim Senden des JSON-Datensatzes an den FIT-Connect-Zustellpunkt „%s“ in der Umgebung „PROD“ ist ein Fehler aufgetreten.".formatted(DESTINATION_ID),
                exception.getMessage()
        );
        assertSame(cause, exception.getCause());
    }

    @Test
    void rejectsMissingEnvironmentDuringExecution() throws Exception {
        var fixture = fixture();

        var exception = assertThrows(
                ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> fixture.node().init(context(validConfig(null), new ProcessExecutionData()))
        );

        assertEquals("Wählen Sie TEST, STAGE oder PROD als FIT-Connect-Umgebung aus.", exception.getMessage());
    }

    @Test
    void mapsClientInitializationFailureToUnknownExecutionError() throws Exception {
        var fixture = fixture();
        var cause = new IllegalStateException("SDK-Konfiguration ungültig: private client details");
        doThrow(cause)
                .when(fixture.node())
                .createOnlineService(anyString(), anyString(), any(FitConnectEnvironment.class), any(UUID.class));

        var exception = assertThrows(
                ProcessNodeExecutionExceptionUnknown.class,
                () -> fixture.node().init(context(validConfig("STAGE"), new ProcessExecutionData()))
        );

        assertEquals(
                "Der FIT-Connect-Versand konnte für den Onlinedienst-Zustellpunkt „%s“ in der Umgebung „STAGE“ nicht vorbereitet werden.".formatted(SENDER_DESTINATION_ID),
                exception.getMessage()
        );
        assertSame(cause, exception.getCause());
    }

    @Test
    void mapsStatusRetrievalFailureToUnknownExecutionError() throws Exception {
        var fixture = fixture();
        var cause = new FitConnectSenderException("Statusdienst nicht erreichbar: private API details");
        when(fixture.cases().logOf(fixture.sentSubmission()))
                .thenThrow(cause);

        var exception = assertThrows(
                ProcessNodeExecutionExceptionUnknown.class,
                () -> fixture.node().init(context(validConfig("TEST"), new ProcessExecutionData()))
        );

        assertEquals(
                "Der JSON-Datensatz wurde an FIT-Connect übergeben. Der Status der Einreichung für den Zustellpunkt „%s“ in der Umgebung „TEST“ konnte nicht abgefragt werden.".formatted(DESTINATION_ID),
                exception.getMessage()
        );
        assertSame(cause, exception.getCause());
        verify(fixture.onlineService()).send(any(OutgoingSubmission.class));
    }

    @ParameterizedTest
    @EnumSource(value = Event.class, names = {"REJECT_SUBMISSION", "NOTIFY_SUBMISSION"})
    void reportsUnsupportedStatusWithoutRawProblemList(Event event) throws Exception {
        var fixture = fixture();
        var problem = new Problem("urn:test:problem", "Private problem title", "Private diagnostic detail", "private-instance");
        when(fixture.cases().logOf(fixture.sentSubmission()))
                .thenReturn(new TransferLog(List.of(
                        CaseEvent.builder().event(event).issueTime(Instant.now()).problems(List.of(problem)).build()
                )));

        var exception = assertThrows(
                ProcessNodeExecutionExceptionUnknown.class,
                () -> fixture.node().init(context(validConfig("TEST"), new ProcessExecutionData()))
        );

        assertEquals(
                "FIT-Connect meldet für die Einreichung an den Zustellpunkt „%s“ in der Umgebung „TEST“ den Status „%s“. Dieses Prozesselement setzt den Vorgang nur bei „Eingereicht (SUBMITTED)“ oder „Angenommen (ACCEPTED)“ fort."
                        .formatted(DESTINATION_ID, event.getState()),
                exception.getMessage()
        );
        assertFalse(exception.getMessage().contains(problem.toString()));
    }

    @Test
    void rejectsInvalidSchemaReferenceWithoutExposingUriAndKeepsCause() throws Exception {
        var fixture = fixture();
        var config = validConfig("TEST");
        config.jsonSchemaLink = "https://private-user:private-secret@schema.example/%";

        var exception = assertThrows(
                ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> fixture.node().init(context(config, new ProcessExecutionData()))
        );

        assertEquals("Die JSON-Schema-Referenz ist keine gültige URI.", exception.getMessage());
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        verifyNoInteractions(fixture.onlineService(), fixture.secretService());
    }

    @Test
    void rejectsInvalidSecretReferenceWithPercentSignsAndKeepsCause() throws Exception {
        var fixture = fixture();
        var config = validConfig("TEST");
        config.senderClientSecret = "ungueltiges-geheimnis-%s-%";

        var exception = assertThrows(
                ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> fixture.node().init(context(config, new ProcessExecutionData()))
        );

        assertEquals(
                "Wählen Sie ein gültiges gespeichertes Geheimnis mit dem Client-Secret des Onlinedienstes aus.",
                exception.getMessage()
        );
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        verifyNoInteractions(fixture.onlineService(), fixture.secretService());
    }

    @Test
    void reportsMissingSelectedSecretWithoutExposingItsReference() throws Exception {
        var fixture = fixture();
        when(fixture.secretService().retrieve(SECRET_ID)).thenReturn(Optional.empty());

        var exception = assertThrows(
                ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> fixture.node().init(context(validConfig("TEST"), new ProcessExecutionData()))
        );

        assertEquals("Das ausgewählte Geheimnis mit dem Client-Secret wurde nicht gefunden.", exception.getMessage());
        verifyNoInteractions(fixture.onlineService());
    }

    @Test
    void reportsUnreadableSecretWithoutExposingDetailsAndKeepsCause() throws Exception {
        var fixture = fixture();
        var cause = new IllegalStateException("Private encryption details with 100% content");
        when(fixture.secretService().decrypt(fixture.secret())).thenThrow(cause);

        var exception = assertThrows(
                ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> fixture.node().init(context(validConfig("TEST"), new ProcessExecutionData()))
        );

        assertEquals("Das gespeicherte Client-Secret des Onlinedienstes konnte nicht gelesen werden.", exception.getMessage());
        assertSame(cause, exception.getCause());
        verifyNoInteractions(fixture.onlineService());
    }

    private static Fixture fixture() throws Exception {
        var secretService = mock(SecretService.class);
        var secret = mock(SecretEntity.class);
        when(secretService.retrieve(SECRET_ID)).thenReturn(Optional.of(secret));
        when(secretService.decrypt(secret)).thenReturn("sender-secret");

        var jsonMapper = mock(JsonMapper.class);
        when(jsonMapper.writeValueAsString(any())).thenReturn("{\"name\":\"Ada\"}");

        var onlineService = mock(OnlineService.class);
        var cases = mock(OnlineServiceCases.class);
        var sentSubmission = new SentSubmission(
                DESTINATION_ID,
                SENDER_DESTINATION_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(),
                new PublicService("Test service", "urn:de:service:test"),
                null,
                null,
                null
        );
        when(onlineService.send(any(OutgoingSubmission.class))).thenReturn(sentSubmission);
        when(onlineService.cases()).thenReturn(cases);
        when(cases.logOf(sentSubmission)).thenReturn(transferLog(Event.ACCEPT_SUBMISSION));

        var node = spy(new FitConnectSendJsonActionNodeV1(secretService, jsonMapper));
        doReturn(onlineService).when(node).createOnlineService(
                anyString(), anyString(), any(FitConnectEnvironment.class), any(UUID.class)
        );

        return new Fixture(node, onlineService, cases, sentSubmission, secretService, secret);
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

    private static FitConnectSendJsonActionNodeV1.SendDataFitConnectActionNodeV1Config validConfig(String environment) {
        var config = new FitConnectSendJsonActionNodeV1.SendDataFitConnectActionNodeV1Config();
        config.environment = environment;
        config.serviceIdentifier = "urn:de:service:test";
        config.serviceName = "Test service";
        config.destinationId = DESTINATION_ID.toString();
        config.senderDestinationId = SENDER_DESTINATION_ID.toString();
        config.senderClientId = "sender-client";
        config.senderClientSecret = SECRET_ID.toString();
        config.jsonDatasetProcessKey = "payload";
        config.jsonSchemaLink = "https://schema.example/submission.json";
        return config;
    }

    private static ProcessNodeExecutionInitContext<FitConnectSendJsonActionNodeV1.SendDataFitConnectActionNodeV1Config> context(
            FitConnectSendJsonActionNodeV1.SendDataFitConnectActionNodeV1Config config,
            ProcessExecutionData processData
    ) {
        return new ProcessNodeExecutionInitContext<>(
                mock(ProcessNodeExecutionLogger.class),
                mock(ProcessNodeEntity.class),
                mock(ProcessInstanceEntity.class),
                mock(ProcessInstanceTaskEntity.class),
                null,
                processData,
                config
        );
    }

    private record Fixture(
            FitConnectSendJsonActionNodeV1 node,
            OnlineService onlineService,
            OnlineServiceCases cases,
            SentSubmission sentSubmission,
            SecretService secretService,
            SecretEntity secret
    ) {
    }
}
