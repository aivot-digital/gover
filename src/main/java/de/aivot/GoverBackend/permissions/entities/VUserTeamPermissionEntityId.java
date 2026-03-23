package de.aivot.GoverBackend.permissions.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Id;

import java.util.Objects;

public class VUserTeamPermissionEntityId {
    @Id
    @Nonnull
    private String userId;

    @Id
    @Nonnull
    private Integer teamId;

    public VUserTeamPermissionEntityId() {
        userId = "";
        teamId = 0;
    }

    public VUserTeamPermissionEntityId(@Nonnull String userId,
                                       @Nonnull Integer teamId) {
        this.userId = userId;
        this.teamId = teamId;
    }

    public static VUserTeamPermissionEntityId of(@Nonnull String userId,
                                                 @Nonnull Integer teamId) {
        return new VUserTeamPermissionEntityId(userId, teamId);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        VUserTeamPermissionEntityId that = (VUserTeamPermissionEntityId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(teamId, that.teamId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, teamId);
    }

    @Nonnull
    public String getUserId() {
        return userId;
    }

    public VUserTeamPermissionEntityId setUserId(@Nonnull String userId) {
        this.userId = userId;
        return this;
    }

    @Nonnull
    public Integer getTeamId() {
        return teamId;
    }

    public VUserTeamPermissionEntityId setTeamId(@Nonnull Integer teamId) {
        this.teamId = teamId;
        return this;
    }
}
