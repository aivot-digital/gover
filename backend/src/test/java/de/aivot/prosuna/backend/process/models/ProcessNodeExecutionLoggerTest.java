package de.aivot.prosuna.backend.process.models;

import de.aivot.prosuna.backend.process.entities.ProcessInstanceEventEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceHistoryEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ProcessNodeExecutionLoggerTest {
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

        logger.logf(
                ProcessNodeExecutionLogLevel.Info,
                false,
                true,
                "Nachricht versendet",
                details,
                "Nachricht an %s versendet.",
                "applicant"
        );

        var eventCaptor = ArgumentCaptor.forClass(ProcessInstanceEventEntity.class);
        verify(repository).save(eventCaptor.capture());
        var event = eventCaptor.getValue();

        assertEquals("Nachricht an applicant versendet.", event.getMessage());
        assertEquals(Map.of(
                "sendResult", Map.of("submissionId", "submission-1"),
                "identityId", "triggering-identity"
        ), event.getDetails());
        assertFalse(details.containsKey("identityId"));
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
