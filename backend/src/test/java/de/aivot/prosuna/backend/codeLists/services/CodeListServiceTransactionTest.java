package de.aivot.prosuna.backend.codeLists.services;

import de.aivot.prosuna.backend.asset.services.AssetService;
import de.aivot.prosuna.backend.codeLists.entities.CodeListEntity;
import de.aivot.prosuna.backend.codeLists.models.CodeListColumnMapping;
import de.aivot.prosuna.backend.codeLists.repositories.CodeListItemRepository;
import de.aivot.prosuna.backend.codeLists.repositories.CodeListRepository;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.storage.services.StorageService;
import de.aivot.prosuna.backend.xrepository.services.XRepositoryCodeListService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.http.HttpStatus;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/** Runs the service in real transactions, so a test sees what a rejected write leaves in the database. */
@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=none",
        "spring.data.jpa.repositories.enabled=false",
        "spring.test.database.replace=none",
        "spring.datasource.url=jdbc:h2:mem:code-list-service;NON_KEYWORDS=KEY,VALUE",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"})
@ContextConfiguration(classes = CodeListServiceTransactionTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CodeListServiceTransactionTest {
    @Autowired
    private CodeListService service;
    @Autowired
    private EntityManager em;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setup() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            sql("drop table if exists code_lists");
            sql("""
                    create table code_lists (
                        key varchar(255) primary key, id integer, source_type smallint, source_ref varchar(255),
                        name varchar(255), description varchar(255), columns varchar array,
                        column_mappings varchar(4000), key_column_key varchar(255), status smallint,
                        status_message varchar(255), sync_keep_outdated boolean, sync_run_id uuid,
                        sync_started timestamp with time zone, last_sync timestamp with time zone,
                        created timestamp with time zone, updated timestamp with time zone, lock_version bigint
                    )
                    """);
            sql("drop table if exists code_list_items");
            sql("""
                    create table code_list_items (
                        id bigint primary key, code_list_id integer, columns varchar array,
                        created timestamp with time zone, updated timestamp with time zone, lock_version bigint
                    )
                    """);
            // Two rows share the name "Berlin", so the name cannot become the key column. Source type 0 is Manual.
            sql("""
                    insert into code_lists values ('cities', 7, 0, '', 'Städte', '', array['code', 'name'],
                        '[{"key":"code","name":"Code","index":1},{"key":"name","name":"Name","index":2}]', 'code', 2,
                        null, false, null, null, null, current_timestamp, current_timestamp, 0)
                    """);
            sql("""
                    insert into code_list_items values
                        (1, 7, array['001', 'Berlin'], current_timestamp, current_timestamp, 0),
                        (2, 7, array['002', 'Berlin'], current_timestamp, current_timestamp, 0)
                    """);
        });
    }

    @Test
    void aRejectedUpdateLeavesTheStoredListUnchanged() {
        var update = new CodeListEntity()
                .setName("Umbenannt")
                .setDescription("")
                .setColumnMappings(List.of(
                        new CodeListColumnMapping("code", "Code", 1),
                        new CodeListColumnMapping("name", "Name", 2)
                ))
                .setKeyColumnKey("name");

        var exception = assertThrows(ResponseException.class, () -> service.update("cities", update));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        var stored = (Object[]) em.createNativeQuery("select name, key_column_key from code_lists where key = 'cities'")
                .getSingleResult();
        assertEquals(List.of("Städte", "code"), List.of(stored));
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

        @Bean
        CodeListService codeListService(EntityManagerFactory emf, PlatformTransactionManager transactionManager) {
            var repositories = new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(emf));
            return new CodeListService(
                    repositories.getRepository(CodeListRepository.class),
                    repositories.getRepository(CodeListItemRepository.class),
                    mock(XRepositoryCodeListService.class),
                    mock(AssetService.class),
                    mock(StorageService.class),
                    transactionManager,
                    new SimpleMeterRegistry()
            );
        }
    }
}
