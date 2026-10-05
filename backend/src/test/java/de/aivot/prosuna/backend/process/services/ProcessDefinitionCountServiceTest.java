package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.lib.services.SpecificationCountService;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.repositories.ProcessRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessSlugHistoryRepository;
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
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.connection.url=jdbc:h2:mem:process-definition-counts",
        "spring.jpa.properties.hibernate.connection.username=sa",
        "spring.jpa.properties.hibernate.connection.password=",
        "spring.jpa.properties.hibernate.connection.driver_class=org.h2.Driver"
})
@ContextConfiguration(classes = ProcessDefinitionCountServiceTest.JpaTestConfiguration.class)
class ProcessDefinitionCountServiceTest {
    private static final String READ = ProcessPermissionProvider.PROCESS_DEFINITION_READ;
    @Autowired
    private EntityManager entityManager;
    private PermissionService permissions;
    private ProcessRepository processes;
    private ProcessDefinitionCountService counts;

    @BeforeEach
    void setUp() {
        permissions = mock(PermissionService.class);
        processes = mock(ProcessRepository.class);
        var processService = new ProcessService(processes, mock(ProcessSlugHistoryRepository.class), permissions, mock(PlatformTransactionManager.class));
        counts = new ProcessDefinitionCountService(processService, new SpecificationCountService(entityManager));
        entityManager.createNativeQuery("create table if not exists processes (id integer primary key, department_id integer, internal_title varchar, drafted_version integer, published_version integer)").executeUpdate();
        entityManager.createNativeQuery("insert into processes values (101, 10, 'Anmeldung A', 2, 1), (102, 10, 'Anmeldung B', null, 1), (201, 20, 'Genehmigung', 1, null), (301, 30, 'Archiviert', null, null)").executeUpdate();
    }

    @Test
    void countsOverlappingDraftAndPublishedCategoriesWithSystemAccess() {
        when(permissions.hasSystemPermission("user", READ)).thenReturn(true);
        assertEquals(Map.of("drafted", 2L, "published", 2L), counts.count("user"));
        verify(permissions, never()).getDepartmentsWithPermission(any(), any());
        verify(processes, never()).getProcessIdsWithPermission(any(), any());
    }

    @Test
    void combinesDepartmentAndExplicitProcessGrantsWithoutDuplicatingProcesses() {
        when(permissions.getDepartmentsWithPermission("user", READ)).thenReturn(List.of(10));
        when(processes.getProcessIdsWithPermission("user", READ)).thenReturn(List.of(101, 201));
        assertEquals(Map.of("drafted", 2L, "published", 2L), counts.count("user"));
        when(permissions.getDepartmentsWithPermission("user", READ)).thenReturn(List.of());
        when(processes.getProcessIdsWithPermission("user", READ)).thenReturn(List.of(201));
        assertEquals(Map.of("drafted", 1L, "published", 0L), counts.count("user"));
    }

    @Test
    void returnsZerosWhenOnlyWithdrawnProcessesAreReadable() {
        when(processes.getProcessIdsWithPermission("user", READ)).thenReturn(List.of(301));
        assertEquals(Map.of("drafted", 0L, "published", 0L), counts.count("user"));
    }

    @Test
    void returnsZerosWithoutReadAccess() {
        when(processes.getProcessIdsWithPermission("other-user", READ)).thenReturn(List.of(101));
        var zero = Map.of("drafted", 0L, "published", 0L);
        assertEquals(zero, counts.count("user"));
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
