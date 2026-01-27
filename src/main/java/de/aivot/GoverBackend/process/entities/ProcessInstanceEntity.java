package de.aivot.GoverBackend.process.entities;

import de.aivot.GoverBackend.core.converters.JsonObjectConverter;
import de.aivot.GoverBackend.process.converters.ProcessIdentityItemsConverter;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.process.models.ProcessIdentityItem;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "process_instances")
public class ProcessInstanceEntity {
    private static final String ID_SEQUENCE_NAME = "process_instances_id_seq";

    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_SEQUENCE_NAME)
    @SequenceGenerator(name = ID_SEQUENCE_NAME, allocationSize = 1)
    private Long id;

    @Nonnull
    @NotNull(message = "Der Zugriffsschlüssel darf nicht null sein.")
    private UUID accessKey;

    @Nonnull
    @NotNull(message = "Die Prozessdefinitions-ID darf nicht null sein.")
    private Integer processId;

    @Nonnull
    @NotNull(message = "Der Prozessinstanz-Status darf nicht null sein.")
    @Column(columnDefinition = "int2")
    private ProcessInstanceStatus status;

    @Nullable
    @Size(max = 96, message = "Die Status-Überschreibung darf maximal 96 Zeichen lang sein.")
    private String statusOverride;

    @Nullable
    @Size(min = 36, max = 36, message = "Die Benutzer-ID des Zuständigen muss 36 Zeichen lang sein.")
    private String assignedUserId;

    // Arrays and JSON fields can be mapped as String or custom types
    @Nonnull
    @NotNull(message = "Die zugewiesenen Aktenzeichen dürfen nicht null sein.")
    @Column(columnDefinition = "varchar(96)[]")
    private List<String> assignedFileNumbers;

    @Nonnull
    @NotNull(message = "Die Zustellkanalkonfigurationen dürfen nicht null sein.")
    @Convert(converter = ProcessIdentityItemsConverter.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, ProcessIdentityItem> identities;

    @Nonnull
    @NotNull(message = "Das Startdatum darf nicht null sein.")
    private LocalDateTime started;

    @Nonnull
    @NotNull(message = "Das Aktualisierungsdatum darf nicht null sein.")
    private LocalDateTime updated;

    @Nullable
    private LocalDateTime finished;

    @Nullable
    @Column(columnDefinition = "interval", insertable = false, updatable = false)
    private Duration runtime;

    @Nonnull
    @NotNull(message = "Die Initial-Payload darf nicht null sein.")
    @Convert(converter = JsonObjectConverter.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> initialPayload;

    @Nonnull
    @NotNull(message = "Die Initial-Knoten-ID darf nicht null sein.")
    private Integer initialNodeId;

    @Nullable
    private LocalDateTime keepUntil;

    @Nullable
    private Integer createdForTestClaimId;

    // region Constructors

    // Empty constructor for JPA
    public ProcessInstanceEntity() {
    }

    public ProcessInstanceEntity(@Nonnull Long id,
                                 @Nonnull UUID accessKey,
                                 @Nonnull Integer processId,
                                 @Nonnull ProcessInstanceStatus status,
                                 @Nullable String statusOverride,
                                 @Nullable String assignedUserId,
                                 @Nonnull List<String> assignedFileNumbers,
                                 @Nonnull Map<String, ProcessIdentityItem> identities,
                                 @Nonnull LocalDateTime started,
                                 @Nonnull LocalDateTime updated,
                                 @Nullable LocalDateTime finished,
                                 @Nullable Duration runtime,
                                 @Nonnull Map<String, Object> initialPayload,
                                 @Nonnull Integer initialNodeId,
                                 @Nullable LocalDateTime keepUntil,
                                 @Nullable Integer createdForTestClaimId) {
        this.id = id;
        this.accessKey = accessKey;
        this.processId = processId;
        this.status = status;
        this.statusOverride = statusOverride;
        this.assignedUserId = assignedUserId;
        this.assignedFileNumbers = assignedFileNumbers;
        this.identities = identities;
        this.started = started;
        this.updated = updated;
        this.finished = finished;
        this.runtime = runtime;
        this.initialPayload = initialPayload;
        this.initialNodeId = initialNodeId;
        this.keepUntil = keepUntil;
        this.createdForTestClaimId = createdForTestClaimId;
    }

    // endregion

    // region Hashcode and Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ProcessInstanceEntity instance = (ProcessInstanceEntity) o;
        return Objects.equals(id, instance.id) && Objects.equals(accessKey, instance.accessKey) && Objects.equals(processId, instance.processId) && status == instance.status && Objects.equals(statusOverride, instance.statusOverride) && Objects.equals(assignedUserId, instance.assignedUserId) && Objects.equals(assignedFileNumbers, instance.assignedFileNumbers) && Objects.equals(identities, instance.identities) && Objects.equals(started, instance.started) && Objects.equals(updated, instance.updated) && Objects.equals(finished, instance.finished) && Objects.equals(runtime, instance.runtime) && Objects.equals(initialPayload, instance.initialPayload) && Objects.equals(initialNodeId, instance.initialNodeId) && Objects.equals(keepUntil, instance.keepUntil) && Objects.equals(createdForTestClaimId, instance.createdForTestClaimId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, accessKey, processId, status, statusOverride, assignedUserId, assignedFileNumbers, identities, started, updated, finished, runtime, initialPayload, initialNodeId, keepUntil, createdForTestClaimId);
    }

    // endregion

    // region Getters and Setters

    @Nonnull
    public Long getId() {
        return id;
    }

    public ProcessInstanceEntity setId(@Nonnull Long id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public UUID getAccessKey() {
        return accessKey;
    }

    public ProcessInstanceEntity setAccessKey(@Nonnull UUID accessKey) {
        this.accessKey = accessKey;
        return this;
    }

    @Nonnull
    public Integer getProcessId() {
        return processId;
    }

    public ProcessInstanceEntity setProcessId(@Nonnull Integer processDefinitionId) {
        this.processId = processDefinitionId;
        return this;
    }

    @Nonnull
    public ProcessInstanceStatus getStatus() {
        return status;
    }

    public ProcessInstanceEntity setStatus(@Nonnull ProcessInstanceStatus status) {
        this.status = status;
        return this;
    }

    @Nullable
    public String getStatusOverride() {
        return statusOverride;
    }

    public ProcessInstanceEntity setStatusOverride(@Nullable String statusOverride) {
        this.statusOverride = statusOverride;
        return this;
    }

    @Nonnull
    public List<String> getAssignedFileNumbers() {
        return assignedFileNumbers;
    }

    public ProcessInstanceEntity setAssignedFileNumbers(@Nonnull List<String> assignedFileNumbers) {
        this.assignedFileNumbers = assignedFileNumbers;
        return this;
    }

    @Nonnull
    public Map<String, ProcessIdentityItem> getIdentities() {
        return identities;
    }

    public ProcessInstanceEntity setIdentities(@Nonnull Map<String, ProcessIdentityItem> identities) {
        this.identities = identities;
        return this;
    }

    @Nonnull
    public LocalDateTime getStarted() {
        return started;
    }

    public ProcessInstanceEntity setStarted(@Nonnull LocalDateTime started) {
        this.started = started;
        return this;
    }

    @Nonnull
    public LocalDateTime getUpdated() {
        return updated;
    }

    public ProcessInstanceEntity setUpdated(@Nonnull LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    @Nullable
    public LocalDateTime getFinished() {
        return finished;
    }

    public ProcessInstanceEntity setFinished(@Nullable LocalDateTime finished) {
        this.finished = finished;
        return this;
    }

    @Nullable
    public Duration getRuntime() {
        return runtime;
    }

    public ProcessInstanceEntity setRuntime(@Nullable Duration runtime) {
        this.runtime = runtime;
        return this;
    }

    @Nonnull
    public Map<String, Object> getInitialPayload() {
        return initialPayload;
    }

    public ProcessInstanceEntity setInitialPayload(@Nonnull Map<String, Object> initialPayload) {
        this.initialPayload = initialPayload;
        return this;
    }

    @Nonnull
    public Integer getInitialNodeId() {
        return initialNodeId;
    }

    public ProcessInstanceEntity setInitialNodeId(@Nonnull Integer initialNodeId) {
        this.initialNodeId = initialNodeId;
        return this;
    }

    @Nullable
    public String getAssignedUserId() {
        return assignedUserId;
    }

    public ProcessInstanceEntity setAssignedUserId(@Nullable String assigneeUserId) {
        this.assignedUserId = assigneeUserId;
        return this;
    }

    @Nullable
    public LocalDateTime getKeepUntil() {
        return keepUntil;
    }

    public ProcessInstanceEntity setKeepUntil(@Nullable LocalDateTime keepUntil) {
        this.keepUntil = keepUntil;
        return this;
    }

    @Nullable
    public Integer getCreatedForTestClaimId() {
        return createdForTestClaimId;
    }

    public ProcessInstanceEntity setCreatedForTestClaimId(@Nullable Integer createdForTestClaimId) {
        this.createdForTestClaimId = createdForTestClaimId;
        return this;
    }

    // endregion
}