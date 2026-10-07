package de.aivot.prosuna.backend.process.entities;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.process.controllers.CustomerProcessInstanceViewController;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=none",
        "spring.data.jpa.repositories.enabled=false",
        "spring.jpa.properties.hibernate.connection.url=jdbc:h2:mem:task-summaries",
        "spring.jpa.properties.hibernate.connection.username=sa", "spring.jpa.properties.hibernate.connection.password=",
        "spring.jpa.properties.hibernate.connection.driver_class=org.h2.Driver"})
@ContextConfiguration(classes = ProcessInstanceTaskSummaryTest.Config.class)
class ProcessInstanceTaskSummaryTest {
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JsonMapper jsonMapper;

    @BeforeEach
    void setup() throws Exception {
        sql("drop table if exists process_instance_tasks");
        sql("create sequence if not exists process_instance_tasks_id_seq increment by 1");
        // The pre-migration table uses text for converted JSON and an explicit H2 interval qualifier.
        sql("""
                create table process_instance_tasks (
                    id bigint primary key, access_key varchar(128), process_instance_id bigint, process_id int,
                    process_version int, process_node_id int, previous_process_instance_task_id bigint, restart_for_task_id bigint,
                    previous_process_node_id int, previous_process_node_port_key varchar(96), status smallint,
                    status_override varchar(96), started timestamp with time zone, updated timestamp with time zone,
                    finished timestamp with time zone, runtime interval second, runtime_data text, node_data text,
                    process_data text, process_data_diff text, assigned_user_id varchar(36),
                    assigned_customer_identity_id varchar(36), deadline timestamp with time zone,
                    postponed_until timestamp with time zone, retry_count int, next_retry_at timestamp with time zone
                )
                """);
        sql("insert into process_instance_tasks (id) values (9999)");
        var migration = new ClassPathResource("db/migration/V31_4_0__process_task_execution_summary.sql");
        sql(migration.getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    void migrationPreservesLegacyTasksWithoutInventingSummaries() {
        var summary = entityManager.createNativeQuery(
                "select execution_summary_markdown from process_instance_tasks where id = 9999", String.class)
                .getSingleResultOrNull();
        assertNull(summary);
        assertEquals(1L, ((Number) entityManager.createNativeQuery(
                "select count(*) from process_instance_tasks where id = 9999").getSingleResult()).longValue());
    }

    @Test
    void persistsAndSerializesLongMarkdownVerbatimWithoutExposingItToCustomers() {
        var summary = "    eingerückter Code\n\n**Ergebnis:** Prüfung abgeschlossen.  \n".repeat(100);
        var now = Instant.now();
        var task = new ProcessInstanceTaskEntity().setAccessKey("task-key").setProcessInstanceId(17L)
                .setProcessId(1).setProcessVersion(1).setProcessNodeId(2).setStatus(ProcessTaskStatus.Completed)
                .setStarted(now).setUpdated(now).setFinished(now).setRuntimeData(Map.of()).setNodeData(Map.of())
                .setProcessData(Map.of()).setProcessDataDiff(Map.of()).setExecutionSummaryMarkdown(summary);
        entityManager.persist(task);
        entityManager.flush();
        entityManager.clear();

        var loaded = entityManager.find(ProcessInstanceTaskEntity.class, task.getId());
        assertEquals(summary, loaded.getExecutionSummaryMarkdown());
        var internalJson = jsonMapper.valueToTree(loaded);
        assertEquals(summary, internalJson.path("executionSummaryMarkdown").asString());
        var customerJson = jsonMapper.valueToTree(CustomerProcessInstanceViewController.ProcessInstanceTaskStatusResponse.of(loaded));
        assertFalse(customerJson.has("executionSummaryMarkdown"));
        assertFalse(customerJson.toString().contains("Prüfung abgeschlossen"));
    }

    private void sql(String statement) {
        entityManager.createNativeQuery(statement).executeUpdate();
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
