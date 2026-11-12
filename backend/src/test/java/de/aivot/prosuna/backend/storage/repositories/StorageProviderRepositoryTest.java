package de.aivot.prosuna.backend.storage.repositories;

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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=none",
        "spring.data.jpa.repositories.enabled=false",
        "spring.test.database.replace=none",
        "spring.datasource.url=jdbc:h2:mem:storage-provider-sync",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"})
@ContextConfiguration(classes = StorageProviderRepositoryTest.Config.class)
class StorageProviderRepositoryTest {
    private static final Instant NOW = Instant.parse("2030-05-01T12:00:00Z");
    private static final Instant BEFORE = Instant.parse("2030-05-01T11:55:00Z");
    private static final Instant STALE_BEFORE = Instant.parse("2030-05-01T11:00:00Z");
    private static final UUID RUN = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_RUN = UUID.fromString("00000000-0000-0000-0000-000000000002");
    @Autowired
    private EntityManager em;
    private StorageProviderRepository providers;

    @BeforeEach
    void setup() {
        sql("drop table if exists storage_providers");
        // Only the columns written by the synchronization queries; statuses use their persisted ordinals.
        sql("""
                create table storage_providers (
                    id int primary key, status smallint, status_message varchar(255), sync_run_id uuid,
                    sync_started timestamp with time zone, updated timestamp with time zone
                )
                """);
        // Statuses: 0 SyncPending, 1 Syncing, 2 Synced, 3 SyncFailed.
        sql("""
                insert into storage_providers values
                    (1, 0, null, null, null, '2030-05-01 10:00:00+00'),
                    (2, 0, null, null, null, '2030-05-01 11:59:00+00'),
                    (3, 2, 'old', null, null, '2030-05-01 10:00:00+00'),
                    (4, 1, null, '%s', '2030-05-01 11:30:00+00', '2030-05-01 11:30:00+00'),
                    (5, 1, null, '%s', '2030-05-01 10:00:00+00', '2030-05-01 10:00:00+00')
                """.formatted(RUN, OTHER_RUN));
        providers = new JpaRepositoryFactory(em).getRepository(StorageProviderRepository.class);
    }

    @Test
    void onlyOneRunStartsARequestedSynchronization() {
        assertEquals(1, providers.claimSync(1, UUID.randomUUID(), NOW, STALE_BEFORE));
        assertEquals(0, providers.claimSync(1, UUID.randomUUID(), NOW, STALE_BEFORE));
        assertEquals(0, providers.claimSync(3, UUID.randomUUID(), NOW, STALE_BEFORE));
        assertEquals(0, providers.claimSync(4, UUID.randomUUID(), NOW, STALE_BEFORE));
    }

    @Test
    void interruptedRunIsTakenOverAndCanNoLongerRecordItsResult() {
        assertEquals(List.of(5), providers.findInterruptedSyncIds(STALE_BEFORE));

        var newRun = UUID.randomUUID();
        assertEquals(1, providers.claimSync(5, newRun, NOW, STALE_BEFORE));

        assertEquals(0, providers.failSync(5, OTHER_RUN, "Fehler", NOW));
        assertEquals(0, providers.releaseSync(5, OTHER_RUN));
        assertEquals(1, providers.failSync(5, newRun, "Fehler", NOW));
        assertEquals(List.of(), providers.findInterruptedSyncIds(STALE_BEFORE));
    }

    @Test
    void requestDuringARunWaitsUntilTheRunReleasesIt() {
        assertEquals(1, providers.requestSync(4, NOW));
        assertEquals(0, providers.claimSync(4, UUID.randomUUID(), NOW, STALE_BEFORE));
        assertEquals(0, providers.failSync(4, RUN, "Fehler", NOW));

        assertEquals(1, providers.releaseSync(4, RUN));

        assertEquals(1, providers.claimSync(4, UUID.randomUUID(), NOW, STALE_BEFORE));
        assertEquals(0, providers.requestSync(99, NOW));
    }

    @Test
    void onlyOneCallerPublishesAPendingRequestAgain() {
        assertEquals(List.of(1), providers.findPendingSyncIds(BEFORE));
        assertEquals(1, providers.claimSyncRedispatch(1, BEFORE, NOW));
        assertEquals(0, providers.claimSyncRedispatch(1, BEFORE, NOW));
        assertEquals(0, providers.claimSyncRedispatch(3, BEFORE, NOW));
        assertEquals(List.of(), providers.findPendingSyncIds(BEFORE));
    }

    @Test
    void nightlySyncRequestsAllProviders() {
        assertEquals(5, providers.requestSyncOfAll(NOW));

        assertEquals(List.of(1, 2, 3, 4, 5), providers.findPendingSyncIds(NOW));
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
