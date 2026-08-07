package de.aivot.prosuna.backend.process.entities;

import de.aivot.prosuna.backend.core.converters.JsonObjectConverter;
import de.aivot.prosuna.backend.identity.converters.IdentityDataMapConverter;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Duration;
import java.time.Instant;
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
    @NotNull(message = "Der Vorgangsschlüssel darf nicht null sein.")
    private String caseNumber;

    @Nonnull
    @NotNull(message = "Der Zugriffsschlüssel darf nicht null sein.")
    private UUID accessKey;

    @Nonnull
    @NotNull(message = "Die Prozessdefinitions-ID darf nicht null sein.")
    private Integer processId;

    @Nonnull
    @NotNull(message = "Die Initial-Prozessversion darf nicht null sein.")
    private Integer initialProcessVersion;

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
    @Convert(converter = IdentityDataMapConverter.class)
    @Column(columnDefinition = "jsonb")
    private IdentityDataMap identities;

    @Nonnull
    @NotNull(message = "Das Startdatum darf nicht null sein.")
    @Column(columnDefinition = "timestamp with time zone")
    private Instant started;

    @Nonnull
    @NotNull(message = "Das Aktualisierungsdatum darf nicht null sein.")
    @Column(columnDefinition = "timestamp with time zone")
    private Instant updated;

    @Nullable
    @Column(columnDefinition = "timestamp with time zone")
    private Instant finished;

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
    @Column(columnDefinition = "timestamp with time zone")
    private Instant keepUntil;

    @Nullable
    private Integer createdForTestClaimId;

    // region Constructors

    // Empty constructor for JPA
    public ProcessInstanceEntity() {
    }

    public ProcessInstanceEntity(@Nonnull Long id,
                                 @Nonnull String caseNumber,
                                 @Nonnull UUID accessKey,
                                 @Nonnull Integer processId,
                                 @Nonnull Integer initialProcessVersion,
                                 @Nonnull ProcessInstanceStatus status,
                                 @Nullable String statusOverride,
                                 @Nullable String assignedUserId,
                                 @Nonnull List<String> assignedFileNumbers,
                                 @Nonnull IdentityDataMap identities,
                                 @Nonnull Instant started,
                                 @Nonnull Instant updated,
                                 @Nullable Instant finished,
                                 @Nullable Duration runtime,
                                 @Nonnull Map<String, Object> initialPayload,
                                 @Nonnull Integer initialNodeId,
                                 @Nullable Instant keepUntil,
                                 @Nullable Integer createdForTestClaimId) {
        this.id = id;
        this.caseNumber = caseNumber;
        this.accessKey = accessKey;
        this.processId = processId;
        this.initialProcessVersion = initialProcessVersion;
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
        ProcessInstanceEntity that = (ProcessInstanceEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(caseNumber, that.caseNumber) && Objects.equals(accessKey, that.accessKey) &&
                Objects.equals(processId, that.processId) && Objects.equals(initialProcessVersion, that.initialProcessVersion) && status == that.status &&
                Objects.equals(statusOverride, that.statusOverride) && Objects.equals(assignedUserId, that.assignedUserId) &&
                Objects.equals(assignedFileNumbers, that.assignedFileNumbers) && Objects.equals(identities, that.identities) &&
                Objects.equals(started, that.started) && Objects.equals(updated, that.updated) && Objects.equals(finished, that.finished) &&
                Objects.equals(runtime, that.runtime) && Objects.equals(initialPayload, that.initialPayload) &&
                Objects.equals(initialNodeId, that.initialNodeId) && Objects.equals(keepUntil, that.keepUntil) &&
                Objects.equals(createdForTestClaimId, that.createdForTestClaimId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, caseNumber, accessKey, processId, initialProcessVersion, status, statusOverride, assignedUserId, assignedFileNumbers, identities, started, updated, finished, runtime, initialPayload, initialNodeId, keepUntil, createdForTestClaimId);
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
    public String getCaseNumber() {
        return caseNumber;
    }

    public ProcessInstanceEntity setCaseNumber(@Nonnull String caseNumber) {
        this.caseNumber = caseNumber;
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
    public Integer getInitialProcessVersion() {
        return initialProcessVersion;
    }

    public ProcessInstanceEntity setInitialProcessVersion(@Nonnull Integer initialProcessVersion) {
        this.initialProcessVersion = initialProcessVersion;
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
    public IdentityDataMap getIdentities() {
        return identities;
    }

    public ProcessInstanceEntity setIdentities(@Nonnull IdentityDataMap identities) {
        this.identities = identities;
        return this;
    }

    @Nonnull
    public Instant getStarted() {
        return started;
    }

    public ProcessInstanceEntity setStarted(@Nonnull Instant started) {
        this.started = started;
        return this;
    }

    @Nonnull
    public Instant getUpdated() {
        return updated;
    }

    public ProcessInstanceEntity setUpdated(@Nonnull Instant updated) {
        this.updated = updated;
        return this;
    }

    @Nullable
    public Instant getFinished() {
        return finished;
    }

    public ProcessInstanceEntity setFinished(@Nullable Instant finished) {
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
    public Instant getKeepUntil() {
        return keepUntil;
    }

    public ProcessInstanceEntity setKeepUntil(@Nullable Instant keepUntil) {
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
