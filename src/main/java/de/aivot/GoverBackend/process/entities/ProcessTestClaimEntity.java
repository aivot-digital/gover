package de.aivot.GoverBackend.process.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "process_test_claims")
public class ProcessTestClaimEntity {
    private static final String ID_SEQUENCE_NAME = "process_test_claims_id_seq";
    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_SEQUENCE_NAME)
    @SequenceGenerator(name = ID_SEQUENCE_NAME, allocationSize = 1)
    private Integer id;

    @Nonnull
    private String accessKey;

    @Nullable
    private Integer processTestConfigId;

    @Nonnull
    @NotNull(message = "Die ID der Prozessdefinition darf nicht null sein.")
    private Integer processId;

    @Nonnull
    @NotNull(message = "Die Version der Prozessdefinition darf nicht null sein.")
    private Integer processVersion;

    @Nonnull
    private LocalDateTime created;

    @Nonnull
    private String owningUserId;

    // region Constructors
    // Empty constructor for JPA
    public ProcessTestClaimEntity() {

    }

    // Full Constructor
    public ProcessTestClaimEntity(@Nonnull Integer id,
                                  @Nonnull String accessKey,
                                  @Nullable Integer processTestConfigId,
                                  @Nonnull Integer processId,
                                  @Nonnull Integer processVersion,
                                  @Nonnull LocalDateTime created,
                                  @Nonnull String owningUserId) {
        this.id = id;
        this.accessKey = accessKey;
        this.processTestConfigId = processTestConfigId;
        this.processId = processId;
        this.processVersion = processVersion;
        this.created = created;
        this.owningUserId = owningUserId;
    }
    // endregion

    // region Signals
    @PrePersist
    public void onCreate() {
        this.created = LocalDateTime.now();
    }
    // endregion

    // region Getters and Setters
    @Nonnull
    public Integer getId() {
        return id;
    }

    public ProcessTestClaimEntity setId(@Nonnull Integer id) {
        this.id = id;
        return this;
    }

    @Nullable
    public Integer getProcessTestConfigId() {
        return processTestConfigId;
    }

    public ProcessTestClaimEntity setProcessTestConfigId(@Nullable Integer processTestConfigId) {
        this.processTestConfigId = processTestConfigId;
        return this;
    }

    @Nonnull
    public Integer getProcessId() {
        return processId;
    }

    public ProcessTestClaimEntity setProcessId(@Nonnull Integer processId) {
        this.processId = processId;
        return this;
    }

    @Nonnull
    public Integer getProcessVersion() {
        return processVersion;
    }

    public ProcessTestClaimEntity setProcessVersion(@Nonnull Integer processVersion) {
        this.processVersion = processVersion;
        return this;
    }

    @Nonnull
    public LocalDateTime getCreated() {
        return created;
    }

    public ProcessTestClaimEntity setCreated(@Nonnull LocalDateTime created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public String getOwningUserId() {
        return owningUserId;
    }

    public ProcessTestClaimEntity setOwningUserId(@Nonnull String owningUserId) {
        this.owningUserId = owningUserId;
        return this;
    }

    @Nonnull
    public String getAccessKey() {
        return accessKey;
    }

    public ProcessTestClaimEntity setAccessKey(@Nonnull String accessKey) {
        this.accessKey = accessKey;
        return this;
    }
    // endregion
}
