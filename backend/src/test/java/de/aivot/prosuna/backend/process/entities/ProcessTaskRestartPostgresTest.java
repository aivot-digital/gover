package de.aivot.prosuna.backend.process.entities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// H2 checks NO ACTION references row by row during cascades. Verify PostgreSQL's statement-level
// behavior against an explicitly supplied database, using only temporary tables and a rolled-back transaction.
@EnabledIfEnvironmentVariable(named = "PROSUNA_TEST_POSTGRES_URL", matches = "jdbc:postgresql:.*")
class ProcessTaskRestartPostgresTest {
    @Test
    void deletingAnInstanceRemovesAllAttemptsAndTheirEvents() throws Exception {
        var properties = new Properties();
        for (var key : new String[]{"user", "password"}) {
            var value = System.getenv("PROSUNA_TEST_POSTGRES_" + key.toUpperCase(java.util.Locale.ROOT));
            if (value != null) properties.setProperty(key, value);
        }
        try (var connection = DriverManager.getConnection(System.getenv("PROSUNA_TEST_POSTGRES_URL"), properties)) {
            connection.setAutoCommit(false);
            try (var statement = connection.createStatement()) {
                statement.execute("set local search_path = pg_temp");
                statement.execute("create temporary table process_instances (id bigint primary key)");
                statement.execute("""
                        create temporary table process_instance_tasks (
                            id bigint primary key, process_instance_id bigint references process_instances(id) on delete cascade
                        )
                        """);
                statement.execute("""
                        create temporary table process_instance_events (
                            id bigint primary key,
                            process_instance_id bigint references process_instances(id) on delete cascade,
                            process_instance_task_id bigint references process_instance_tasks(id) on delete cascade
                        )
                        """);
                statement.execute("insert into process_instances values (12), (99)");
                statement.execute("insert into process_instance_tasks values (1, 12)");
                var migration = new ClassPathResource("db/migration/V31_5_0__process_task_restart_reference.sql");
                statement.execute(migration.getContentAsString(StandardCharsets.UTF_8));
                statement.execute("insert into process_instance_tasks values (2, 12, 1), (3, 12, 2), (4, 99, null)");
                statement.execute("insert into process_instance_events values (1, 12, 1), (2, 12, 2), (3, 12, 3), (4, 99, 4), (5, 12, null)");
                statement.execute("delete from process_instances where id = 12");
                for (var table : new String[]{"process_instance_tasks", "process_instance_events"}) {
                    try (var rows = statement.executeQuery("select count(*), min(process_instance_id) from " + table)) {
                        assertTrue(rows.next());
                        assertEquals(1L, rows.getLong(1));
                        assertEquals(99L, rows.getLong(2));
                    }
                }
            } finally {
                connection.rollback();
            }
        }
    }
}
