package de.aivot.prosuna.backend.permissions.models;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.department.permissions.DepartmentPermissionProvider;
import de.aivot.prosuna.backend.permissions.permissions.PermissionSetPermissionProvider;
import de.aivot.prosuna.backend.teams.permissions.TeamPermissionProvider;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

class PermissionProviderTest {
    private final JsonMapper objectMapper = JsonMapperTestUtils.createMapper();

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
