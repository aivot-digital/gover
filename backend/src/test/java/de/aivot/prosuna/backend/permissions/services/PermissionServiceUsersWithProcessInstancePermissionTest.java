package de.aivot.prosuna.backend.permissions.services;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.department.repositories.DepartmentRepository;
import de.aivot.prosuna.backend.permissions.repositories.VUserDepartmentPermissionRepository;
import de.aivot.prosuna.backend.permissions.repositories.VUserSystemPermissionRepository;
import de.aivot.prosuna.backend.permissions.repositories.VUserTeamPermissionRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessRepository;
import de.aivot.prosuna.backend.teams.repositories.TeamRepository;
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

import java.util.List;
import java.util.Set;

import static de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=none",
        "spring.data.jpa.repositories.enabled=false",
        "spring.test.database.replace=none",
        "spring.datasource.url=jdbc:h2:mem:users-with-process-instance-permission",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"})
@ContextConfiguration(classes = PermissionServiceUsersWithProcessInstancePermissionTest.Config.class)
class PermissionServiceUsersWithProcessInstancePermissionTest {
    private static final List<String> CONNECTED_USERS = List.of("system-reader", "system-other", "department-reader",
            "team-reader", "other-instance-reader", "updater-only", "without-grants");

    @Autowired
    private EntityManager em;
    private PermissionService permissions;

    @BeforeEach
    void setUp() {
        sql("drop table if exists v_user_system_permission");
        sql("drop table if exists v_user_process_instance_access_permissions");
        // The views flattened to the columns the queries read.
        sql("create table v_user_system_permission (user_id varchar(255), permissions varchar(255) array)");
        sql("""
                create table v_user_process_instance_access_permissions (
                    user_id varchar(255), via_source_team_id integer, via_source_department_id integer,
                    target_process_instance_id bigint, permissions varchar(255) array
                )
                """);
        sql("""
                insert into v_user_system_permission values
                    ('system-reader', array['process_instance.read']),
                    ('system-other', array['process.read'])
                """);
        sql("""
                insert into v_user_process_instance_access_permissions values
                    ('department-reader', null, 1, 7, array['process_instance.read', 'process_instance.update']),
                    ('department-reader', 2, null, 7, array['process_instance.read']),
                    ('team-reader', 2, null, 7, array['process_instance.read']),
                    ('other-instance-reader', null, 1, 8, array['process_instance.read']),
                    ('updater-only', null, 1, 7, array['process_instance.update']),
                    ('not-connected', null, 1, 7, array['process_instance.read'])
                """);

        var repositories = new JpaRepositoryFactory(em);
        permissions = new PermissionService(
                mock(VUserDepartmentPermissionRepository.class),
                mock(VUserTeamPermissionRepository.class),
                repositories.getRepository(VUserSystemPermissionRepository.class),
                mock(DepartmentRepository.class),
                mock(TeamRepository.class),
                mock(ProcessRepository.class),
                repositories.getRepository(ProcessInstanceRepository.class)
        );
    }

    @Test
    void returnsTheUsersWithTheSystemPermissionOrAGrantForTheInstance() {
        var allowed = permissions.filterUsersWithProcessInstancePermission(CONNECTED_USERS, 7L, PROCESS_INSTANCE_READ);

        assertEquals(Set.of("system-reader", "department-reader", "team-reader"), allowed);
    }

    @Test
    void returnsOnlyTheSystemPermissionHoldersForAnInstanceWithoutGrants() {
        var allowed = permissions.filterUsersWithProcessInstancePermission(CONNECTED_USERS, 9L, PROCESS_INSTANCE_READ);

        assertEquals(Set.of("system-reader"), allowed);
    }

    @Test
    void returnsNobodyForNoUsers() {
        assertEquals(Set.of(), permissions.filterUsersWithProcessInstancePermission(List.of(), 7L, PROCESS_INSTANCE_READ));
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
