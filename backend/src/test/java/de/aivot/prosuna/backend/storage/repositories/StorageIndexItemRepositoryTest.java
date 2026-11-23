package de.aivot.prosuna.backend.storage.repositories;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.storage.projections.StorageIndexItemStateProjection;
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
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=none",
        "spring.data.jpa.repositories.enabled=false",
        "spring.test.database.replace=none",
        "spring.datasource.url=jdbc:h2:mem:storage-index-items",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"})
@ContextConfiguration(classes = StorageIndexItemRepositoryTest.Config.class)
class StorageIndexItemRepositoryTest {
    // Microseconds, as stored by PostgreSQL, so the read value must match the stored one exactly.
    private static final Instant INDEXED = Instant.parse("2030-04-01T10:00:00.123456Z");
    private static final Instant NOW = Instant.parse("2030-05-01T12:00:00Z");
    @Autowired
    private EntityManager em;
    private StorageIndexItemRepository items;

    @BeforeEach
    void setup() {
        sql("drop table if exists storage_index_items");
        // Only the columns read and written by the synchronization's cleanup.
        sql("""
                create table storage_index_items (
                    storage_provider_id int, path_from_root varchar(2048), directory boolean, missing boolean,
                    updated timestamp(6) with time zone, primary key (storage_provider_id, path_from_root)
                )
                """);
        sql("""
                insert into storage_index_items values
                    (7, '/', true, false, '2030-04-01 10:00:00.123456+00'),
                    (7, '/a.pdf', false, false, '2030-04-01 10:00:00.123456+00'),
                    (8, '/a.pdf', false, false, '2030-04-01 10:00:00.123456+00')
                """);
        items = new JpaRepositoryFactory(em).getRepository(StorageIndexItemRepository.class);
    }

    @Test
    void itemUnchangedSinceItWasReadIsMarkedMissing() {
        var states = items.findStatesByStorageProviderId(7).stream()
                .sorted(Comparator.comparing(StorageIndexItemStateProjection::pathFromRoot))
                .toList();
        assertEquals(List.of(
                new StorageIndexItemStateProjection("/", true, INDEXED),
                new StorageIndexItemStateProjection("/a.pdf", false, INDEXED)
        ), states);

        assertEquals(1, items.markMissingIfUnchanged(7, "/a.pdf", states.getLast().updated(), NOW));

        assertEquals(true, sql("select missing from storage_index_items where storage_provider_id = 7 and path_from_root = '/a.pdf'"));
        assertEquals(false, sql("select missing from storage_index_items where storage_provider_id = 8"));
    }

    @Test
    void itemChangedSinceItWasReadIsKept() {
        // An upload during the run writes the item again.
        sql("update storage_index_items set updated = '2030-05-01 11:00:00+00' where path_from_root = '/a.pdf'");

        assertEquals(0, items.markMissingIfUnchanged(7, "/a.pdf", INDEXED, NOW));

        assertEquals(false, sql("select missing from storage_index_items where storage_provider_id = 7 and path_from_root = '/a.pdf'"));
    }

    private Object sql(String statement) {
        var query = em.createNativeQuery(statement);
        return statement.startsWith("select") ? query.getSingleResult() : query.executeUpdate();
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
