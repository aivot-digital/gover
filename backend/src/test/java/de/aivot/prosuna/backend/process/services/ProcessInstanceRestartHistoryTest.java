package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.dtos.ProcessInstanceEventLogDTO;
import de.aivot.prosuna.backend.process.enums.ProcessHistoryEventScope;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.repositories.*;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.test.context.ContextConfiguration;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=none",
        "spring.data.jpa.repositories.enabled=false",
        "spring.test.database.replace=none",
        "spring.datasource.url=jdbc:h2:mem:restart-history",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"})
@ContextConfiguration(classes = ProcessInstanceRestartHistoryTest.Config.class)
public class ProcessInstanceRestartHistoryTest {
    @Autowired
    private EntityManager em;
    @Autowired
    private JsonMapper jsonMapper;
    private ProcessInstanceEventLogService service;
    private ProcessInstanceHistoryService history;

    @BeforeEach
    void setup() {
        sql("create alias if not exists jsonb_extract_path_text for \"de.aivot.prosuna.backend.process.services.ProcessInstanceRestartHistoryTest.jsonbExtractPathText\"");
        sql("drop table if exists process_instance_events");
        sql("drop table if exists process_instance_tasks");
        sql("drop table if exists process_instances");
        sql("create table process_instances (id bigint primary key)");
        sql("insert into process_instances values (12), (99)");
        sql("""
                create table process_instance_tasks (
                    id bigint primary key, access_key varchar(128), process_instance_id bigint, process_id int,
                    process_version int, process_node_id int, previous_process_instance_task_id bigint,
                    restart_for_task_id bigint, previous_process_node_id int, previous_process_node_port_key varchar(96), status smallint,
                    status_override varchar(96), execution_summary_markdown text,
                    started timestamp with time zone default current_timestamp, updated timestamp with time zone,
                    finished timestamp with time zone, runtime interval second, runtime_data text default '{}', node_data text default '{}',
                    process_data text default '{}', process_data_diff text default '{}', assigned_user_id varchar(36),
                    assigned_customer_identity_id varchar(36), deadline timestamp with time zone, deadline_key varchar(32),
                    deadline_strict boolean not null default false,
                    postponed_until timestamp with time zone, retry_count int, next_retry_at timestamp with time zone,
                    foreign key (process_instance_id) references process_instances(id) on delete cascade,
                    constraint process_instance_tasks_restart_for_task_fk
                        foreign key (restart_for_task_id) references process_instance_tasks(id),
                    constraint process_instance_tasks_restart_not_self check (restart_for_task_id <> id)
                )
                """);
        sql("""
                insert into process_instance_tasks (id, process_instance_id, process_node_id, restart_for_task_id) values
                    (1, 12, 56, null), (2, 12, 56, 1), (3, 12, 56, 2), (4, 12, 56, null),
                    (5, 99, 56, null), (6, 12, 57, null), (7, 12, 56, 3)
                """);
        sql("""
                create table process_instance_events (
                    id bigint primary key, process_instance_id bigint, process_instance_task_id bigint,
                    level smallint, is_technical boolean default false, is_audit boolean default false,
                    is_history_relevant boolean default true, title varchar(96), message varchar(4096),
                    details text default '{}', timestamp timestamp with time zone,
                    triggering_user_id varchar(36), concerned_user_id varchar(36),
                    concerned_identity_id varchar(255), concerned_identity_title varchar(255),
                    foreign key (process_instance_task_id) references process_instance_tasks(id) on delete cascade
                )
                """);
        sql("""
                insert into process_instance_events (id, process_instance_id, process_instance_task_id, level,
                    title, message, timestamp) values
                    (11, 12, 1, 2, 'Fehler', 'Früherer Versuch', '2026-08-14 08:00:00+00'),
                    (12, 12, 2, 2, 'Fehler', 'Zweiter Versuch', '2026-08-14 08:00:00+00'),
                    (13, 12, 3, 1, 'Gestartet', 'Aktueller Versuch', '2026-08-14 08:00:00+00'),
                    (14, 12, 4, 2, 'Fehler', 'Eigener Schleifendurchlauf', '2026-08-14 08:00:00+00'),
                    (15, 99, 5, 2, 'Fehler', 'Anderer Vorgang', '2026-08-14 08:00:00+00'),
                    (16, 12, 6, 2, 'Fehler', 'Anderer Knoten', '2026-08-14 08:00:00+00'),
                    (17, 12, 7, 2, 'Fehler', 'Späterer Versuch', '2026-08-14 08:00:00+00'),
                    (18, 12, null, 1, 'Gestartet', 'Vorgangsereignis', '2026-08-14 08:00:00+00')
                """);
        sql("update process_instance_tasks set status = 2, process_version = 1, started = '2026-08-14 08:00:00+00'");
        var factory = new JpaRepositoryFactory(em);
        var tasks = factory.getRepository(ProcessInstanceTaskRepository.class);
        history = new ProcessInstanceHistoryService(tasks, mock(PermissionService.class));
        var events = factory.getRepository(ProcessInstanceHistoryEventRepository.class);
        var instances = mock(ProcessInstanceRepository.class);
        when(instances.findById(12L)).thenReturn(Optional.of(new ProcessInstanceEntity()
                .setId(12L).setCaseNumber("V-12").setStarted(Instant.parse("2026-08-14T08:00:00Z"))));
        var nodes = mock(ProcessNodeRepository.class);
        var node = new ProcessNodeEntity().setId(56).setName("Prüfung");
        when(nodes.findById(56)).thenReturn(Optional.of(node));
        when(nodes.findAllById(any())).thenReturn(List.of(node));
        service = new ProcessInstanceEventLogService(events, instances, tasks, nodes,
                mock(ProcessNodeDefinitionService.class), mock(UserRepository.class));
    }

