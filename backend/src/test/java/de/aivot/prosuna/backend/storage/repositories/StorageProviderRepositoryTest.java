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

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=none",
        "spring.data.jpa.repositories.enabled=false",
        "spring.jpa.properties.hibernate.connection.url=jdbc:h2:mem:storage-provider-sync",
        "spring.jpa.properties.hibernate.connection.username=sa", "spring.jpa.properties.hibernate.connection.password=",
        "spring.jpa.properties.hibernate.connection.driver_class=org.h2.Driver"})
@ContextConfiguration(classes = StorageProviderRepositoryTest.Config.class)
class StorageProviderRepositoryTest {
    private static final Instant NOW = Instant.parse("2030-05-01T12:00:00Z");
    private static final Instant BEFORE = Instant.parse("2030-05-01T11:55:00Z");
    @Autowired
    private EntityManager em;
    private StorageProviderRepository providers;

    @BeforeEach
    void setup() {
        sql("drop table if exists storage_providers");
        // Only the columns written by the synchronization queries; statuses use their persisted ordinals.
        sql("create table storage_providers (id int primary key, status smallint, updated timestamp with time zone)");
        // Statuses: 0 SyncPending, 1 Syncing, 2 Synced, 3 SyncFailed.
        sql("""
                insert into storage_providers values
                    (1, 0, '2030-05-01 10:00:00+00'),
                    (2, 0, '2030-05-01 11:59:00+00'),
                    (3, 2, '2030-05-01 10:00:00+00')
                """);
        providers = new JpaRepositoryFactory(em).getRepository(StorageProviderRepository.class);
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
        assertEquals(3, providers.requestSyncOfAll(NOW));

        assertEquals(List.of(1, 2, 3), providers.findPendingSyncIds(NOW));
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
