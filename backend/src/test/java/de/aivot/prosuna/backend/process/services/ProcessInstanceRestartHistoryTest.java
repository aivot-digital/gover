package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.process.dtos.ProcessInstanceEventLogDTO;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.repositories.*;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.test.context.ContextConfiguration;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=none",
        "spring.data.jpa.repositories.enabled=false",
        "spring.jpa.properties.hibernate.connection.url=jdbc:h2:mem:restart-history",
        "spring.jpa.properties.hibernate.connection.username=sa", "spring.jpa.properties.hibernate.connection.password=",
        "spring.jpa.properties.hibernate.connection.driver_class=org.h2.Driver"})
@ContextConfiguration(classes = ProcessInstanceRestartHistoryTest.Config.class)
class ProcessInstanceRestartHistoryTest {
    @Autowired
    private EntityManager em;
    @Autowired
    private JsonMapper jsonMapper;
    private ProcessInstanceEventLogService service;

    @BeforeEach
    void setup() throws Exception {
        sql("drop table if exists process_instance_events");
        sql("drop table if exists process_instance_tasks");
        sql("drop table if exists process_instances");
        sql("create table process_instances (id bigint primary key)");
        sql("insert into process_instances values (12), (99)");
        sql("""
                create table process_instance_tasks (
                    id bigint primary key, access_key varchar(128), process_instance_id bigint, process_id int,
                    process_version int, process_node_id int, previous_process_instance_task_id bigint,
                    previous_process_node_id int, previous_process_node_port_key varchar(96), status smallint,
                    status_override varchar(96), execution_summary_markdown text,
                    started timestamp with time zone default current_timestamp, updated timestamp with time zone,
                    finished timestamp with time zone, runtime interval second, runtime_data text default '{}', node_data text default '{}',
                    process_data text default '{}', process_data_diff text default '{}', assigned_user_id varchar(36),
                    assigned_customer_identity_id varchar(36), deadline timestamp with time zone,
                    postponed_until timestamp with time zone, retry_count int, next_retry_at timestamp with time zone,
                    foreign key (process_instance_id) references process_instances(id) on delete cascade
                )
                """);
        sql("insert into process_instance_tasks (id, process_instance_id, process_node_id) values (1, 12, 56)");
        var migration = new ClassPathResource("db/migration/V31_5_0__process_task_restart_reference.sql");
        for (var statement : migration.getContentAsString(StandardCharsets.UTF_8).split(";")) {
            if (!statement.isBlank()) sql(statement);
        }
        sql("""
                insert into process_instance_tasks (id, process_instance_id, process_node_id, restart_for_task_id) values
                    (2, 12, 56, 1), (3, 12, 56, 2), (4, 12, 56, null),
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
        var factory = new JpaRepositoryFactory(em);
        var tasks = factory.getRepository(ProcessInstanceTaskRepository.class);
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
    void migrationKeepsLegacyTasksUnlinkedAndSerializesNewReferences() {
        assertNull(em.find(ProcessInstanceTaskEntity.class, 1L).getRestartForTaskId());
        var task = em.find(ProcessInstanceTaskEntity.class, 3L);
        assertEquals(2L, task.getRestartForTaskId());
        assertEquals(2L, jsonMapper.valueToTree(task).path("restartForTaskId").asLong());
    }

    @Test
    void cannotReferenceSelfOrMissingTask() {
        assertThrows(PersistenceException.class, () -> sql("update process_instance_tasks set restart_for_task_id = 1 where id = 1"));
        assertThrows(PersistenceException.class, () -> sql("update process_instance_tasks set restart_for_task_id = 999 where id = 1"));
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
