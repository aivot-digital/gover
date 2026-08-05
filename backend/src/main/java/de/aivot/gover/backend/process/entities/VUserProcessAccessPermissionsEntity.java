package de.aivot.gover.backend.process.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.util.List;

@Entity
@IdClass(VUserProcessAccessPermissionsEntityId.class)
@Table(name = "v_user_process_access_permissions")
public class VUserProcessAccessPermissionsEntity {
    @Id
    @Nonnull
    private String userId;

    @Id
    @Nullable
    private Integer viaSourceTeamId;

    @Id
    @Nullable
    private Integer viaSourceDepartmentId;

    @Id
    @Nonnull
    private Integer targetProcessId;

    @Nonnull
    private List<String> permissions;

    @Nonnull
    public String getUserId() {
        return userId;
    }

    @Nullable
    public Integer getViaSourceTeamId() {
        return viaSourceTeamId;
    }

    @Nullable
    public Integer getViaSourceDepartmentId() {
        return viaSourceDepartmentId;
    }

    @Nonnull
    public Integer getTargetProcessId() {
        return targetProcessId;
    }

    @Nonnull
    public List<String> getPermissions() {
        return permissions;
    }
}
