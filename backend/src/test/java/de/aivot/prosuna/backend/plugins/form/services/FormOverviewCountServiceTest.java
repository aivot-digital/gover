package de.aivot.prosuna.backend.plugins.form.services;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.lib.services.SpecificationCountService;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.plugins.form.v1.nodes.FormTriggerNodeV1;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.repositories.ProcessRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessSlugHistoryRepository;
import de.aivot.prosuna.backend.process.services.ProcessService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.connection.url=jdbc:h2:mem:form-overview-counts",
        "spring.jpa.properties.hibernate.connection.username=sa",
        "spring.jpa.properties.hibernate.connection.password=",
        "spring.jpa.properties.hibernate.connection.driver_class=org.h2.Driver"
})
@ContextConfiguration(classes = FormOverviewCountServiceTest.JpaTestConfiguration.class)
public class FormOverviewCountServiceTest {
    private static final String READ = ProcessPermissionProvider.PROCESS_DEFINITION_READ;
    @Autowired
    private EntityManager entityManager;
    private PermissionService permissions;
    private ProcessRepository processes;
    private FormOverviewCountService counts;

    @BeforeEach
    void setUp() {
        permissions = mock(PermissionService.class);
        processes = mock(ProcessRepository.class);
        var processService = new ProcessService(processes, mock(ProcessSlugHistoryRepository.class), permissions);
        var trigger = mock(FormTriggerNodeV1.class);
        when(trigger.getKey()).thenReturn("form:form:1");
        counts = new FormOverviewCountService(processService, new SpecificationCountService(entityManager), trigger);
        sql("create table if not exists processes (id integer primary key, department_id integer, internal_title varchar, drafted_version integer, published_version integer)");
        sql("create table if not exists process_nodes (id integer primary key, process_id integer, process_version integer, name varchar, process_node_definition_key varchar, process_node_definition_version integer, configuration varchar)");
        sql("insert into processes values (10, 100, 'Anmeldung', 3, 2), (20, 200, 'Geschützt', 1, null), (30, 100, 'Archiviert', null, null)");
        sql("""
                insert into process_nodes values
                (1,10,1,'Alte Version','form:form:1',1,'{}'),
                (2,10,2,'Online-Eingang','form:form:1',1,'{"formLayout":{"value":{"publicTitle":"Wohnsitz anmelden","showOnFormIndexPage":false}},"formSlug":{"value":"wohnsitz"}}'),
                (3,10,3,'Entwurf','form:form:1',1,'{}'),
                (4,10,3,'Weiterer Entwurf','form:form:1',1,'{}'),
                (5,20,1,'Geschützter Entwurf','form:form:1',1,'{}'),
                (6,30,1,'Zurückgezogen','form:form:1',1,'{}'),
                (7,10,2,'Andere Definition','other',1,'{}'),
                (8,10,2,'Andere Definitionsversion','form:form:1',2,'{}')
                """);
    }

    @Test
    void countsOnlyCurrentVersionsAndFormDefinitionsIncludingUnlistedForms() {
        when(permissions.getDepartmentsWithPermission("user", READ)).thenReturn(List.of(100));
        assertEquals(Map.of("Published", 1L, "Drafted", 2L), counts.count("user"));
    }

    @Test
    void handlesSystemOverridesExplicitGrantsAndOverlappingAccessWithoutDuplicates() {
        when(processes.getProcessIdsWithPermission("user", READ)).thenReturn(List.of(10, 20));
        when(permissions.getDepartmentsWithPermission("user", READ)).thenReturn(List.of(100));
        assertEquals(Map.of("Published", 1L, "Drafted", 3L), counts.count("user"));
        when(permissions.getDepartmentsWithPermission("user", READ)).thenReturn(List.of());
        when(processes.getProcessIdsWithPermission("user", READ)).thenReturn(List.of(20));
        assertEquals(Map.of("Published", 0L, "Drafted", 1L), counts.count("user"));
        when(processes.getProcessIdsWithPermission("user", READ)).thenReturn(List.of());
        when(permissions.hasSystemPermission("user", READ)).thenReturn(true);
        assertEquals(Map.of("Published", 1L, "Drafted", 3L), counts.count("user"));
    }

    @Test
    void followsTheCurrentVersionsAfterPublishing() {
        when(permissions.getDepartmentsWithPermission("user", READ)).thenReturn(List.of(100));
        assertEquals(Map.of("Published", 1L, "Drafted", 2L), counts.count("user"));
        sql("update processes set published_version = 3, drafted_version = null where id = 10");
        assertEquals(Map.of("Published", 2L, "Drafted", 0L), counts.count("user"));
    }

    @Test
    void doesNotRevealFormsWithoutProcessReadAccess() {
        when(processes.getProcessIdsWithPermission("other-user", READ)).thenReturn(List.of(10));
        assertEquals(Map.of("Published", 0L, "Drafted", 0L), counts.count("user"));
    }

    private void sql(String statement) {
        entityManager.createNativeQuery(statement).executeUpdate();
    }

    @Configuration(proxyBeanMethods = false)
    @AutoConfigurationPackage
    @EntityScan("de.aivot.prosuna.backend")
    static class JpaTestConfiguration {
        @Bean
        JsonMapper jsonMapper() {
            return JsonMapperTestUtils.createMapper();
        }
    }
}
