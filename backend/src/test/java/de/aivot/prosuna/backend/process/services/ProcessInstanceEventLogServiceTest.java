package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEventEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceHistoryEventRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessNodeRepository;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessInstanceEventLogServiceTest {
    @Test
    void getEventLog_ResolvesDisplayDataInBatches() throws ResponseException {
        var eventRepository = mock(ProcessInstanceHistoryEventRepository.class);
        var instanceRepository = mock(ProcessInstanceRepository.class);
        var taskRepository = mock(ProcessInstanceTaskRepository.class);
        var nodeRepository = mock(ProcessNodeRepository.class);
        var nodeDefinitionService = mock(ProcessNodeDefinitionService.class);
        var userRepository = mock(UserRepository.class);
        var service = new ProcessInstanceEventLogService(
                eventRepository,
                instanceRepository,
                taskRepository,
                nodeRepository,
                nodeDefinitionService,
                userRepository
        );

        var started = Instant.parse("2026-08-14T08:00:00Z");
        var instance = new ProcessInstanceEntity()
                .setId(12L)
                .setCaseNumber("V-2026-0012")
                .setStarted(started)
                .setFinished(started.plusSeconds(120))
                .setRuntime(Duration.ofMinutes(2));
        var task = new ProcessInstanceTaskEntity()
                .setId(34L)
                .setProcessInstanceId(12L)
                .setProcessNodeId(56)
                .setStarted(started.plusSeconds(10))
                .setFinished(started.plusSeconds(70))
                .setRuntime(Duration.ofMinutes(1));
        var node = new ProcessNodeEntity()
                .setId(56)
                .setName("Antrag prüfen")
                .setProcessNodeDefinitionKey("test")
                .setProcessNodeDefinitionVersion(1);
        var user = new UserEntity()
                .setId("00000000-0000-0000-0000-000000000001")
                .setFullName("Alex Beispiel")
                .setEnabled(false)
                .setDeletedInIdp(false);
        var event = new ProcessInstanceEventEntity(
                78L,
                12L,
                34L,
                ProcessNodeExecutionLogLevel.Warn,
                true,
                true,
                "Prüfung verzögert",
                "Die Prüfung konnte noch nicht abgeschlossen werden.",
                Map.of("attempt", 2),
                started.plusSeconds(30),
                user.getId()
        );

        when(instanceRepository.findById(12L)).thenReturn(Optional.of(instance));
        when(taskRepository.findById(34L)).thenReturn(Optional.of(task));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 50), 1));
        when(taskRepository.findAllById(any())).thenReturn(List.of(task));
        when(nodeRepository.findAllById(any())).thenReturn(List.of(node));
        when(nodeRepository.findById(56)).thenReturn(Optional.of(node));
        when(nodeDefinitionService.getProcessNodeDefinition(node)).thenReturn(Optional.empty());
        when(userRepository.findAllById(any())).thenReturn(List.of(user));

        var result = service.getEventLog(
                12L,
                34L,
                null,
                false,
                PageRequest.of(0, 500, Sort.by(Sort.Direction.ASC, "timestamp"))
        );

        assertEquals("V-2026-0012", result.instance().caseNumber());
        assertEquals(started, result.instance().started());
        assertEquals(started.plusSeconds(120), result.instance().finished());
        assertEquals(120_000L, result.instance().runtime());
        assertEquals("Antrag prüfen", result.task().name());
        assertEquals(started.plusSeconds(10), result.task().started());
        assertEquals(started.plusSeconds(70), result.task().finished());
        assertEquals(60_000L, result.task().runtime());
        assertEquals(1, result.events().getTotalElements());
        var entry = result.events().getContent().getFirst();
        assertEquals(78L, entry.id());
        assertEquals(12L, entry.processInstanceId());
        assertEquals(34L, entry.processInstanceTaskId());
        assertEquals(ProcessNodeExecutionLogLevel.Warn, entry.level());
        assertTrue(entry.technical());
        assertTrue(entry.audit());
        assertEquals("Prüfung verzögert", entry.title());
        assertEquals("Die Prüfung konnte noch nicht abgeschlossen werden.", entry.message());
        assertEquals(Map.of("attempt", 2), entry.details());
        assertEquals(started.plusSeconds(30), entry.timestamp());
        assertEquals(user.getId(), entry.triggeringUserId());
        assertEquals("Alex Beispiel (inaktiv)", entry.triggeringUserName());
        assertEquals("Antrag prüfen", entry.processNodeName());

        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(eventRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertEquals(100, pageableCaptor.getValue().getPageSize());
        assertEquals(Sort.Direction.ASC, pageableCaptor.getValue().getSort().getOrderFor("timestamp").getDirection());
    }

    @Test
    void getEventLog_RejectsTaskFromAnotherProcessInstance() {
        var eventRepository = mock(ProcessInstanceHistoryEventRepository.class);
        var instanceRepository = mock(ProcessInstanceRepository.class);
        var taskRepository = mock(ProcessInstanceTaskRepository.class);
        var service = new ProcessInstanceEventLogService(
                eventRepository,
                instanceRepository,
                taskRepository,
                mock(ProcessNodeRepository.class),
                mock(ProcessNodeDefinitionService.class),
                mock(UserRepository.class)
        );

        var instance = new ProcessInstanceEntity()
                .setId(12L)
                .setCaseNumber("V-2026-0012")
                .setStarted(Instant.parse("2026-08-14T08:00:00Z"));
        var task = new ProcessInstanceTaskEntity()
                .setId(34L)
                .setProcessInstanceId(99L);
        when(instanceRepository.findById(12L)).thenReturn(Optional.of(instance));
        when(taskRepository.findById(34L)).thenReturn(Optional.of(task));

        var exception = assertThrows(
                ResponseException.class,
                () -> service.getEventLog(12L, 34L, null, false, PageRequest.of(0, 50))
        );

        assertEquals("Die Aufgabe gehört nicht zum angegebenen Vorgang.", exception.getTitle());
        assertNull(exception.getDetails());
    }
}
