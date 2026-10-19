package de.aivot.prosuna.backend.codeLists.repositories;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=none",
        "spring.data.jpa.repositories.enabled=false",
        "spring.jpa.properties.hibernate.connection.url=jdbc:h2:mem:code-list-sync;NON_KEYWORDS=KEY,VALUE",
        "spring.jpa.properties.hibernate.connection.username=sa", "spring.jpa.properties.hibernate.connection.password=",
        "spring.jpa.properties.hibernate.connection.driver_class=org.h2.Driver"})
@ContextConfiguration(classes = CodeListRepositoryTest.Config.class)
class CodeListRepositoryTest {
    private static final Instant NOW = Instant.parse("2030-05-01T12:00:00Z");
    private static final Instant BEFORE = Instant.parse("2030-05-01T11:55:00Z");
    private static final Instant STALE_BEFORE = Instant.parse("2030-05-01T11:00:00Z");
    private static final UUID RUN = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_RUN = UUID.fromString("00000000-0000-0000-0000-000000000002");
    @Autowired
    private EntityManager em;
    private CodeListRepository codeLists;

    @BeforeEach
    void setup() {
        sql("drop table if exists code_lists");
        // Only the columns written by the synchronization queries; enums use their persisted ordinals.
        sql("""
                create table code_lists (
                    key varchar(255) primary key, source_type smallint, status smallint, status_message varchar(255),
                    sync_keep_outdated boolean, sync_run_id uuid, sync_started timestamp with time zone,
                    last_sync timestamp with time zone, updated timestamp with time zone
                )
                """);
        // Source types: 0 Manual, 2 XRepository, 3 Asset. Statuses: 0 SyncPending, 1 Syncing, 2 Synced, 3 SyncFailed.
        sql("""
                insert into code_lists values
                    ('manual', 0, 2, null, false, null, null, null, '2030-05-01 10:00:00+00'),
                    ('pending', 2, 0, null, false, null, null, null, '2030-05-01 10:00:00+00'),
                    ('recent', 3, 0, null, false, null, null, null, '2030-05-01 11:59:00+00'),
                    ('syncing', 2, 1, null, false, '%s', '2030-05-01 11:30:00+00', null, '2030-05-01 11:30:00+00'),
                    ('requested-while-syncing', 2, 0, null, true, '%s', '2030-05-01 11:30:00+00', null, '2030-05-01 10:00:00+00'),
                    ('stale', 2, 1, null, false, '%s', '2030-05-01 10:00:00+00', null, '2030-05-01 10:00:00+00'),
                    ('interrupted-before-runs', 3, 1, null, false, null, null, null, '2030-05-01 11:59:00+00'),
                    ('synced', 3, 2, 'old', false, null, null, null, '2030-05-01 10:00:00+00')
                """.formatted(RUN, RUN, OTHER_RUN));
        codeLists = new JpaRepositoryFactory(em).getRepository(CodeListRepository.class);
    }

    @Test
    void recordsRequestsOnlyForListsWithASynchronizedSource() {
        assertEquals(1, codeLists.requestSync("synced", true, NOW));
        assertEquals(0, codeLists.requestSync("manual", true, NOW));

        assertEquals(Arrays.asList(0, true, null), row("synced"));
        assertEquals(Arrays.asList(2, false, null), row("manual"));
    }

    @Test
    void onlyOneRunStartsARequestedSynchronization() {
        var newRun = UUID.randomUUID();
        assertEquals(1, codeLists.claimSync("pending", newRun, NOW, STALE_BEFORE));
        assertEquals(0, codeLists.claimSync("pending", UUID.randomUUID(), NOW, STALE_BEFORE));
        assertEquals(0, codeLists.claimSync("synced", UUID.randomUUID(), NOW, STALE_BEFORE));

        assertEquals(Arrays.asList(1, false, null), row("pending"));
        assertEquals(newRun, runId("pending"));
    }

    @Test
    void aRequestDuringARunWaitsUntilTheRunReleasesIt() {
        assertEquals(0, codeLists.claimSync("requested-while-syncing", UUID.randomUUID(), NOW, STALE_BEFORE));
        assertEquals(0, codeLists.claimSync("syncing", UUID.randomUUID(), NOW, STALE_BEFORE));

        assertEquals(1, codeLists.releaseSync("requested-while-syncing", RUN));

        assertEquals(1, codeLists.claimSync("requested-while-syncing", UUID.randomUUID(), NOW, STALE_BEFORE));
    }

    @Test
    void interruptedRunsAreTakenOver() {
        assertEquals(List.of("interrupted-before-runs", "stale"), codeLists.findInterruptedSyncKeys(STALE_BEFORE));

        var newRun = UUID.randomUUID();
        assertEquals(1, codeLists.claimSync("stale", newRun, NOW, STALE_BEFORE));
        assertEquals(1, codeLists.claimSync("interrupted-before-runs", UUID.randomUUID(), NOW, STALE_BEFORE));

        assertEquals(newRun, runId("stale"));
        assertEquals(List.of(), codeLists.findInterruptedSyncKeys(STALE_BEFORE));
    }

    @Test
    void onlyTheRunThatOwnsTheSynchronizationRecordsAFailure() {
        assertEquals(0, codeLists.failSync("syncing", OTHER_RUN, "Fehler", NOW));
        assertEquals(0, codeLists.failSync("requested-while-syncing", RUN, "Fehler", NOW));
        assertEquals(1, codeLists.failSync("syncing", RUN, "Fehler", NOW));

        assertEquals(Arrays.asList(3, false, "Fehler"), row("syncing"));
        assertNull(runId("syncing"));
        assertEquals(Arrays.asList(0, true, null), row("requested-while-syncing"));
    }

    @Test
    void releasingIgnoresRunsThatNoLongerOwnTheSynchronization() {
        assertEquals(0, codeLists.releaseSync("stale", RUN));

        assertEquals(OTHER_RUN, runId("stale"));
    }

    @Test
    void pendingRequestsArePublishedAgainByOneCaller() {
        assertEquals(List.of("pending", "requested-while-syncing"), codeLists.findPendingSyncKeys(BEFORE));
        assertEquals(1, codeLists.claimSyncRedispatch("pending", BEFORE, NOW));
        assertEquals(0, codeLists.claimSyncRedispatch("pending", BEFORE, NOW));
        assertEquals(0, codeLists.claimSyncRedispatch("recent", BEFORE, NOW));
        assertEquals(List.of("requested-while-syncing"), codeLists.findPendingSyncKeys(BEFORE));
    }

    private List<Object> row(String key) {
        var row = (Object[]) em.createNativeQuery("select status, sync_keep_outdated, status_message from code_lists where key = ?1")
                .setParameter(1, key)
                .getSingleResult();
        return Arrays.asList(((Number) row[0]).intValue(), row[1], row[2]);
    }

    private UUID runId(String key) {
        var runId = em.createNativeQuery("select cast(sync_run_id as varchar(36)) from code_lists where key = ?1")
                .setParameter(1, key)
                .getSingleResult();
        return runId == null ? null : UUID.fromString((String) runId);
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
