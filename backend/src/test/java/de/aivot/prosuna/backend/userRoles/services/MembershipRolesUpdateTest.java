package de.aivot.prosuna.backend.userRoles.services;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.department.repositories.DepartmentMembershipRepository;
import de.aivot.prosuna.backend.department.repositories.DepartmentRepository;
import de.aivot.prosuna.backend.department.services.DepartmentMembershipService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.teams.repositories.TeamMembershipRepository;
import de.aivot.prosuna.backend.teams.repositories.TeamRepository;
import de.aivot.prosuna.backend.teams.services.TeamMembershipService;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
import de.aivot.prosuna.backend.user.services.UserService;
import de.aivot.prosuna.backend.userRoles.entities.UserRoleAssignmentEntity;
import de.aivot.prosuna.backend.userRoles.repositories.UserRoleAssignmentRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * The role dialogs of department and team members save the whole role set of a membership, guarded by the lock
 * version of the membership row.
 */
@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=none",
        "spring.data.jpa.repositories.enabled=false",
        "spring.jpa.properties.hibernate.connection.url=jdbc:h2:mem:membership-roles-update",
        "spring.jpa.properties.hibernate.connection.username=sa", "spring.jpa.properties.hibernate.connection.password=",
        "spring.jpa.properties.hibernate.connection.driver_class=org.h2.Driver"})
@ContextConfiguration(classes = MembershipRolesUpdateTest.Config.class)
class MembershipRolesUpdateTest {
    @Autowired
    private EntityManager em;
    private DepartmentMembershipService departmentMemberships;
    private TeamMembershipService teamMemberships;

    @BeforeEach
    void setup() {
        sql("drop table if exists department_memberships");
        sql("drop table if exists team_memberships");
        sql("drop table if exists domain_role_assignments");
        sql("drop sequence if exists domain_role_assignments_id_seq");
        sql("""
                create table department_memberships (id int primary key, department_id int, user_id varchar(36),
                    created timestamp with time zone, updated timestamp with time zone, lock_version bigint default 0 not null)
                """);
        sql("""
                create table team_memberships (id int primary key, team_id int, user_id varchar(36),
                    created timestamp with time zone, updated timestamp with time zone, lock_version bigint default 0 not null)
                """);
        sql("""
                create table domain_role_assignments (id int primary key, department_membership_id int, team_membership_id int,
                    domain_role_id int, created timestamp with time zone)
                """);
        sql("create sequence domain_role_assignments_id_seq start with 100");
        sql("insert into department_memberships values (1, 5, 'user', now(), now(), 0)");
        sql("insert into team_memberships values (1, 6, 'user', now(), now(), 0)");
        sql("insert into domain_role_assignments values (1, 1, null, 10, now()), (2, 1, null, 11, now())");
        sql("insert into domain_role_assignments values (3, null, 1, 10, now()), (4, null, 1, 11, now())");

        var factory = new JpaRepositoryFactory(em);
        var assignments = new UserRoleAssignmentService(factory.getRepository(UserRoleAssignmentRepository.class));
        departmentMemberships = new DepartmentMembershipService(factory.getRepository(DepartmentMembershipRepository.class),
                mock(DepartmentRepository.class), mock(UserService.class), assignments);
        teamMemberships = new TeamMembershipService(factory.getRepository(TeamMembershipRepository.class),
                mock(TeamRepository.class), mock(UserRepository.class), assignments);
    }

    @Test
    void departmentRoleSetReplacesTheAssignmentsAndRaisesTheLockVersion() throws ResponseException {
        var changes = departmentMemberships.updateRoles(1, 0L, List.of(11, 12));
        em.flush();

        assertEquals(List.of(12), roleIds(changes.created()));
        assertEquals(List.of(10), roleIds(changes.deleted()));
        assertEquals(Set.of(11, 12), storedRoles("department_membership_id"));
        assertEquals(1L, storedLockVersion("department_memberships"));
    }

    @Test
    void departmentRoleSetBasedOnAnOutdatedVersionChangesNothing() {
        // Another tab saved the roles after this dialog loaded the membership.
        sql("update department_memberships set lock_version = 1 where id = 1");
        em.clear();

        var exception = assertThrows(ResponseException.class,
                () -> departmentMemberships.updateRoles(1, 0L, List.of(12)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals(Set.of(10, 11), storedRoles("department_membership_id"));
        assertEquals(1L, storedLockVersion("department_memberships"));
    }

    @Test
    void teamRoleSetReplacesTheAssignmentsAndRaisesTheLockVersion() throws ResponseException {
        var changes = teamMemberships.updateRoles(1, 0L, List.of(10, 12, 12));
        em.flush();

        assertEquals(List.of(12), roleIds(changes.created()));
        assertEquals(List.of(11), roleIds(changes.deleted()));
        assertEquals(Set.of(10, 12), storedRoles("team_membership_id"));
        assertEquals(1L, storedLockVersion("team_memberships"));
    }

    @Test
    void teamRoleSetBasedOnAnOutdatedVersionChangesNothing() {
        sql("update team_memberships set lock_version = 1 where id = 1");
        em.clear();

        var exception = assertThrows(ResponseException.class,
                () -> teamMemberships.updateRoles(1, 0L, List.of()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals(Set.of(10, 11), storedRoles("team_membership_id"));
    }

    private static List<Integer> roleIds(List<UserRoleAssignmentEntity> assignments) {
        return assignments.stream().map(UserRoleAssignmentEntity::getUserRoleId).toList();
    }

    private Set<Integer> storedRoles(String membershipColumn) {
        List<?> rows = em.createNativeQuery("select domain_role_id from domain_role_assignments where " + membershipColumn + " = 1")
                .getResultList();
        return rows.stream().map(row -> ((Number) row).intValue()).collect(Collectors.toSet());
    }

    private long storedLockVersion(String table) {
        return ((Number) em.createNativeQuery("select lock_version from " + table + " where id = 1").getSingleResult()).longValue();
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
