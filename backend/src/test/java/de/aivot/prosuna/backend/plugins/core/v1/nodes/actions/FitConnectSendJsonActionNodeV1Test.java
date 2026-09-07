package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.secrets.entities.SecretEntity;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import dev.fitko.fitconnect.core.http.api.RestApiException;
import dev.fitko.fitconnect.rest.client.config.FitConnectEnvironment;
import dev.fitko.fitconnect.rest.model.event.Event;
import dev.fitko.fitconnect.rest.model.submission.PublicService;
import dev.fitko.fitconnect.rest.model.submission.SentSubmission;
import dev.fitko.fitconnect.sdk.api.OutgoingSubmission;
import dev.fitko.fitconnect.sdk.api.event.CaseEvent;
import dev.fitko.fitconnect.sdk.api.event.TransferLog;
import dev.fitko.fitconnect.sdk.clients.OnlineService;
import dev.fitko.fitconnect.sdk.clients.OnlineServiceCases;
import dev.fitko.fitconnect.sdk.exceptions.FitConnectSenderException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class FitConnectSendJsonActionNodeV1Test {
    private static final UUID DESTINATION_ID = UUID.fromString("22cc92dc-6145-4d29-ab18-4e8b4baaf3a8");
    private static final UUID SENDER_DESTINATION_ID = UUID.fromString("6396c078-0ab0-4aa6-ac78-386fd8e21dd7");
    private static final UUID SECRET_ID = UUID.fromString("114b3dcc-a8e4-43cc-a7d7-017bfe13b432");

    @Test
    void configurationLayoutContainsRequiredEnvironmentSelectionWithoutDefault() throws Exception {
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
        assertEquals(
                List.of(
                        SelectInputElementOption.of("TEST", "TEST"),
                        SelectInputElementOption.of("STAGE", "STAGE"),
                        SelectInputElementOption.of("PROD", "PROD")
                ),
                environment.getOptions()
        );
    }

    @Test
    void configurationValidationAcceptsOnlySupportedEnvironmentsAndValidDestinationIds() {
        var node = new FitConnectSendJsonActionNodeV1(mock(SecretService.class), mock(JsonMapper.class));
        var processNode = mock(ProcessNodeEntity.class);

        for (var value : List.of("TEST", "STAGE", "PROD", " stage ")) {
            assertNull(node.validateConfiguration(processNode, validConfig(value)));
        }

        for (var value : new String[]{null, "", "STAGING", "unknown"}) {
            var errors = node.validateConfiguration(processNode, validConfig(value));
            assertEquals(
                    List.of("Wählen Sie eine unterstützte FIT-Connect-Umgebung aus."),
                    errors.get(FitConnectSendJsonActionNodeV1.SendDataFitConnectActionNodeV1Config.ENVIRONMENT_FIELD_ID)
            );
        }

        var config = validConfig("TEST");
        config.destinationId = "invalid";
        config.senderDestinationId = "invalid";
        var errors = node.validateConfiguration(processNode, config);
        assertTrue(errors.containsKey(FitConnectSendJsonActionNodeV1.SendDataFitConnectActionNodeV1Config.DESTINATION_ID_FIELD_ID));
        assertTrue(errors.containsKey(FitConnectSendJsonActionNodeV1.SendDataFitConnectActionNodeV1Config.SENDER_DESTINATION_ID_FIELD_ID));
    }

    @Test
    void usesConfiguredEnvironmentAndCompletesViaSuccessPort() throws Exception {
        var fixture = fixture();
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
    void mapsMissingDestinationToInvalidConfiguration() throws Exception {
        var fixture = fixture();
        when(fixture.onlineService().send(any(OutgoingSubmission.class))).thenThrow(
                new FitConnectSenderException(
                        "Could not get destination for id " + DESTINATION_ID,
                        new RestApiException("Destination does not exist.", 404)
                )
        );

        var exception = assertThrows(
                ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> fixture.node().init(context(validConfig("STAGE"), new ProcessExecutionData()))
        );

        assertTrue(exception.getMessage().contains(DESTINATION_ID.toString()));
        assertTrue(exception.getMessage().contains("STAGE"));
    }

    @Test
    void mapsGeneralSendingFailureToUnknownExecutionError() throws Exception {
        var fixture = fixture();
        when(fixture.onlineService().send(any(OutgoingSubmission.class)))
                .thenThrow(new FitConnectSenderException("Authentifizierung fehlgeschlagen"));

        var exception = assertThrows(
                ProcessNodeExecutionExceptionUnknown.class,
                () -> fixture.node().init(context(validConfig("PROD"), new ProcessExecutionData()))
        );

        assertTrue(exception.getMessage().contains("Authentifizierung fehlgeschlagen"));
        assertTrue(exception.getMessage().contains("PROD"));
    }

    @Test
    void rejectsMissingEnvironmentDuringExecution() throws Exception {
        var fixture = fixture();

        var exception = assertThrows(
                ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> fixture.node().init(context(validConfig(null), new ProcessExecutionData()))
        );

        assertTrue(exception.getMessage().contains("TEST, STAGE oder PROD"));
    }

    @Test
    void mapsClientInitializationFailureToUnknownExecutionError() throws Exception {
        var fixture = fixture();
        doThrow(new IllegalStateException("SDK-Konfiguration ungültig"))
                .when(fixture.node())
                .createOnlineService(anyString(), anyString(), any(FitConnectEnvironment.class), any(UUID.class));

        var exception = assertThrows(
                ProcessNodeExecutionExceptionUnknown.class,
                () -> fixture.node().init(context(validConfig("STAGE"), new ProcessExecutionData()))
        );

        assertTrue(exception.getMessage().contains("SDK-Konfiguration ungültig"));
        assertTrue(exception.getMessage().contains("STAGE"));
    }

    @Test
    void mapsStatusRetrievalFailureToUnknownExecutionError() throws Exception {
        var fixture = fixture();
        when(fixture.cases().logOf(fixture.sentSubmission()))
                .thenThrow(new FitConnectSenderException("Statusdienst nicht erreichbar"));

        var exception = assertThrows(
                ProcessNodeExecutionExceptionUnknown.class,
                () -> fixture.node().init(context(validConfig("TEST"), new ProcessExecutionData()))
        );

        assertTrue(exception.getMessage().contains("Statusdienst nicht erreichbar"));
        assertTrue(exception.getMessage().contains("TEST"));
    }

    @Test
    void rejectsNonSuccessfulSubmissionStatusWithDetails() throws Exception {
        var fixture = fixture();
        when(fixture.cases().logOf(fixture.sentSubmission()))
                .thenReturn(transferLog(Event.REJECT_SUBMISSION));

        var exception = assertThrows(
                ProcessNodeExecutionExceptionUnknown.class,
                () -> fixture.node().init(context(validConfig("TEST"), new ProcessExecutionData()))
        );

        assertTrue(exception.getMessage().contains("REJECTED"));
        assertTrue(exception.getMessage().contains("Probleme"));
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

        return new Fixture(node, onlineService, cases, sentSubmission);
    }

    private static TransferLog transferLog(Event event) {
        return new TransferLog(List.of(
                CaseEvent.builder()
                        .event(event)
                        .issueTime(new Date())
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
            SentSubmission sentSubmission
    ) {
    }
}
