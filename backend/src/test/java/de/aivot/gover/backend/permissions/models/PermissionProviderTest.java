package de.aivot.gover.backend.permissions.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.gover.backend.department.permissions.DepartmentPermissionProvider;
import de.aivot.gover.backend.permissions.permissions.PermissionSetPermissionProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionProviderTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerializeDomainRoleAssignmentSupportForOptedInProvider() throws Exception {
        var json = objectMapper.valueToTree(new DepartmentPermissionProvider());

        assertTrue(json.get("supportsDomainRoleAssignment").asBoolean());
    }

    @Test
    void shouldSerializeDomainRoleAssignmentSupportForDefaultProvider() throws Exception {
        var json = objectMapper.valueToTree(new PermissionSetPermissionProvider());

        assertFalse(json.get("supportsDomainRoleAssignment").asBoolean());
    }
}
