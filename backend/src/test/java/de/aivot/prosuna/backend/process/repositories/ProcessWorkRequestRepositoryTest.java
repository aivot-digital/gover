package de.aivot.prosuna.backend.process.repositories;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.process.entities.ProcessWorkRequestEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.enums.ProcessWorkRequestKind;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.test.context.ContextConfiguration;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=none",
        "spring.data.jpa.repositories.enabled=false",
        "spring.jpa.properties.hibernate.connection.url=jdbc:h2:mem:process-work-requests",
        "spring.jpa.properties.hibernate.connection.username=sa", "spring.jpa.properties.hibernate.connection.password=",
        "spring.jpa.properties.hibernate.connection.driver_class=org.h2.Driver"})
@ContextConfiguration(classes = ProcessWorkRequestRepositoryTest.Config.class)
class ProcessWorkRequestRepositoryTest {
    private static final Instant NOW = Instant.parse("2030-05-01T12:00:00Z");
    private static final Instant DISPATCHED_BEFORE = Instant.parse("2030-05-01T11:55:00Z");
    private static final List<ProcessInstanceStatus> DISPATCHABLE = List.of(ProcessInstanceStatus.Running,
            ProcessInstanceStatus.Completed, ProcessInstanceStatus.Aborted);
    private static final List<ProcessTaskStatus> HELD = List.of(ProcessTaskStatus.Paused);
    @Autowired
    private EntityManager em;
    private ProcessWorkRequestRepository requests;

    @BeforeEach
    void setup() {
        sql("drop table if exists process_work_requests");
        sql("drop table if exists process_instances");
        sql("drop table if exists process_instance_tasks");
        sql("drop sequence if exists process_work_requests_id_seq");
        // Only the columns read by the dispatch query; instance statuses use their persisted ordinals.
        sql("create table process_instances (id bigint primary key, status smallint)");
        sql("create table process_instance_tasks (id bigint primary key, status smallint)");
        sql("""
                create table process_work_requests (
                    id bigint primary key, kind smallint, process_instance_id bigint, task_id bigint,
                    via_port varchar(96), to_node_id int, created timestamp with time zone,
                    last_dispatched timestamp with time zone
                )
                """);
        sql("create sequence process_work_requests_id_seq start with 100");
        // Created, Running, Paused, Completed, Aborted and Failed instances.
        sql("insert into process_instances values (6, 0), (1, 1), (2, 2), (4, 3), (5, 4), (3, 5)");
        // Task statuses: 1 Paused, 2 Completed, 5 Restarted, 6 AwaitingPayment.
        sql("insert into process_instance_tasks values (100, 2), (101, 5), (102, 6), (104, 2), (105, 2), (106, 2), (108, 1)");
        sql("""
                insert into process_work_requests values
                    (10, 1, 1, 100, 'next', 2, '2030-05-01 11:00:00+00', null),
                    (11, 2, 1, 101, null, null, '2030-05-01 11:00:00+00', '2030-05-01 11:59:00+00'),
                    (12, 3, 1, 102, null, null, '2030-05-01 11:00:00+00', '2030-05-01 11:55:00+00'),
                    (13, 0, 2, null, null, null, '2030-05-01 11:00:00+00', null),
                    (14, 1, 3, 104, 'next', 2, '2030-05-01 11:00:00+00', null),
                    (15, 1, 4, 105, 'next', 2, '2030-05-01 11:00:00+00', null),
                    (16, 1, 5, 106, 'next', 2, '2030-05-01 11:00:00+00', null),
                    (17, 0, 6, null, null, null, '2030-05-01 11:00:00+00', null),
                    (18, 3, 1, 108, null, null, '2030-05-01 11:00:00+00', null)
                """);
        requests = new JpaRepositoryFactory(em).getRepository(ProcessWorkRequestRepository.class);
    }

    @Test
    void selectsUnpublishedAndStaleRequestsOnlyForInstancesAndTasksThatActOnThem() {
        assertEquals(List.of(10L, 12L, 15L, 16L),
                requests.findDispatchableIds(0L, DISPATCHED_BEFORE, DISPATCHABLE, HELD, PageRequest.of(0, 10)));
        assertEquals(List.of(15L, 16L),
                requests.findDispatchableIds(12L, DISPATCHED_BEFORE, DISPATCHABLE, HELD, PageRequest.of(0, 10)));
    }

    @Test
    void releasedRequestIsDueAgain() {
        assertEquals(1, requests.claimDispatch(10L, DISPATCHED_BEFORE, NOW));
        assertEquals(List.of(12L, 15L, 16L),
                requests.findDispatchableIds(0L, DISPATCHED_BEFORE, DISPATCHABLE, HELD, PageRequest.of(0, 10)));

        assertEquals(1, requests.releaseDispatch(10L));

        assertEquals(List.of(10L, 12L, 15L, 16L),
                requests.findDispatchableIds(0L, DISPATCHED_BEFORE, DISPATCHABLE, HELD, PageRequest.of(0, 10)));
    }

    @Test
    void onlyOneDispatcherClaimsAPendingRequest() {
        assertEquals(1, requests.claimDispatch(10L, DISPATCHED_BEFORE, NOW));
        assertEquals(0, requests.claimDispatch(10L, DISPATCHED_BEFORE, NOW));
        assertEquals(0, requests.claimDispatch(11L, DISPATCHED_BEFORE, NOW));
        assertEquals(0, requests.claimDispatch(99L, DISPATCHED_BEFORE, NOW));

        assertEquals(List.of(12L, 15L, 16L),
                requests.findDispatchableIds(0L, DISPATCHED_BEFORE, DISPATCHABLE, HELD, PageRequest.of(0, 10)));
        assertEquals(9L, ((Number) em.createNativeQuery("select count(*) from process_work_requests")
                .getSingleResult()).longValue());
    }

    @Test
    void findsPendingRequestsByKind() {
        assertTrue(requests.existsByKindAndTaskId(ProcessWorkRequestKind.Transition, 100L));
        assertFalse(requests.existsByKindAndTaskId(ProcessWorkRequestKind.Restart, 100L));
        assertTrue(requests.existsByKindAndProcessInstanceId(ProcessWorkRequestKind.Start, 2L));
        assertFalse(requests.existsByKindAndProcessInstanceId(ProcessWorkRequestKind.Start, 1L));
        assertEquals(List.of(10L, 11L, 12L, 18L), requests.findIdsByProcessInstanceId(1L));
    }

    @Test
    void persistsKindsWithTheirExplicitDatabaseValues() {
        var saved = requests.saveAndFlush(ProcessWorkRequestEntity.resume(1L, 107L, NOW));

        assertEquals(100L, saved.getId());
        assertEquals(NOW, saved.getLastDispatched());
        assertEquals(3, ((Number) em.createNativeQuery("select kind from process_work_requests where id = 100")
                .getSingleResult()).intValue());
        em.clear();
        assertEquals(ProcessWorkRequestKind.Restart, requests.findById(11L).orElseThrow().getKind());
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
