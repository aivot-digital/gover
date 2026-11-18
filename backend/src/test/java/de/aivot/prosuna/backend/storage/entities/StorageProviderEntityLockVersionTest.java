package de.aivot.prosuna.backend.storage.entities;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.storage.enums.StorageProviderStatus;
import de.aivot.prosuna.backend.storage.repositories.StorageProviderRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers the Hibernate behavior the optimistic locking relies on, using a storage provider as an example
 * of an entity whose synchronization bookkeeping is excluded from the lock version.
 */
@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=none",
        "spring.data.jpa.repositories.enabled=false",
        "spring.jpa.properties.hibernate.connection.url=jdbc:h2:mem:storage-provider-lock-version",
        "spring.jpa.properties.hibernate.connection.username=sa", "spring.jpa.properties.hibernate.connection.password=",
        "spring.jpa.properties.hibernate.connection.driver_class=org.h2.Driver"})
@ContextConfiguration(classes = StorageProviderEntityLockVersionTest.Config.class)
class StorageProviderEntityLockVersionTest {
    @Autowired
    private EntityManager em;
    private StorageProviderRepository providers;

    @BeforeEach
    void setup() {
        sql("drop table if exists storage_providers");
        sql("""
                create table storage_providers (
                    id int primary key, storage_provider_definition_key varchar(255), storage_provider_definition_version int,
                    name varchar(255), description varchar(255), type smallint, status smallint, status_message varchar(4096),
                    read_only_storage boolean, configuration varchar(4096), max_file_size_in_bytes bigint,
                    system_provider boolean, test_provider boolean, metadata_attributes varchar(4096),
                    sync_run_id uuid, sync_started timestamp with time zone, last_sync timestamp with time zone,
                    created timestamp with time zone, updated timestamp with time zone,
                    lock_version bigint default 0 not null
                )
                """);
        // Status 1 is Syncing.
        sql("""
                insert into storage_providers (id, storage_provider_definition_key, storage_provider_definition_version,
                    name, description, type, status, read_only_storage, configuration, max_file_size_in_bytes,
                    system_provider, test_provider, metadata_attributes, sync_run_id, sync_started, created, updated)
                values (1, 'local-disk', 1, 'Lokale Dokumente', 'Beschreibung', 0, 1, false, '{}', 0,
                    false, false, '[]', '00000000-0000-0000-0000-000000000001', '2030-05-01 11:30:00+00',
                    '2030-05-01 10:00:00+00', '2030-05-01 10:00:00+00')
                """);
        providers = new JpaRepositoryFactory(em).getRepository(StorageProviderRepository.class);
    }

    @Test
    void contentChangeRaisesTheLockVersion() {
        var provider = providers.findById(1).orElseThrow();

        providers.saveAndFlush(provider.setName("Archiv"));

        assertEquals(1L, provider.getLockVersion());
        assertEquals(1L, storedLockVersion());
    }

    @Test
    void synchronizationBookkeepingKeepsTheLockVersion() {
        var provider = providers.findById(1).orElseThrow();

        providers.saveAndFlush(provider
                .setStatus(StorageProviderStatus.Synced)
                .setStatusMessage(null)
                .setLastSync(Instant.parse("2030-05-01T12:00:00Z"))
                .setSyncRunId(null)
                .setSyncStarted(null));

        // The update was written, including the timestamp set before each update, without raising the version.
        em.clear();
        var stored = providers.findById(1).orElseThrow();
        assertEquals(StorageProviderStatus.Synced, stored.getStatus());
        assertEquals(0L, stored.getLockVersion());
    }

    @Test
    void saveOfAnOutdatedStateIsRejected() {
        var provider = providers.findById(1).orElseThrow();
        // Another transaction committed a change after this one loaded the provider.
        sql("update storage_providers set name = 'Fremde Änderung', lock_version = 1 where id = 1");

        provider.setName("Archiv");

        assertThrows(OptimisticLockException.class, () -> providers.saveAndFlush(provider));
    }

    @Test
    void rebuiltEntityWithTheLoadedLockVersionUpdatesTheExistingRow() {
        var loaded = providers.findById(1).orElseThrow();
        em.clear();

        // Mirrors updates that save a newly built object for an existing row, such as departments and users.
        var rebuilt = new StorageProviderEntity()
                .setId(loaded.getId())
                .setStorageProviderDefinitionKey(loaded.getStorageProviderDefinitionKey())
                .setStorageProviderDefinitionVersion(loaded.getStorageProviderDefinitionVersion())
                .setName("Archiv")
                .setDescription(loaded.getDescription())
                .setType(loaded.getType())
                .setStatus(loaded.getStatus())
                .setReadOnlyStorage(loaded.getReadOnlyStorage())
                .setMaxFileSizeInBytes(loaded.getMaxFileSizeInBytes())
                .setSystemProvider(loaded.getSystemProvider())
                .setTestProvider(loaded.getTestProvider())
                .setConfiguration(loaded.getConfiguration())
                .setMetadataAttributes(loaded.getMetadataAttributes())
                .setCreated(loaded.getCreated())
                .setUpdated(loaded.getUpdated())
                .setLockVersion(loaded.getLockVersion());

        providers.saveAndFlush(rebuilt);

        assertEquals(1L, providers.count());
        assertEquals(1L, storedLockVersion());
    }

    private long storedLockVersion() {
        return ((Number) em.createNativeQuery("select lock_version from storage_providers where id = 1").getSingleResult()).longValue();
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
