package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.dtos.ProcessDepartmentOptionDTO;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.filters.ProcessFilter;
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
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.connection.url=jdbc:h2:mem:process-department-options",
        "spring.jpa.properties.hibernate.connection.username=sa",
        "spring.jpa.properties.hibernate.connection.password=",
        "spring.jpa.properties.hibernate.connection.driver_class=org.h2.Driver"
})
@ContextConfiguration(classes = ProcessDepartmentOptionServiceTest.JpaTestConfiguration.class)
class ProcessDepartmentOptionServiceTest {
    private static final String READ = ProcessPermissionProvider.PROCESS_DEFINITION_READ;
    @Autowired
    private EntityManager entityManager;
    private PermissionService permissions;
    private ProcessRepository processes;
    private ProcessService processService;
    private ProcessDepartmentOptionService options;

    @BeforeEach
    void setUp() {
        permissions = mock(PermissionService.class);
        processes = mock(ProcessRepository.class);
        processService = new ProcessService(processes, mock(ProcessSlugHistoryRepository.class), permissions);
        options = new ProcessDepartmentOptionService(processService, entityManager);
        // Only the projected columns are needed; no process or department configuration is loaded.
        entityManager.createNativeQuery("create table if not exists departments (id integer primary key, name varchar(255))").executeUpdate();
        entityManager.createNativeQuery("create table if not exists processes (id integer primary key, department_id integer)").executeUpdate();
        entityManager.createNativeQuery("insert into departments values (10, 'Alpha'), (20, 'Beta'), (30, 'Gamma'), (40, 'Unused')").executeUpdate();
        entityManager.createNativeQuery("insert into processes values (101, 10), (102, 10), (201, 20), (301, 30)").executeUpdate();
    }

    @Test
    void systemReadIncludesEveryOwnerButNotDepartmentsWithoutProcesses() {
        when(permissions.hasSystemPermission("user", READ)).thenReturn(true);
        assertEquals(List.of(option(10, "Alpha"), option(20, "Beta"), option(30, "Gamma")), options.listForUser("user"));
        verify(permissions, never()).getDepartmentsWithPermission(any(), any());
        verify(processes, never()).getProcessIdsWithPermission(any(), any());
    }

    @Test
    void combinesDepartmentAndExplicitProcessGrantsWithoutDuplicatesOrOtherOwners() {
        when(permissions.getDepartmentsWithPermission("user", READ)).thenReturn(List.of(10));
        when(processes.getProcessIdsWithPermission("user", READ)).thenReturn(List.of(102, 201));
        assertEquals(List.of(option(10, "Alpha"), option(20, "Beta")), options.listForUser("user"));
    }

    @Test
    void explicitProcessGrantDoesNotRequireDepartmentReadPermission() {
        when(processes.getProcessIdsWithPermission("user", READ)).thenReturn(List.of(201));
        assertEquals(List.of(option(20, "Beta")), options.listForUser("user"));
        verify(permissions).getDepartmentsWithPermission("user", READ);
    }

    @Test
    void noProcessReadGrantReturnsNoOptions() {
        when(processes.getProcessIdsWithPermission("other-user", READ)).thenReturn(List.of(201));
        assertEquals(List.of(), options.listForUser("user"));
    }

    @Test
    void departmentFilterCannotWidenTheSharedProcessAccessScope() {
        when(permissions.getDepartmentsWithPermission("user", READ)).thenReturn(List.of(10));
        var access = processService.getReadAccessSpecification("user");
        var filter = ProcessFilter.create().setDepartmentId(30).build();
        var builder = entityManager.getCriteriaBuilder();
        var query = builder.createQuery(Integer.class);
        var root = query.from(ProcessEntity.class);
        query.select(root.get("id")).where(filter.and(access).toPredicate(root, query, builder));
        assertEquals(List.of(), entityManager.createQuery(query).getResultList());
    }

    private ProcessDepartmentOptionDTO option(int id, String name) {
        return new ProcessDepartmentOptionDTO(id, name);
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
