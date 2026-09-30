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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

class ProcessInstanceEventLogServiceTest {
    @Test
    @SuppressWarnings("unchecked")
    void searchAndNewFiltersRemainRestrictedToTheRequestedInstance() throws Exception {
        var events = mock(ProcessInstanceHistoryEventRepository.class);
        var instances = mock(ProcessInstanceRepository.class);
        var tasks = mock(ProcessInstanceTaskRepository.class);
        var users = mock(UserRepository.class);
        var service = new ProcessInstanceEventLogService(events, instances, tasks,
                mock(ProcessNodeRepository.class), mock(ProcessNodeDefinitionService.class), users);
        when(instances.findById(12L)).thenReturn(Optional.of(new ProcessInstanceEntity()
                .setId(12L).setCaseNumber("V-12").setStarted(Instant.now())));
        when(events.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(users.findIdsByFullNameContaining("robin")).thenReturn(List.of("matching-user"));

        service.getEventLog(12L, null, " Robin ", true, false,
                "user-1", "identity-1", "Antrag", PageRequest.of(0, 50));

        ArgumentCaptor<Specification<ProcessInstanceEventEntity>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(events).findAll(captor.capture(), any(Pageable.class));
        Root<ProcessInstanceEventEntity> root = mock(Root.class, RETURNS_DEEP_STUBS);
        var builder = mock(CriteriaBuilder.class, RETURNS_DEEP_STUBS);
        Predicate instance = mock(), notable = mock(), scope = mock(), search = mock(), scopedSearch = mock(), filters = mock(), result = mock();
        when(builder.equal(root.get("processInstanceId"), 12L)).thenReturn(instance);
        when(root.get("level").in(ProcessNodeExecutionLogLevel.Warn, ProcessNodeExecutionLogLevel.Error)).thenReturn(notable);
        when(builder.and(instance, notable)).thenReturn(scope);
        when(builder.or(any(Predicate[].class))).thenReturn(search);
        when(builder.and(scope, search)).thenReturn(scopedSearch);
        when(builder.and(any(Predicate[].class))).thenReturn(filters);
        when(builder.and(scopedSearch, filters)).thenReturn(result);

        assertSame(result, captor.getValue().toPredicate(root, mock(CriteriaQuery.class), builder));
        verify(builder).and(instance, notable);
        verify(builder).and(scope, search);
        verify(builder).and(scopedSearch, filters);
        verify(builder).equal(root.get("isHistoryRelevant"), false);
        verify(builder).equal(root.get("concernedUserId"), "user-1");
        verify(builder).equal(root.get("concernedIdentityId"), "identity-1");
        verify(root.get("triggeringUserId")).in(List.of("matching-user"));
        verify(root.get("concernedUserId")).in(List.of("matching-user"));
        for (var field : List.of("concernedUserId", "concernedIdentityId", "concernedIdentityTitle")) {
            verify(builder).like(builder.lower(root.get(field)), "%robin%");
        }
        verify(tasks).findAllByProcessInstanceId(12L);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "missing-user")
    @SuppressWarnings("unchecked")
    void missingConcernedUsersKeepTheirReferenceWithoutInventingAName(String concernedUserId) throws Exception {
        var events = mock(ProcessInstanceHistoryEventRepository.class);
        var instances = mock(ProcessInstanceRepository.class);
        var service = new ProcessInstanceEventLogService(events, instances, mock(ProcessInstanceTaskRepository.class),
                mock(ProcessNodeRepository.class), mock(ProcessNodeDefinitionService.class), mock(UserRepository.class));
        when(instances.findById(12L)).thenReturn(Optional.of(new ProcessInstanceEntity()
                .setId(12L).setCaseNumber("V-12").setStarted(Instant.now())));
        var event = new ProcessInstanceEventEntity().setId(1L).setProcessInstanceId(12L)
                .setLevel(ProcessNodeExecutionLogLevel.Info).setTitle("Event").setMessage("Message")
                .setDetails(Map.of()).setTimestamp(Instant.now()).setConcernedUserId(concernedUserId);
        when(events.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(event)));

        var entry = service.getEventLog(12L, null, null, false, null, null, null, null,
                PageRequest.of(0, 50)).events().getContent().getFirst();

        assertEquals(concernedUserId, entry.concernedUserId());
        assertNull(entry.concernedUserName());
        assertNull(entry.concernedIdentityId());
        assertNull(entry.concernedIdentityTitle());
    }

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
                true,
                "Prüfung verzögert",
                "Die Prüfung konnte noch nicht abgeschlossen werden.",
                Map.of("attempt", 2),
                started.plusSeconds(30),
                user.getId(),
                "00000000-0000-0000-0000-000000000002",
                "applicant-identity",
                "Antragstellende Person"
        );
        var concernedUser = new UserEntity()
                .setId(event.getConcernedUserId())
                .setFullName("Robin Beispiel")
                .setEnabled(true)
                .setDeletedInIdp(true);

        when(instanceRepository.findById(12L)).thenReturn(Optional.of(instance));
        when(taskRepository.findById(34L)).thenReturn(Optional.of(task));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 50), 1));
        when(taskRepository.findAllById(any())).thenReturn(List.of(task));
        when(nodeRepository.findAllById(any())).thenReturn(List.of(node));
        when(nodeRepository.findById(56)).thenReturn(Optional.of(node));
        when(nodeDefinitionService.getProcessNodeDefinition(node)).thenReturn(Optional.empty());
        when(userRepository.findAllById(any())).thenReturn(List.of(user, concernedUser));

        var result = service.getEventLog(
                12L,
                34L,
                null,
                false,
                null, null, null, null,
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
        assertTrue(entry.historyRelevant());
        assertEquals(concernedUser.getId(), entry.concernedUserId());
        assertEquals("Robin Beispiel (gelöscht)", entry.concernedUserName());
        assertEquals("applicant-identity", entry.concernedIdentityId());
        assertEquals("Antragstellende Person", entry.concernedIdentityTitle());
        verify(userRepository).findAllById(Set.of(user.getId(), concernedUser.getId()));
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
                () -> service.getEventLog(12L, 34L, null, false, null, null, null, null, PageRequest.of(0, 50))
        );

        assertEquals("Die Aufgabe gehört nicht zum angegebenen Vorgang.", exception.getTitle());
        assertNull(exception.getDetails());
    }
}
