package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.*;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.models.*;
import de.aivot.prosuna.backend.process.repositories.*;
import de.aivot.prosuna.backend.process.workers.ProcessTaskDeadlineResultHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real transactions and row locks; the repository adapters keep this test independent of the full application schema. */
class ProcessTaskExecutionServiceTest {
    private static final Instant DEADLINE = Instant.parse("2030-05-01T12:00:00Z");
    private final AtomicReference<Instant> now = new AtomicReference<>(DEADLINE);
    private final ProcessInstanceRepository instances = mock(ProcessInstanceRepository.class);
    private final ProcessInstanceTaskRepository tasks = mock(ProcessInstanceTaskRepository.class);
    private final ProcessTaskDeadlineContinuationRepository continuations = mock(ProcessTaskDeadlineContinuationRepository.class);
    private final ProcessNodeExecutionLogger logger = mock(ProcessNodeExecutionLogger.class);
    private JdbcTemplate jdbc;
    private ProcessTaskExecutionService execution;

    @BeforeEach
    void setup() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000", "sa", "") {
            @Override
            protected Connection getConnectionFromDriver(String username, String password) throws SQLException {
                // Hibernate opens application connections with REPEATABLE READ (see application.yml), which
                // PostgreSQL implements as snapshot isolation. H2 provides these semantics as SNAPSHOT.
                var connection = super.getConnectionFromDriver(username, password);
                try (var statement = connection.createStatement()) {
                    statement.execute("SET SESSION CHARACTERISTICS AS TRANSACTION ISOLATION LEVEL SNAPSHOT");
                }
                return connection;
            }
        };
        jdbc = new JdbcTemplate(source);
        jdbc.execute("create table instances (id bigint primary key)");
        jdbc.execute("create table tasks (id bigint primary key, status varchar(40))");
        jdbc.execute("create table continuations (task_id bigint unique)");
        jdbc.update("insert into instances values (1)");
        jdbc.update("insert into tasks values (2, 'AwaitingCustomer')");
        when(instances.lockAccessById(1L)).thenAnswer(call -> Optional.ofNullable(
                jdbc.queryForObject("select id from instances where id = 1 for update", Long.class)));
        when(instances.findById(1L)).thenAnswer(call -> Optional.of(new ProcessInstanceEntity().setId(1L).setStatus(ProcessInstanceStatus.Running)));
        when(tasks.findById(2L)).thenAnswer(call -> Optional.of(new ProcessInstanceTaskEntity().setId(2L).setProcessInstanceId(1L)
                .setProcessNodeId(3).setDeadline(DEADLINE).setDeadlineKey("customer").setDeadlineStrict(true).setNodeData(Map.of())
                .setStatus(ProcessTaskStatus.valueOf(jdbc.queryForObject("select status from tasks where id = 2", String.class)))));
        when(tasks.save(any())).thenAnswer(call -> {
            ProcessInstanceTaskEntity task = call.getArgument(0);
            jdbc.update("update tasks set status = ? where id = 2", task.getStatus().name());
            return task;
        });
        when(continuations.save(any())).thenAnswer(call -> {
            ProcessTaskDeadlineContinuationEntity continuation = call.getArgument(0);
            jdbc.update("insert into continuations values (?)", continuation.getTaskId());
            return continuation;
        });
        var nodes = mock(ProcessNodeRepository.class);
        var nodeService = mock(ProcessNodeService.class);
        var edges = mock(ProcessEdgeRepository.class);
        var loggers = mock(ProcessNodeExecutionLoggerFactory.class);
        var clock = mock(Clock.class);
        var time = new BusinessTime(ZoneOffset.UTC, clock);
        when(clock.instant()).thenAnswer(call -> now.get());
        var node = new ProcessNodeEntity().setId(3).setDeadlines(Map.of("customer", new ProcessNodeDeadlineConfiguration(2, true)));
        when(nodes.findById(3)).thenReturn(Optional.of(node));
        when(nodes.findById(4)).thenReturn(Optional.of(new ProcessNodeEntity().setId(4)));
        var port = new ProcessNodePort("overdue", "Fristüberschreitung", "Fortsetzung", "customer");
        when(nodeService.resolvePorts(node)).thenReturn(new ProcessNodePortResolution(List.of(port), List.of(),
                List.of(new ProcessNodeDeadlineDefinition("customer", "Kundenbearbeitung", "Zeit für die Antwort", port))));
        when(edges.findByFromNodeIdAndViaPort(3, "overdue")).thenReturn(Optional.of(new ProcessEdgeEntity().setToNodeId(4).setViaPort("overdue")));
        when(loggers.create(1L, 2L, null, null)).thenReturn(logger);
        var deadlines = new ProcessTaskDeadlineService(nodes, nodeService, edges, instances, tasks, loggers,
                new ProcessTaskDeadlineResultHandler(tasks, continuations), time);
        execution = new ProcessTaskExecutionService(instances, tasks, deadlines, new DataSourceTransactionManager(source), mock(jakarta.persistence.EntityManager.class));
    }

    @Test
    void rejectsLateActionAfterCommittingExpiryAndContinuation() {
        var calls = new AtomicInteger();
        var exception = assertThrows(ResponseException.class, () -> execution.execute(1L, 2L, () -> {}, calls::incrementAndGet));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals(Map.of("reason", "task_deadline_exceeded"), exception.getDetails());
        assertEquals(0, calls.get());
        assertEquals("DeadlineExceeded", jdbc.queryForObject("select status from tasks where id = 2", String.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from continuations", Integer.class));
    }

    @Test
    void failedContinuationWriteRollsBackTaskExpiry() {
        doThrow(new IllegalStateException("database failure")).when(continuations).save(any());
        assertThrows(IllegalStateException.class, () -> execution.execute(1L, 2L, () -> {}, () -> null));
        assertEquals("AwaitingCustomer", jdbc.queryForObject("select status from tasks where id = 2", String.class));
        assertEquals(0, jdbc.queryForObject("select count(*) from continuations", Integer.class));
        verifyNoInteractions(logger);
    }

    @Test
    void unauthorizedRequestCannotExpireTask() {
        var exception = assertThrows(ResponseException.class, () -> execution.execute(1L, 2L,
                () -> { throw ResponseException.forbidden(); }, () -> null));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("AwaitingCustomer", jdbc.queryForObject("select status from tasks where id = 2", String.class));
        verifyNoInteractions(continuations, logger);
    }

    @Test
    void concurrentLateActionAndScannerProduceOnlyOneTransition() throws Exception {
        var calls = new AtomicInteger();
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var request = pool.submit(() -> {
                start.await();
                return assertThrows(ResponseException.class, () -> execution.execute(1L, 2L, () -> {}, calls::incrementAndGet));
            });
            var scanner = pool.submit(() -> {
                start.await();
                execution.executeBackground(1L, 2L, () -> { calls.incrementAndGet(); return null; });
                return null;
            });
            start.countDown();
            assertEquals(HttpStatus.CONFLICT, request.get(10, TimeUnit.SECONDS).getStatus());
            scanner.get(10, TimeUnit.SECONDS);
        }
        assertEquals(0, calls.get());
        assertEquals(1, jdbc.queryForObject("select count(*) from continuations", Integer.class));
        verify(continuations).save(any());
    }

    @Test
    void actionAdmittedBeforeDeadlineCanFinishWhileScannerWaitsForLock() throws Exception {
        now.set(DEADLINE.minusNanos(1));
        var admitted = new CountDownLatch(1);
        var finish = new CountDownLatch(1);
        var scannerStarted = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var request = pool.submit(() -> execution.execute(1L, 2L, () -> {}, () -> {
                admitted.countDown();
                try { assertTrue(finish.await(5, TimeUnit.SECONDS)); }
                catch (InterruptedException exception) { throw new IllegalStateException(exception); }
                jdbc.update("update tasks set status = 'Completed' where id = 2");
                return null;
            }));
            assertTrue(admitted.await(5, TimeUnit.SECONDS));
            now.set(DEADLINE.plusSeconds(1));
            var scanner = pool.submit(() -> {
                scannerStarted.countDown();
                execution.executeBackground(1L, 2L, () -> fail("Completed task must not execute again"));
                return null;
            });
            assertTrue(scannerStarted.await(5, TimeUnit.SECONDS));
            finish.countDown();
            request.get(10, TimeUnit.SECONDS);
            scanner.get(10, TimeUnit.SECONDS);
        } finally {
            finish.countDown();
        }
        assertEquals("Completed", jdbc.queryForObject("select status from tasks where id = 2", String.class));
        verifyNoInteractions(continuations, logger);
    }

    @Test
    void operationReadsWorkCommittedWhileItWaitedForTheLock() throws Exception {
        var locked = new CountDownLatch(1);
        var finish = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            // Like a completed task that queues the transition to the next element before its request commits.
            var request = pool.submit(() -> execution.inInstance(1L, () -> {
                locked.countDown();
                try { assertTrue(finish.await(5, TimeUnit.SECONDS)); }
                catch (InterruptedException exception) { throw new IllegalStateException(exception); }
                jdbc.update("update tasks set status = 'Completed' where id = 2");
                return null;
            }));
            assertTrue(locked.await(5, TimeUnit.SECONDS));
            var worker = pool.submit(() -> execution.inInstance(1L,
                    () -> jdbc.queryForObject("select status from tasks where id = 2", String.class)));
            awaitBlockedSession();
            finish.countDown();
            request.get(10, TimeUnit.SECONDS);
            assertEquals("Completed", worker.get(10, TimeUnit.SECONDS));
        } finally {
            finish.countDown();
        }
    }

    private void awaitBlockedSession() throws InterruptedException {
        var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (jdbc.queryForObject("select count(*) from information_schema.sessions where blocker_id is not null",
                Integer.class) == 0) {
            assertTrue(System.nanoTime() < deadline, "The second operation did not wait for the instance lock.");
            Thread.sleep(10);
        }
    }
}
