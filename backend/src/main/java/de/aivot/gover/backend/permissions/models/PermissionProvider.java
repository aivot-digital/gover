package de.aivot.gover.backend.permissions.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface PermissionProvider {
    String getContextLabel();

    PermissionEntry[] getPermissions();

    /**
     * All permissions are assignable to system roles. Providers opt in here only when their
     * permissions may additionally be assigned through domain roles.
     */
    @JsonProperty("supportsDomainRoleAssignment")
    default boolean supportsDomainRoleAssignment() {
        return false;
    }
}
