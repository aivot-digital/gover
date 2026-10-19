package de.aivot.prosuna.backend.permissions.models;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.department.permissions.DepartmentPermissionProvider;
import de.aivot.prosuna.backend.permissions.permissions.PermissionSetPermissionProvider;
import de.aivot.prosuna.backend.teams.permissions.TeamPermissionProvider;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.filter.AssignableTypeFilter;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

class PermissionProviderTest {
    private final JsonMapper objectMapper = JsonMapperTestUtils.createMapper();

    @Test
    void shouldSeedEveryDeclaredPermissionInTheDefaultSuperadministratorRole() throws Exception {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(PermissionProvider.class));
        var declaredPermissions = new TreeSet<String>();
        for (var candidate : scanner.findCandidateComponents("de.aivot.prosuna.backend")) {
            var provider = (PermissionProvider) Class.forName(candidate.getBeanClassName()).getDeclaredConstructor().newInstance();
            Arrays.stream(provider.getPermissions()).map(PermissionEntry::permission).forEach(declaredPermissions::add);
        }
        assertFalse(declaredPermissions.isEmpty());

        // Check the explicit seed, not role names or runtime grants: new providers must update this default as well.
        var seed = new ClassPathResource("db/migration/V17_1_0__default_system_roles.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        var role = Pattern.compile("(?s)values\\s*\\(1,.*?array\\s*\\[(.*?)]").matcher(seed);
        assertTrue(role.find(), "Default role 1 must have an explicit permission array.");
        var grants = Pattern.compile("'([^']+)'").matcher(role.group(1)).results()
                .map(match -> match.group(1)).toList();
        assertEquals(grants.size(), new TreeSet<>(grants).size(), "Default role grants must not contain duplicates.");
        assertEquals(declaredPermissions, new TreeSet<>(grants), "Keep the default full-access role aligned with the providers.");
    }

    @Test
    void shouldSerializeDomainRoleAssignmentSupportForOptedInProvider() throws Exception {
        var json = objectMapper.valueToTree(new DepartmentPermissionProvider());

        assertTrue(json.get("supportsDomainRoleAssignment").asBoolean());
        assertTrue(StreamSupport
                .stream(json.get("excludedFromDomainRoleAssignment").spliterator(), false)
                .anyMatch(node -> DepartmentPermissionProvider.DEPARTMENT_CREATE.equals(node.asText())));
        assertNotNull(json.get("domainRoleAssignmentHint"));
        assertTrue(json.has("systemRoleAssignmentHint"));
    }

    @Test
    void shouldSerializeDomainRoleAssignmentExclusionsForTeamProvider() {
        var json = objectMapper.valueToTree(new TeamPermissionProvider());

        assertTrue(json.get("supportsDomainRoleAssignment").asBoolean());
        assertTrue(StreamSupport
                .stream(json.get("excludedFromDomainRoleAssignment").spliterator(), false)
                .anyMatch(node -> TeamPermissionProvider.TEAM_CREATE.equals(node.asText())));
        assertNotNull(json.get("domainRoleAssignmentHint"));
        assertTrue(json.has("systemRoleAssignmentHint"));
    }

    @Test
    void shouldSerializeDomainRoleAssignmentSupportForDefaultProvider() throws Exception {
        var json = objectMapper.valueToTree(new PermissionSetPermissionProvider());

        assertFalse(json.get("supportsDomainRoleAssignment").asBoolean());
        assertTrue(json.get("excludedFromDomainRoleAssignment").isEmpty());
        assertTrue(json.get("domainRoleAssignmentHint").isNull());
        assertTrue(json.get("systemRoleAssignmentHint").isNull());
    }
}
