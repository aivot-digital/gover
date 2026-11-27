package de.aivot.prosuna.backend.user.repositories;

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

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=none",
        "spring.data.jpa.repositories.enabled=false",
        "spring.jpa.properties.hibernate.connection.url=jdbc:h2:mem:user-api-keys",
        "spring.jpa.properties.hibernate.connection.username=sa", "spring.jpa.properties.hibernate.connection.password=",
        "spring.jpa.properties.hibernate.connection.driver_class=org.h2.Driver"})
@ContextConfiguration(classes = UserApiKeyRepositoryTest.Config.class)
class UserApiKeyRepositoryTest {
    @Autowired
    private EntityManager em;
    private UserApiKeyRepository keys;

    @BeforeEach
    void setup() {
        sql("drop table if exists user_api_keys");
        sql("drop table if exists users");
        // Only the columns read by the key lookup
        sql("create table users (id varchar(36) primary key, enabled boolean, deleted_in_idp boolean)");
        sql("""
                create table user_api_keys (
                    id int primary key, user_id varchar(36), name varchar(255),
                    created timestamp with time zone, expiration timestamp with time zone, key_hash varchar(64)
                )
                """);
        sql("insert into users values ('active', true, false), ('disabled', false, false), ('deleted', true, true)");
        sql("""
                insert into user_api_keys values
                    (1, 'active', 'Active', '2026-05-01 12:00:00+00', '2030-05-01 12:00:00+00', 'active-hash'),
                    (2, 'disabled', 'Disabled', '2026-05-01 12:00:00+00', '2030-05-01 12:00:00+00', 'disabled-hash'),
                    (3, 'deleted', 'Deleted', '2026-05-01 12:00:00+00', '2030-05-01 12:00:00+00', 'deleted-hash')
                """);
        keys = new JpaRepositoryFactory(em).getRepository(UserApiKeyRepository.class);
    }

    @Test
    void findsTheKeyOfAnActiveUser() {
        assertEquals(1, keys.findByKeyHashOfActiveUser("active-hash").orElseThrow().getId());
    }

    @Test
    void findsNoKeyOfDisabledOrIdpDeletedUsersOrUnknownHashes() {
        assertTrue(keys.findByKeyHashOfActiveUser("disabled-hash").isEmpty());
        assertTrue(keys.findByKeyHashOfActiveUser("deleted-hash").isEmpty());
        assertTrue(keys.findByKeyHashOfActiveUser("unknown-hash").isEmpty());
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
