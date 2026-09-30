package de.aivot.prosuna.backend.process.models;

import de.aivot.prosuna.backend.process.entities.ProcessInstanceEventEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceHistoryEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ProcessNodeExecutionLoggerTest {
    @Test
    void executionFailureIsRecordedOnceWithTaskContextAndWithoutRawExceptionInHistory() {
        var repository = mock(ProcessInstanceHistoryEventRepository.class);
        var logger = new ProcessNodeExecutionLogger(42L, null, null, null, repository);
        var taskLogger = logger.withTaskId(9L);
        var exception = new ProcessNodeExecutionExceptionUnknown("Internal URL, identity-id and credentials");

        taskLogger.logFailure(exception);
        logger.logFailure(exception);

        var captor = ArgumentCaptor.forClass(ProcessInstanceEventEntity.class);
        verify(repository, times(2)).save(captor.capture());
        var history = captor.getAllValues().stream().filter(ProcessInstanceEventEntity::getHistoryRelevant).toList();
        assertEquals(1, history.size());
        assertEquals(9L, history.getFirst().getProcessInstanceTaskId());
        assertFalse(history.getFirst().getTechnical());
        assertEquals("Die Verarbeitung konnte nicht abgeschlossen werden.", history.getFirst().getMessage());
    }

    @Test
    void saveEvent_PersistsAllFieldsWithIndependentTriggeringAndConcernedIdentities() {
        var repository = mock(ProcessInstanceHistoryEventRepository.class);
        var triggeringUserId = "00000000-0000-0000-0000-000000000001";
        var concernedUserId = "00000000-0000-0000-0000-000000000002";
        var logger = new ProcessNodeExecutionLogger(42L, 9L, triggeringUserId, "triggering-identity", repository);
        var timestamp = Instant.parse("2026-09-30T08:00:00Z");
        var details = Map.<String, Object>of("identityId", "explicit-detail-identity");

        logger.saveEvent(ProcessNodeExecutionLogLevel.Warn, true, false, true,
                "Event title", "Event message", details, timestamp,
                concernedUserId, "concerned-identity", "Concerned identity");

        var eventCaptor = ArgumentCaptor.forClass(ProcessInstanceEventEntity.class);
        verify(repository).save(eventCaptor.capture());
        var event = eventCaptor.getValue();
        assertNull(event.getId());
        assertEquals(42L, event.getProcessInstanceId());
        assertEquals(9L, event.getProcessInstanceTaskId());
        assertEquals(ProcessNodeExecutionLogLevel.Warn, event.getLevel());
        assertTrue(event.getTechnical());
        assertFalse(event.getAudit());
        assertTrue(event.getHistoryRelevant());
        assertEquals("Event title", event.getTitle());
        assertEquals("Event message", event.getMessage());
        assertEquals(details, event.getDetails());
        assertEquals(timestamp, event.getTimestamp());
        assertEquals(triggeringUserId, event.getTriggeringUserId());
        assertEquals(concernedUserId, event.getConcernedUserId());
        assertEquals("concerned-identity", event.getConcernedIdentityId());
        assertEquals("Concerned identity", event.getConcernedIdentityTitle());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void saveEvent_SupportsAttachmentDetailsWithOptionalContext(boolean hasContext) {
        var repository = mock(ProcessInstanceHistoryEventRepository.class);
        Long taskId = hasContext ? 9L : null;
        String userId = hasContext ? "00000000-0000-0000-0000-000000000001" : null;
        var logger = new ProcessNodeExecutionLogger(42L, taskId, userId, "triggering-identity", repository);
        var details = new LinkedHashMap<String, Object>();
        details.put("attachmentKey", UUID.randomUUID());
        details.put("fileName", "file.pdf");
        details.put("originalFileName", "uploaded-file.pdf");
        details.put("group", null);
        details.put("position", 1);
        details.put("attachmentSetId", null);
        details.put("processInstanceId", 42L);
        details.put("processInstanceTaskId", taskId);
        details.put("storageProviderId", 5);
        details.put("storagePathFromRoot", "/attachments/file.pdf");
        details.put("uploadedByUserId", userId);
        var originalDetails = new LinkedHashMap<>(details);

        logger.saveEvent(ProcessNodeExecutionLogLevel.Info, userId == null, true, false,
                "Attachment created", "The attachment file.pdf was created.", details, Instant.now(),
                null, null, null);

        var eventCaptor = ArgumentCaptor.forClass(ProcessInstanceEventEntity.class);
        verify(repository).save(eventCaptor.capture());
        var event = eventCaptor.getValue();
        assertEquals(taskId, event.getProcessInstanceTaskId());
        assertEquals(userId, event.getTriggeringUserId());
        assertEquals(userId == null, event.getTechnical());
        assertTrue(event.getAudit());
        assertFalse(event.getHistoryRelevant());
        assertNull(event.getConcernedUserId());
        assertNull(event.getConcernedIdentityId());
        assertNull(event.getConcernedIdentityTitle());
        assertEquals(originalDetails, details);
        assertEquals(originalDetails, event.getDetails());
        assertEquals(new ArrayList<>(originalDetails.keySet()), new ArrayList<>(event.getDetails().keySet()));
        details.put("fileName", "changed.pdf");
        assertEquals("file.pdf", event.getDetails().get("fileName"));
    }

    @Test
    void saveEvent_DoesNotPropagatePersistenceFailures() {
        var repository = mock(ProcessInstanceHistoryEventRepository.class);
        var logger = new ProcessNodeExecutionLogger(42L, null, null, null, repository);
        doThrow(new IllegalStateException("Persistence unavailable"))
                .when(repository).save(any(ProcessInstanceEventEntity.class));

        assertDoesNotThrow(() -> logger.saveEvent(ProcessNodeExecutionLogLevel.Info, true, true, false,
                "Attachment created", "The attachment file.pdf was created.", Map.of(), Instant.now(),
                null, null, null));

        verify(repository).save(any(ProcessInstanceEventEntity.class));
    }

    @Test
    void logf_PersistsCustomDetailsWithoutMutatingInput() {
        var repository = mock(ProcessInstanceHistoryEventRepository.class);
        var logger = new ProcessNodeExecutionLogger(
                42L,
                9L,
                null,
                "triggering-identity",
                repository
        );
        var details = new LinkedHashMap<String, Object>();
        details.put("sendResult", Map.of("submissionId", "submission-1"));
        var before = Instant.now();

        logger.logf(
                ProcessNodeExecutionLogLevel.Info,
                false,
                true,
                "Nachricht versendet",
                details,
                "Nachricht an %s versendet.",
                "applicant"
        );
        var after = Instant.now();

        var eventCaptor = ArgumentCaptor.forClass(ProcessInstanceEventEntity.class);
        verify(repository).save(eventCaptor.capture());
        var event = eventCaptor.getValue();

        assertEquals("Nachricht an applicant versendet.", event.getMessage());
        assertEquals(Map.of(
                "sendResult", Map.of("submissionId", "submission-1"),
                "identityId", "triggering-identity"
        ), event.getDetails());
        assertFalse(details.containsKey("identityId"));
        assertFalse(event.getHistoryRelevant());
        assertNull(event.getConcernedUserId());
        assertNull(event.getConcernedIdentityId());
        assertNull(event.getConcernedIdentityTitle());
        assertFalse(event.getTimestamp().isBefore(before));
        assertFalse(event.getTimestamp().isAfter(after));
    }

    @Test
    void logException_PersistsProcessExceptionOnlyOnceAcrossOverloads() {
        var repository = mock(ProcessInstanceHistoryEventRepository.class);
        var logger = new ProcessNodeExecutionLogger(
                42L,
                9L,
                null,
                null,
                repository
        );
        var exception = new ProcessNodeExecutionExceptionUnknown("execution failed");

        logger.logException(exception);
        logger.logException(exception);
        logger.logException((Exception) exception);

        verify(repository, times(1)).save(org.mockito.ArgumentMatchers.any(ProcessInstanceEventEntity.class));
        assertTrue(exception.isAlreadyLogged());
    }

    @Test
    void logException_DoesNotPersistAlreadyLoggedProcessException() {
        var repository = mock(ProcessInstanceHistoryEventRepository.class);
        var logger = new ProcessNodeExecutionLogger(
                42L,
                9L,
                null,
                null,
                repository
        );
        var exception = new ProcessNodeExecutionExceptionUnknown("execution failed")
                .setAlreadyLogged(true);

        logger.logException(exception);

        verifyNoInteractions(repository);
    }
}
