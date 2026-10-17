package de.aivot.prosuna.backend.permissions.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    /**
     * Providers can be domain-role assignable as a group while excluding individual permissions
     * whose checks are intentionally system-wide.
     */
    @JsonProperty("excludedFromDomainRoleAssignment")
    default Set<String> getExcludedFromDomainRoleAssignment() {
        return Set.of();
    }

    @Nullable
    @JsonProperty("domainRoleAssignmentHint")
    default String getDomainRoleAssignmentHint() {
        return null;
    }

    @Nullable
    @JsonProperty("systemRoleAssignmentHint")
    default String getSystemRoleAssignmentHint() {
        return null;
    }

    @Nonnull
    @JsonIgnore
    default Optional<SearchPermission> getSearchPermission() {
        return Optional.empty();
    }

    @Nonnull
    @JsonIgnore
    default List<SearchPermission> getSearchPermissions() {
        return getSearchPermission()
                .map(searchPermission -> List.of(searchPermission))
                .orElseGet(() -> List.of());
    }

    record SearchPermission(
            @Nonnull
            String originTable,
            @Nonnull
            String searchPermission
    ) {
    }
}
