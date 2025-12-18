package de.aivot.GoverBackend.userRoles.entities;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "v_user_system_permission")
public class VUserSystemPermissionEntity {
    @Id
    @Nonnull
    private String userId;

    @Nonnull
    private List<String> permissions;

    // region Getters and Setters

    @Nonnull
    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(@Nonnull List<String> permissions) {
        this.permissions = permissions;
    }

    @Nonnull
    public String getUserId() {
        return userId;
    }

    public void setUserId(@Nonnull String userId) {
        this.userId = userId;
    }

    // endregion
}
