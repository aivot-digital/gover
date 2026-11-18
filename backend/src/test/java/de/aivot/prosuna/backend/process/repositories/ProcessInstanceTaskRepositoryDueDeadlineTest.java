package de.aivot.prosuna.backend.process.repositories;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.process.services.ProcessTaskDeadlineService;
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
        "spring.test.database.replace=none",
        "spring.datasource.url=jdbc:h2:mem:due-task-deadlines",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"})
@ContextConfiguration(classes = ProcessInstanceTaskRepositoryDueDeadlineTest.Config.class)
class ProcessInstanceTaskRepositoryDueDeadlineTest {
    private static final Instant NOW = Instant.parse("2030-05-01T12:00:00Z");
    @Autowired
    private EntityManager em;
    private ProcessInstanceTaskRepository tasks;

    @BeforeEach
    void setup() {
        sql("drop table if exists process_instance_tasks");
        sql("drop table if exists process_instances");
        // Only the columns read by the due-deadline query; statuses use their persisted values.
        sql("create table process_instances (id bigint primary key, status smallint)");
        sql("""
                create table process_instance_tasks (
                    id bigint primary key, process_instance_id bigint, status smallint,
                    deadline timestamp with time zone, deadline_key varchar(32),
                    deadline_strict boolean not null default false
                )
                """);
        // Instance 1 is running, instance 2 is completed.
        sql("insert into process_instances values (1, 1), (2, 3)");
        // Task status 7 is AwaitingCustomer, 2 is Completed and 0 is Running.
        sql("""
                insert into process_instance_tasks values
                    (10, 1, 7, '2030-05-01 11:00:00+00', 'customer', true),
                    (11, 1, 7, '2030-05-01 11:00:00+00', 'customer', false),
                    (12, 1, 7, '2030-05-01 13:00:00+00', 'customer', true),
                    (13, 1, 2, '2030-05-01 11:00:00+00', 'customer', true),
                    (14, 2, 7, '2030-05-01 11:00:00+00', 'customer', true),
                    (15, 1, 0, '2030-05-01 12:00:00+00', 'processing', true)
                """);
        tasks = new JpaRepositoryFactory(em).getRepository(ProcessInstanceTaskRepository.class);
    }

    @Test
    void selectsOnlyDueHardDeadlinesOfActiveTasksInRunningInstances() {
        var ids = tasks.findDueDeadlineIds(0L, NOW, ProcessTaskDeadlineService.EXPIRABLE_STATUSES, PageRequest.of(0, 10));

        assertEquals(List.of(10L, 15L), ids);
    }

    @Test
    void continuesAfterTheLastSelectedTask() {
        var ids = tasks.findDueDeadlineIds(10L, NOW, ProcessTaskDeadlineService.EXPIRABLE_STATUSES, PageRequest.of(0, 10));

        assertEquals(List.of(15L), ids);
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
