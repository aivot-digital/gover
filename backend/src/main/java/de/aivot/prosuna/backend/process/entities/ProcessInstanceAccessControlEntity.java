package de.aivot.prosuna.backend.process.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;
import de.aivot.prosuna.backend.lib.models.LockVersioned;

@Entity
@Table(name = "process_instance_access_controls")
public class ProcessInstanceAccessControlEntity implements LockVersioned {
    private static final String ID_SEQUENCE_NAME = "process_instance_access_controls_id_seq";

    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_SEQUENCE_NAME)
    @SequenceGenerator(name = ID_SEQUENCE_NAME, allocationSize = 1)
    private Integer id;

    @Nullable
    private Integer sourceTeamId;

    @Nullable
    private Integer sourceDepartmentId;

    @Nonnull
    private Long targetProcessInstanceId;

    @Nonnull
    private List<String> permissions;

    @Nonnull
    private Instant created;

    @Nonnull
    private Instant updated;

    @Nullable
    @Version
    @Column(nullable = false)
    private Long lockVersion;

    // region Signales

    @PrePersist
    public void prePersist() {
        created = Instant.now();
        updated = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updated = Instant.now();
    }

    // endregion

    // region Getters and Setters

    @Nonnull
    public Integer getId() {
        return id;
    }

    public ProcessInstanceAccessControlEntity setId(@Nonnull Integer id) {
        this.id = id;
        return this;
    }

    @Nullable
    public Integer getSourceTeamId() {
        return sourceTeamId;
    }

    public ProcessInstanceAccessControlEntity setSourceTeamId(@Nullable Integer sourceTeamId) {
        this.sourceTeamId = sourceTeamId;
        return this;
    }

    @Nullable
    public Integer getSourceDepartmentId() {
        return sourceDepartmentId;
    }

    public ProcessInstanceAccessControlEntity setSourceDepartmentId(@Nullable Integer sourceDepartmentId) {
        this.sourceDepartmentId = sourceDepartmentId;
        return this;
    }

    @Nullable
    public Long getTargetProcessInstanceId() {
        return targetProcessInstanceId;
    }

    public ProcessInstanceAccessControlEntity setTargetProcessInstanceId(@Nullable Long targetProcessInstanceId) {
        this.targetProcessInstanceId = targetProcessInstanceId;
        return this;
    }

    @Nonnull
    public List<String> getPermissions() {
        return permissions;
    }

    public ProcessInstanceAccessControlEntity setPermissions(@Nonnull List<String> permissions) {
        this.permissions = permissions;
        return this;
    }

    @Nonnull
    public Instant getCreated() {
        return created;
    }

    public ProcessInstanceAccessControlEntity setCreated(@Nonnull Instant created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public Instant getUpdated() {
        return updated;
    }

    public ProcessInstanceAccessControlEntity setUpdated(@Nonnull Instant updated) {
        this.updated = updated;
        return this;
    }

    @Nullable
    @Override
    public Long getLockVersion() {
        return lockVersion;
    }

    public ProcessInstanceAccessControlEntity setLockVersion(@Nullable Long lockVersion) {
        this.lockVersion = lockVersion;
        return this;
    }

    // endregion
}