    @Test
    void persistsAndSerializesRestartReferences() {
        assertNull(em.find(ProcessInstanceTaskEntity.class, 1L).getRestartForTaskId());
        var task = em.find(ProcessInstanceTaskEntity.class, 3L);
        assertEquals(2L, task.getRestartForTaskId());
        assertEquals(2L, jsonMapper.valueToTree(task).path("restartForTaskId").asLong());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ASC", "DESC"})
    void paginatesTheWholeChainWithStableOrderingAndExcludesOtherRuns(String direction) throws Exception {
        var first = log(3L, true, null, false, null, PageRequest.of(0, 2, Sort.by(Sort.Direction.valueOf(direction), "timestamp")));
        var second = log(3L, true, null, false, null, PageRequest.of(1, 2, Sort.by(Sort.Direction.valueOf(direction), "timestamp")));
        assertEquals(3, first.events().getTotalElements());
        assertEquals(2, first.events().getTotalPages());
        var ids = java.util.stream.Stream.concat(first.events().stream(), second.events().stream())
                .map(ProcessInstanceEventLogDTO.Entry::id).toList();
        assertEquals(direction.equals("ASC") ? List.of(11L, 12L, 13L) : List.of(13L, 12L, 11L), ids);
        assertEquals(3L, first.task().id());
        assertEquals(2L, first.task().restartForTaskId());
        var entries = java.util.stream.Stream.concat(first.events().stream(), second.events().stream()).toList();
        assertEquals(2L, entries.stream().filter(entry -> entry.id() == 13).findFirst().orElseThrow().restartForTaskId());
        assertNull(entries.stream().filter(entry -> entry.id() == 11).findFirst().orElseThrow().restartForTaskId());
    }

    @Test
    void filtersTheChainBeforeCountingAndPaging() throws Exception {
        var filtered = log(3L, true, " Versuch ", true, true, PageRequest.of(0, 1));
        assertEquals(2, filtered.events().getTotalElements());
        assertEquals(List.of(12L), filtered.events().map(ProcessInstanceEventLogDTO.Entry::id).getContent());
        var second = log(3L, true, " Versuch ", true, true, PageRequest.of(1, 1));
        assertEquals(List.of(11L), second.events().map(ProcessInstanceEventLogDTO.Entry::id).getContent());
        assertTrue(log(3L, true, "Schleifendurchlauf", false, null, PageRequest.of(0, 50)).events().isEmpty());
        assertTrue(log(3L, true, null, false, false, PageRequest.of(0, 50)).events().isEmpty());
    }

    @Test
    void exactTaskAndInstanceLogsKeepTheirScope() throws Exception {
        assertEquals(List.of(13L), log(3L, false, null, false, null, PageRequest.of(0, 50))
                .events().map(ProcessInstanceEventLogDTO.Entry::id).getContent());
        assertEquals(List.of(14L), log(4L, true, null, false, null, PageRequest.of(0, 50))
                .events().map(ProcessInstanceEventLogDTO.Entry::id).getContent());
        for (boolean include : List.of(false, true)) {
            assertEquals(7, log(null, include, null, false, null, PageRequest.of(0, 50)).events().getTotalElements());
        }
    }

    @Test
    void cyclesTerminateWithoutDuplicateEvents() throws Exception {
        sql("update process_instance_tasks set restart_for_task_id = 3 where id = 1");
        assertEquals(3, log(3L, true, null, false, null, PageRequest.of(0, 50)).events().getTotalElements());
    }

    @ParameterizedTest
    @ValueSource(longs = {5, 6})
    void invalidCrossScopeLinksDoNotIncludeOtherInstancesOrNodes(long outsideTaskId) throws Exception {
        sql("update process_instance_tasks set restart_for_task_id = " + outsideTaskId + " where id = 1");
        assertEquals(3, log(3L, true, null, false, null, PageRequest.of(0, 50)).events().getTotalElements());
    }

    public static String jsonbExtractPathText(String json, String key) {
        return json == null ? null : new org.json.JSONObject(json).optString(key, null);
    }

    @Test
    void taskHistoryKeepsOnlyManualRestartEventsAndPagesEachScopeIndependently() throws Exception {
        sql("update process_instance_events set is_audit = true, triggering_user_id = 'actor', details = '{\"manualIntervention\":true}', is_history_relevant = false where id in (11,12,14)");
        sql("update process_instance_events set level = 3 where id = 18");
        var page = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "timestamp"));
        var taskEvents = service.getHistoryEvents(12L, ProcessHistoryEventScope.Task, 3L, page);
        assertEquals(3, taskEvents.getTotalElements());
        assertEquals(List.of(13L, 12L), taskEvents.map(ProcessInstanceEventLogDTO.Entry::id).getContent());
        assertEquals(List.of(11L), service.getHistoryEvents(12L, ProcessHistoryEventScope.Task, 3L, page.next())
                .map(ProcessInstanceEventLogDTO.Entry::id).getContent());
        // An unmarked execution error from a replaced attempt must disappear from the task overview only.
        sql("update process_instance_events set details = '{}', is_history_relevant = true, level = 3 where id = 11");
        assertEquals(List.of(13L, 12L), service.getHistoryEvents(12L, ProcessHistoryEventScope.Task, 3L, page)
                .map(ProcessInstanceEventLogDTO.Entry::id).getContent());
        assertEquals(List.of(14L, 12L), service.getHistoryEvents(12L, ProcessHistoryEventScope.ManualInterventions, null, page)
                .map(ProcessInstanceEventLogDTO.Entry::id).getContent());
        assertEquals(List.of(18L), service.getHistoryEvents(12L, ProcessHistoryEventScope.InstanceErrors, null, page)
                .map(ProcessInstanceEventLogDTO.Entry::id).getContent());
        assertThrows(ResponseException.class, () -> service.getHistoryEvents(12L, ProcessHistoryEventScope.Task, 5L, page));
        assertThrows(ResponseException.class, () -> service.getHistoryEvents(12L, ProcessHistoryEventScope.Task, null, page));
    }

    @Test
    void keepsRecentEventsAccessibleBeyondTheFormer999Limit() throws Exception {
        sql("delete from process_instance_events");
        sql("""
                insert into process_instance_events (id, process_instance_id, process_instance_task_id,
                    level, title, message, timestamp)
                select x, 12, 3, 1, 'Event', 'History entry', '2026-08-14 08:00:00+00'
                from system_range(100, 1299)
                """);
        sql("update process_instance_events set process_instance_task_id = null, level = 3 where id = 1298");
        sql("""
                update process_instance_events set process_instance_task_id = null, is_audit = true,
                    triggering_user_id = 'actor', details = '{"manualIntervention":true}' where id = 1299
                """);

        var newest = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "timestamp"));
        var taskEvents = service.getHistoryEvents(12L, ProcessHistoryEventScope.Task, 3L, newest);
        assertEquals(1198, taskEvents.getTotalElements());
        assertEquals(20, taskEvents.getNumberOfElements());
        assertEquals(1297L, taskEvents.getContent().getFirst().id());
        var oldest = service.getHistoryEvents(12L, ProcessHistoryEventScope.Task, 3L, newest.withPage(59));
        assertEquals(18, oldest.getNumberOfElements());
        assertEquals(100L, oldest.getContent().getLast().id());

        var chronological = PageRequest.of(0, 20, Sort.by("timestamp"));
        assertEquals(List.of(1299L), service.getHistoryEvents(12L, ProcessHistoryEventScope.ManualInterventions, null, chronological)
                .map(ProcessInstanceEventLogDTO.Entry::id).getContent());
        assertEquals(List.of(1298L), service.getHistoryEvents(12L, ProcessHistoryEventScope.InstanceErrors, null, chronological)
                .map(ProcessInstanceEventLogDTO.Entry::id).getContent());
    }

    @Test
    void taskPagesExcludeReplacedAttemptsAndKeepOffPageContext() throws Exception {
        sql("update process_instance_tasks set status = 5 where id in (1,2)");
        sql("update process_instance_tasks set status = 0, previous_process_instance_task_id = 3 where id = 7");
        var first = history.getTasks("actor", 12L, PageRequest.of(0, 2), null);
        var second = history.getTasks("actor", 12L, PageRequest.of(1, 2), null);
        assertEquals(List.of(3L, 4L), first.tasks().map(ProcessInstanceTaskEntity::getId).getContent());
        assertEquals(List.of(6L, 7L), second.tasks().map(ProcessInstanceTaskEntity::getId).getContent());
        assertEquals(4, first.tasks().getTotalElements());
        assertEquals(List.of(3L), second.predecessors().stream().map(ProcessInstanceTaskEntity::getId).toList());
        assertEquals(1, first.activeTasks().size());
        assertEquals(56, first.activeTasks().getFirst().processNodeId());
        assertEquals(first.activeTasks(), second.activeTasks());
        assertEquals(1, history.getTasks("actor", 12L, PageRequest.of(99, 2), null).tasks().getNumber());
        assertEquals(100, history.getTasks("actor", 12L, PageRequest.of(0, 999), null).tasks().getSize());
    }

    @ParameterizedTest
    @ValueSource(strings = {"7", "active", "current", "failed", "latest"})
    void locatesTargetsBeforeLoadingTheirPage(String target) throws Exception {
        sql("update process_instance_tasks set status = 5 where id in (1,2)");
        sql("update process_instance_tasks set status = " + (target.equals("failed") ? 4 : 0) + " where id = 7");
        var result = history.getTasks("actor", 12L, PageRequest.of(0, 2), target);
        assertEquals(1, result.tasks().getNumber());
        assertEquals(List.of(6L, 7L), result.tasks().map(ProcessInstanceTaskEntity::getId).getContent());
    }

    @Test
    void currentFallsBackToLatestAndInvalidTargetsDoNotEscapeTheirInstance() throws Exception {
        assertEquals(2, history.getTasks("actor", 12L, PageRequest.of(0, 2), "current").tasks().getNumber());
        for (var target : List.of("5", "missing", "9007199254740993", "0")) {
            assertEquals(0, history.getTasks("actor", 12L, PageRequest.of(0, 2), target).tasks().getNumber());
        }
    }

    @Test
    void reachesTasksBeyondTheFormer999Limit() throws Exception {
        sql("insert into process_instance_tasks (id, process_instance_id, process_node_id, process_version, status) select x, 12, 56, 1, 2 from system_range(100, 1104)");
        var result = history.getTasks("actor", 12L, PageRequest.of(0, 20), "latest");
        assertEquals(1011, result.tasks().getTotalElements());
        assertEquals(50, result.tasks().getNumber());
        assertEquals(1104L, result.tasks().getContent().getLast().getId());
    }

    private ProcessInstanceEventLogDTO log(Long taskId, boolean include, String search, boolean notable,
                                           Boolean historyRelevant, PageRequest pageable) throws Exception {
        return service.getEventLog(12L, taskId, include, search, notable, historyRelevant, null, null, null, pageable);
    }

    private void sql(String statement) {
        em.createNativeQuery(statement).executeUpdate();
    }

    @Configuration(proxyBeanMethods = false)
    @EntityScan("de.aivot.prosuna.backend")
    static class Config {
        @Bean
        JsonMapper jsonMapper() {
            return JsonMapperTestUtils.createMapper();
        }
    }
}
