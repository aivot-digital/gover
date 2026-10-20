package de.aivot.prosuna.backend.process.repositories;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.test.context.ContextConfiguration;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=none",
        "spring.data.jpa.repositories.enabled=false",
        "spring.jpa.properties.hibernate.connection.url=jdbc:h2:mem:attachment-sizes;NON_KEYWORDS=KEY",
        "spring.jpa.properties.hibernate.connection.username=sa", "spring.jpa.properties.hibernate.connection.password=",
        "spring.jpa.properties.hibernate.connection.driver_class=org.h2.Driver"})
@ContextConfiguration(classes = ProcessInstanceAttachmentRepositoryTest.Config.class)
class ProcessInstanceAttachmentRepositoryTest {
    @Autowired
    private EntityManager em;

    @Test
    void resolvesSizesByBothProviderAndPathOnlyForRequestedAttachments() {
        // Only the columns used by the metadata query are needed here.
        sql("create table process_instance_attachments (key uuid primary key, storage_provider_id int, storage_path_from_root varchar)");
        sql("create table storage_index_items (storage_provider_id int, path_from_root varchar, size_in_bytes bigint)");
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        var other = UUID.randomUUID();
        var missing = UUID.randomUUID();
        sql("""
                insert into process_instance_attachments values
                ('%s', 1, '/same.pdf'), ('%s', 2, '/same.pdf'), ('%s', 1, '/other.pdf'), ('%s', 1, '/missing.pdf')
                """.formatted(first, second, other, missing));
        sql("""
                insert into storage_index_items values
                (1, '/same.pdf', 0), (2, '/same.pdf', 5000000000), (1, '/other.pdf', 42)
                """);
        var repository = new JpaRepositoryFactory(em).getRepository(ProcessInstanceAttachmentRepository.class);

        var sizes = repository.findSizesByKeys(List.of(first, second, missing)).stream()
                .collect(Collectors.toMap(item -> item.getKey(), item -> item.getSizeInBytes()));

        assertEquals(Map.of(first, 0L, second, 5_000_000_000L), sizes);
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
