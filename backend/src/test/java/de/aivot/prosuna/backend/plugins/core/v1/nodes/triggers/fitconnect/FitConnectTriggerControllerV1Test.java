package de.aivot.prosuna.backend.plugins.core.v1.nodes.triggers.fitconnect;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.repositories.ProcessNodeRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessTestClaimRepository;
import de.aivot.prosuna.backend.process.services.ProcessNodeService;
import de.aivot.prosuna.backend.process.services.ProcessService;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FitConnectTriggerControllerV1Test {
    private static final String PROCESS_SLUG = "example-process";
    private static final String TRIGGER_SLUG = "fit-connect";
    private static final String AUTHENTICATION = "authentication";
    private static final String TIMESTAMP = "1788343200";
    private static final UUID DESTINATION_ID = UUID.fromString("d12caea8-f372-4eb1-b102-b0a228253a11");
    private static final UUID SUBMISSION_ID = UUID.fromString("f39ab143-d91a-474a-b69f-b00f1a1873c2");
    private static final byte[] RAW_BODY = """
            {
              "type": "https://schema.fitko.de/fit-connect/submission-api/callbacks/new-submissions",
              "submissions": [
                {
                  "destinationId": "d12caea8-f372-4eb1-b102-b0a228253a11",
                  "submissionId": "f39ab143-d91a-474a-b69f-b00f1a1873c2",
                  "caseId": "9eec7d3e-dc66-4f82-9f52-1520bf96a32e"
                }
              ]
            }
            """.getBytes(StandardCharsets.UTF_8);

    @Test
    void validAuthenticationImportsSubmissionFromAuthenticatedRawBody() throws Exception {
        var fixture = createFixture(List.of(createNode()));

        fixture.controller().handleCallback(
                PROCESS_SLUG,
                TRIGGER_SLUG,
                RAW_BODY,
                null,
                AUTHENTICATION,
                TIMESTAMP
        );

        verify(fixture.authenticationService()).authenticate(
                eq("secret-key"),
                eq(AUTHENTICATION),
                eq(TIMESTAMP),
                aryEq(RAW_BODY)
        );
        verify(fixture.importService()).importSubmission(
                isNull(),
                eq(fixture.node()),
                eq(fixture.config()),
                argThat(reference -> DESTINATION_ID.equals(reference.destinationId()) &&
                        SUBMISSION_ID.equals(reference.submissionId())),
                any(Instant.class)
        );
    }

    @Test
    void authenticationFailureDoesNotImportSubmission() throws Exception {
        var fixture = createFixture(List.of(createNode()));
        doThrow(ResponseException.unauthorized("invalid"))
                .when(fixture.authenticationService())
                .authenticate(any(), any(), any(), any(byte[].class));

        var exception = assertThrows(ResponseException.class, () -> fixture.controller().handleCallback(
                PROCESS_SLUG,
                TRIGGER_SLUG,
                RAW_BODY,
                null,
                AUTHENTICATION,
                TIMESTAMP
        ));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        verify(fixture.importService(), never()).importSubmission(any(), any(), any(), any(), any());
    }

    @Test
    void ambiguousSlugFailsBeforeAuthentication() throws Exception {
        var fixture = createFixture(List.of(createNode(), createNode().setId(2)));

        var exception = assertThrows(ResponseException.class, () -> fixture.controller().handleCallback(
                PROCESS_SLUG,
                TRIGGER_SLUG,
                RAW_BODY,
                null,
                AUTHENTICATION,
                TIMESTAMP
        ));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        verify(fixture.authenticationService(), never()).authenticate(any(), any(), any(), any());
        verify(fixture.importService(), never()).importSubmission(any(), any(), any(), any(), any());
    }

    @Test
    void authenticatedMalformedJsonReturnsBadRequestWithoutImport() throws Exception {
        var fixture = createFixture(List.of(createNode()));
        var malformedBody = "not-json".getBytes(StandardCharsets.UTF_8);

        var exception = assertThrows(ResponseException.class, () -> fixture.controller().handleCallback(
                PROCESS_SLUG,
                TRIGGER_SLUG,
                malformedBody,
                null,
                AUTHENTICATION,
                TIMESTAMP
        ));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(fixture.authenticationService()).authenticate(
                eq("secret-key"),
                eq(AUTHENTICATION),
                eq(TIMESTAMP),
                aryEq(malformedBody)
        );
        verify(fixture.importService(), never()).importSubmission(any(), any(), any(), any(), any());
    }

    @Test
    void destinationMismatchReturnsBadRequestWithoutImport() throws Exception {
        var fixture = createFixture(List.of(createNode()));
        fixture.config().destinationId = "31494a79-740c-4995-9e66-30b862647fd7";

        var exception = assertThrows(ResponseException.class, () -> fixture.controller().handleCallback(
                PROCESS_SLUG,
                TRIGGER_SLUG,
                RAW_BODY,
                null,
                AUTHENTICATION,
                TIMESTAMP
        ));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(fixture.importService(), never()).importSubmission(any(), any(), any(), any(), any());
    }

    @Test
    void unsupportedCallbackTypeReturnsBadRequestWithoutImport() throws Exception {
        var fixture = createFixture(List.of(createNode()));
        var body = """
                {
                  "type": "https://example.test/unsupported",
                  "submissions": []
                }
                """.getBytes(StandardCharsets.UTF_8);

        var exception = assertThrows(ResponseException.class, () -> fixture.controller().handleCallback(
                PROCESS_SLUG,
                TRIGGER_SLUG,
                body,
                null,
                AUTHENTICATION,
                TIMESTAMP
        ));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(fixture.importService(), never()).importSubmission(any(), any(), any(), any(), any());
    }

    private static ProcessNodeEntity createNode() {
        return new ProcessNodeEntity()
                .setId(1)
                .setProcessId(1)
                .setProcessVersion(1)
                .setProcessNodeDefinitionKey("core.fit_connect_trigger")
                .setProcessNodeDefinitionVersion(1);
    }

    private static Fixture createFixture(List<ProcessNodeEntity> nodes) throws ResponseException {
        var process = new ProcessEntity()
                .setId(1)
                .setSlug(PROCESS_SLUG);

        var processService = mock(ProcessService.class);
        when(processService.retrieveBySlugOrHistory(PROCESS_SLUG)).thenReturn(Optional.of(process));

        var processNodeRepository = mock(ProcessNodeRepository.class);
        when(processNodeRepository.findAll(any(Specification.class))).thenReturn(nodes);

        var definition = mock(FitConnectTriggerNodeV1.class);
        when(definition.getKey()).thenReturn("core.fit_connect_trigger");
        when(definition.getMajorVersion()).thenReturn(1);

        var config = new FitConnectTriggerConfigV1();
        config.callbackSecret = "secret-key";
        config.destinationId = DESTINATION_ID.toString();

        var processNodeService = mock(ProcessNodeService.class);
        if (!nodes.isEmpty()) {
            when(processNodeService.deriveConfiguration(eq(nodes.getFirst()), eq(definition), isNull(), eq(true)))
                    .thenReturn(new ProcessNodeService.ProcessConfigurationDetails<>(
                            config,
                            DerivedRuntimeElementData.empty()
                    ));
        }

        var authenticationService = mock(FitConnectTriggerCallbackAuthenticationServiceV1.class);
        var importService = mock(FitConnectTriggerSubmissionImportServiceV1.class);
        var controller = new FitConnectTriggerControllerV1(
                mock(ProcessTestClaimRepository.class),
                processNodeService,
                processService,
                processNodeRepository,
                definition,
                authenticationService,
                importService,
                JsonMapper.builder().build()
        );

        return new Fixture(
                controller,
                authenticationService,
                importService,
                nodes.isEmpty() ? null : nodes.getFirst(),
                config
        );
    }

    private record Fixture(
            FitConnectTriggerControllerV1 controller,
            FitConnectTriggerCallbackAuthenticationServiceV1 authenticationService,
            FitConnectTriggerSubmissionImportServiceV1 importService,
            ProcessNodeEntity node,
            FitConnectTriggerConfigV1 config
    ) {
    }
}
