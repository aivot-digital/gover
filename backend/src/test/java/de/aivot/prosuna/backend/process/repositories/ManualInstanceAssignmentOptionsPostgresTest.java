package de.aivot.prosuna.backend.process.repositories;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.process.dtos.ProcessAssignmentOptionDTO;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Uses a migrated PostgreSQL database so that deputy and membership grants come from the production views.
// All fixtures, including changes to live access controls, are rolled back by @DataJpaTest.
@EnabledIfEnvironmentVariable(named = "PROSUNA_TEST_POSTGRES_URL", matches = "jdbc:postgresql:.*")
@DataJpaTest(showSql = false, properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=none",
        "spring.data.jpa.repositories.enabled=false", "spring.test.database.replace=none",
        "spring.datasource.url=${PROSUNA_TEST_POSTGRES_URL}", "spring.datasource.username=${PROSUNA_TEST_POSTGRES_USER:}",
        "spring.datasource.password=${PROSUNA_TEST_POSTGRES_PASSWORD:}"})
@ContextConfiguration(classes = ManualInstanceAssignmentOptionsPostgresTest.Config.class)
@Sql("/process/manual-instance-assignment-options.sql")
class ManualInstanceAssignmentOptionsPostgresTest {
    private static final long INSTANCE_ID = -1L;
    private static final String ORIGINAL = "assignment-original";
    private static final String DEPUTY = "assignment-deputy";
    private static final String GLOBAL = "assignment-global";

    @Autowired
    private EntityManager em;
    @Autowired
    private JdbcTemplate jdbc;
    private ProcessInstanceRepository instances;

    @BeforeEach
    void setUp() {
        instances = new JpaRepositoryFactory(em).getRepository(ProcessInstanceRepository.class);
    }

    @Test
    void deputyNeedsOwnReadPermissionThroughAnEligibleMembership() {
        // The deputy can already read this instance by representing the original, and has own read in another unit.
        assertEquals(List.of(DEPUTY), instances.findUserIdsWithPermission(List.of(DEPUTY), INSTANCE_ID, PROCESS_INSTANCE_READ));
        assertEquals(List.of(ORIGINAL), optionIds());
        assertTrue(instances.findManualInstanceAssignmentOptions(INSTANCE_ID, PROCESS_INSTANCE_READ, DEPUTY).isEmpty());

        jdbc.update("insert into department_memberships (id, department_id, user_id) values (-5, -1, ?)", DEPUTY);
        assertEquals(List.of(ORIGINAL), optionIds());
        jdbc.update("insert into domain_role_assignments (id, department_membership_id, domain_role_id) values (-7, -5, -1)");

        assertEquals(List.of(ORIGINAL, DEPUTY), optionIds());
        assertEquals(List.of(new ProcessAssignmentOptionDTO(DEPUTY, "Bea Beispiel", "bea@example.org")),
                instances.findManualInstanceAssignmentOptions(INSTANCE_ID, PROCESS_INSTANCE_READ, DEPUTY));
    }

    @Test
    void matchesGlobalAndTeamReadersOnlyThroughCurrentInstanceSources() {
        assertEquals(List.of(ORIGINAL), optionIds());
        jdbc.update("""
                insert into process_instance_access_controls (id, source_team_id, target_process_instance_id, permissions)
                values (-1, -1, ?, array[?])
                """, INSTANCE_ID, PROCESS_INSTANCE_READ);
        // The original also belongs to this team. The global reader has a direct membership but no team role.
        assertEquals(List.of(ORIGINAL, GLOBAL, "assignment-team"), optionIds());

        jdbc.update("delete from process_instance_access_controls where id = -1");
        assertTrue(instances.findManualInstanceAssignmentOptions(INSTANCE_ID, PROCESS_INSTANCE_READ, GLOBAL).isEmpty());
        assertEquals(List.of(ORIGINAL), optionIds());
    }

    private List<String> optionIds() {
        return instances.findManualInstanceAssignmentOptions(INSTANCE_ID, PROCESS_INSTANCE_READ, null).stream()
                .map(ProcessAssignmentOptionDTO::id).toList();
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
