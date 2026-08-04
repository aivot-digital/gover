package de.aivot.gover.backend.permissions.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.gover.backend.department.permissions.DepartmentPermissionProvider;
import de.aivot.gover.backend.permissions.permissions.PermissionSetPermissionProvider;
import de.aivot.gover.backend.teams.permissions.TeamPermissionProvider;
import org.junit.jupiter.api.Test;

import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionProviderTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

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
