package de.aivot.prosuna.backend.process.models;

import de.aivot.prosuna.backend.process.entities.ProcessInstanceEventEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceHistoryEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
}
