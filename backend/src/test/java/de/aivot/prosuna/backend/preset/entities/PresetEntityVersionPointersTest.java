package de.aivot.prosuna.backend.preset.entities;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.preset.repositories.PresetRepository;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The version pointers of presets (and processes) are maintained by database triggers that do not raise the lock
 * version, so an update must never write them back from a copy loaded earlier.
 */
@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=none",
        "spring.data.jpa.repositories.enabled=false",
        "spring.jpa.properties.hibernate.connection.url=jdbc:h2:mem:preset-version-pointers;NON_KEYWORDS=KEY",
        "spring.jpa.properties.hibernate.connection.username=sa", "spring.jpa.properties.hibernate.connection.password=",
        "spring.jpa.properties.hibernate.connection.driver_class=org.h2.Driver"})
@ContextConfiguration(classes = PresetEntityVersionPointersTest.Config.class)
class PresetEntityVersionPointersTest {
    private static final UUID KEY = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private EntityManager em;
    private PresetRepository presets;

    @BeforeEach
    void setup() {
        sql("drop table if exists presets");
        sql("""
                create table presets (
                    key uuid primary key, title varchar(255), published_version smallint, drafted_version smallint,
                    version_count int, created timestamp with time zone, updated timestamp with time zone,
                    lock_version bigint default 0 not null
                )
                """);
        sql("insert into presets (key, title, published_version, drafted_version, version_count) values ('" + KEY + "', 'Vorlage', null, 1, 1)");
        presets = new JpaRepositoryFactory(em).getRepository(PresetRepository.class);
    }

    @Test
    void saveOfAnEarlierCopyKeepsTheVersionPointersOfTheTrigger() {
        var preset = presets.findById(KEY).orElseThrow();
        // A publish committed meanwhile: the trigger moved the pointers without raising the lock version.
        sql("update presets set published_version = 1, drafted_version = null where key = '" + KEY + "'");

        presets.saveAndFlush(preset.setTitle("Neuer Titel"));

        em.clear();
        var stored = presets.findById(KEY).orElseThrow();
        assertEquals("Neuer Titel", stored.getTitle());
        assertEquals(1, stored.getPublishedVersion());
        assertNull(stored.getDraftedVersion());
        assertEquals(1, stored.getVersionCount());
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
