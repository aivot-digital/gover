package de.aivot.prosuna.backend.process.repositories;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.process.entities.ProcessTaskDeadlineContinuationEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=none",
        "spring.data.jpa.repositories.enabled=false",
        "spring.jpa.properties.hibernate.connection.url=jdbc:h2:mem:deadline-continuations",
        "spring.jpa.properties.hibernate.connection.username=sa", "spring.jpa.properties.hibernate.connection.password=",
        "spring.jpa.properties.hibernate.connection.driver_class=org.h2.Driver"})
@ContextConfiguration(classes = ProcessTaskDeadlineContinuationRepositoryTest.Config.class)
class ProcessTaskDeadlineContinuationRepositoryTest {
    private static final Instant NOW = Instant.parse("2030-05-01T12:00:00Z");
    private static final Instant DISPATCHED_BEFORE = Instant.parse("2030-05-01T11:55:00Z");
    private static final List<ProcessInstanceStatus> DISPATCHABLE = List.of(ProcessInstanceStatus.Running,
            ProcessInstanceStatus.Completed, ProcessInstanceStatus.Aborted);
    @Autowired
    private EntityManager em;
    private ProcessTaskDeadlineContinuationRepository continuations;

    @BeforeEach
    void setup() {
        sql("drop table if exists process_task_deadline_continuations");
        sql("drop table if exists process_instances");
        // Only the columns read by the dispatch query; instance statuses use their persisted ordinals.
        sql("create table process_instances (id bigint primary key, status smallint)");
        sql("""
                create table process_task_deadline_continuations (
                    id bigint primary key, process_instance_id bigint, task_id bigint, from_node_id int,
                    to_node_id int, via_port varchar(96), created timestamp with time zone,
                    last_dispatched timestamp with time zone
                )
                """);
        // Created, Running, Paused, Completed, Aborted and Failed instances.
        sql("insert into process_instances values (6, 0), (1, 1), (2, 2), (4, 3), (5, 4), (3, 5)");
        sql("""
                insert into process_task_deadline_continuations values
                    (10, 1, 100, 1, 2, 'overdue', '2030-05-01 11:00:00+00', null),
                    (11, 1, 101, 1, 2, 'overdue', '2030-05-01 11:00:00+00', '2030-05-01 11:59:00+00'),
                    (12, 1, 102, 1, 2, 'overdue', '2030-05-01 11:00:00+00', '2030-05-01 11:55:00+00'),
                    (13, 2, 103, 1, 2, 'overdue', '2030-05-01 11:00:00+00', null),
                    (14, 3, 104, 1, 2, 'overdue', '2030-05-01 11:00:00+00', null),
                    (15, 4, 105, 1, 2, 'overdue', '2030-05-01 11:00:00+00', null),
                    (16, 5, 106, 1, 2, 'overdue', '2030-05-01 11:00:00+00', null),
                    (17, 6, 107, 1, 2, 'overdue', '2030-05-01 11:00:00+00', null)
                """);
        continuations = new JpaRepositoryFactory(em).getRepository(ProcessTaskDeadlineContinuationRepository.class);
    }

    @Test
    void selectsUnpublishedAndStaleEntriesOnlyForInstancesThatActOnThem() {
        var ids = continuations.findDispatchable(0L, DISPATCHED_BEFORE, DISPATCHABLE, PageRequest.of(0, 10)).stream()
                .map(ProcessTaskDeadlineContinuationEntity::getId).toList();

        assertEquals(List.of(10L, 12L, 15L, 16L), ids);
    }

    @Test
    void recordsThePublicationAndIgnoresAlreadyConsumedEntries() {
        continuations.markDispatched(10L, NOW);
        continuations.markDispatched(99L, NOW);

        var ids = continuations.findDispatchable(0L, DISPATCHED_BEFORE, DISPATCHABLE, PageRequest.of(0, 10)).stream()
                .map(ProcessTaskDeadlineContinuationEntity::getId).toList();
        assertEquals(List.of(12L, 15L, 16L), ids);
        assertEquals(8L, ((Number) em.createNativeQuery("select count(*) from process_task_deadline_continuations")
                .getSingleResult()).longValue());
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
