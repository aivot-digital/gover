package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceHistoryEventRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.test.context.ContextConfiguration;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

/**
 * Runs the counter without a target path several times against a real task repository, saving each task
 * as the worker does: running before init, then completed with the node data of the result.
 */
@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=none",
        "spring.data.jpa.repositories.enabled=false",
        "spring.test.database.replace=none",
        "spring.datasource.url=jdbc:h2:mem:counter-node-passes",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"})
@ContextConfiguration(classes = CounterActionNodeV1RepositoryTest.Config.class)
class CounterActionNodeV1RepositoryTest {
    private static final long PROCESS_INSTANCE_ID = 17L;
    private static final int NODE_ID = 3;

    @Autowired
    private EntityManager entityManager;
    private ProcessInstanceTaskRepository tasks;
    private CounterActionNodeV1 node;
    private Instant clock;

    @BeforeEach
    void setup() {
        sql("drop table if exists process_instance_tasks");
        sql("create sequence if not exists process_instance_tasks_id_seq increment by 1");
        // H2 stand-in for the table, as in ProcessInstanceTaskSummaryTest: text replaces jsonb for converted JSON.
        sql("""
                create table process_instance_tasks (
                    id bigint primary key, access_key varchar(128), process_instance_id bigint, process_id int,
                    process_version int, process_node_id int, previous_process_instance_task_id bigint, restart_for_task_id bigint,
                    previous_process_node_id int, previous_process_node_port_key varchar(96), status smallint,
                    status_override varchar(96), started timestamp with time zone, updated timestamp with time zone,
                    finished timestamp with time zone, runtime interval second, runtime_data text, node_data text,
                    process_data text, process_data_diff text, execution_summary_markdown text, assigned_user_id varchar(36),
                    assigned_customer_identity_id varchar(36), deadline timestamp with time zone, deadline_key varchar(32),
                    deadline_strict boolean not null default false,
                    postponed_until timestamp with time zone, retry_count int, next_retry_at timestamp with time zone, assignment_pool text
                )
                """);
        tasks = new JpaRepositoryFactory(entityManager).getRepository(ProcessInstanceTaskRepository.class);
        node = new CounterActionNodeV1(tasks);
        clock = Instant.parse("2030-05-01T12:00:00Z");
    }

    @Test
    void continuesFromThePreviousCompletedPassAndSkipsFailedAttempts() throws Exception {
        assertEquals(1L, runPass(ProcessTaskStatus.Completed));
        assertEquals(2L, runPass(ProcessTaskStatus.Completed));
        // A failed attempt that was restarted keeps no node data and must not reset the counter.
        failedAttempt();
        assertEquals(3L, runPass(ProcessTaskStatus.Completed));
    }

    private long runPass(ProcessTaskStatus finalStatus) throws Exception {
        var task = startTask();
        var result = assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class, node.init(new ProcessNodeExecutionInitContext<>(
                new ProcessNodeExecutionLogger(PROCESS_INSTANCE_ID, task.getId(), null, null, mock(ProcessInstanceHistoryEventRepository.class)),
                new ProcessNodeEntity().setId(NODE_ID),
                new ProcessInstanceEntity().setId(PROCESS_INSTANCE_ID),
                task,
                null,
                new ProcessExecutionData(),
                new CounterActionNodeV1.CounterActionNodeV1Configuration()
        )));
        tasks.saveAndFlush(task.setStatus(finalStatus).setNodeData(result.getNodeData()));
        entityManager.clear();
        return ((Number) result.getNodeData().get("value")).longValue();
    }

    private void failedAttempt() {
        tasks.saveAndFlush(startTask().setStatus(ProcessTaskStatus.Restarted));
        entityManager.clear();
    }

    private ProcessInstanceTaskEntity startTask() {
        clock = clock.plusSeconds(60);
        return tasks.saveAndFlush(new ProcessInstanceTaskEntity()
                .setAccessKey("task-" + clock.getEpochSecond())
                .setProcessInstanceId(PROCESS_INSTANCE_ID)
                .setProcessId(1)
                .setProcessVersion(1)
                .setProcessNodeId(NODE_ID)
                .setStatus(ProcessTaskStatus.Running)
                .setStarted(clock)
                .setUpdated(clock)
                .setRuntimeData(new HashMap<>())
                .setNodeData(new HashMap<>())
                .setProcessData(new HashMap<>())
                .setProcessDataDiff(Map.of()));
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
