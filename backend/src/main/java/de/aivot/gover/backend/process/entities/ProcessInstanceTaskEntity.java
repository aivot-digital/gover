package de.aivot.gover.backend.process.entities;

import de.aivot.gover.backend.core.converters.JsonObjectConverter;
import de.aivot.gover.backend.process.enums.ProcessTaskStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "process_instance_tasks")
public class ProcessInstanceTaskEntity {
    private static final String ID_SEQUENCE_NAME = "process_instance_tasks_id_seq";

    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_SEQUENCE_NAME)
    @SequenceGenerator(name = ID_SEQUENCE_NAME, allocationSize = 1)
    private Long id;

    @Nonnull
    @NotNull(message = "Der Zugriffsschlüssel darf nicht null sein.")
    private UUID accessKey;

    @Nonnull
    @NotNull(message = "Die Prozessinstanz-ID darf nicht null sein.")
    private Long processInstanceId;

    @Nonnull
    @NotNull(message = "Die Prozessdefinitions-ID darf nicht null sein.")
    private Integer processId;

    @Nonnull
    @NotNull(message = "Die Prozessdefinitions-Version darf nicht null sein.")
    private Integer processVersion;

    @Nonnull
    @NotNull(message = "Die Prozessdefinitions-Knoten-ID darf nicht null sein.")
    private Integer processNodeId;

    @Nullable
    private Long previousProcessInstanceTaskId;

    @Nullable
    private Integer previousProcessNodeId;

    @Nullable
    @Size(max = 96, message = "Der Schlüssel des vorherigen Prozessknoten-Ports darf maximal 96 Zeichen lang sein.")
    private String previousProcessNodePortKey;

    @Nonnull
    @NotNull(message = "Der Aufgaben-Status darf nicht null sein.")
    @Column(columnDefinition = "int2")
    private ProcessTaskStatus status;

    @Nullable
    @Size(max = 96, message = "Die Aufgaben-Status-Überschreibung darf maximal 96 Zeichen lang sein.")
    private String statusOverride;

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
    @NotNull(message = "Die Laufzeitdaten dürfen nicht null sein.")
    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonObjectConverter.class)
    private Map<String, Object> runtimeData;

    @Nonnull
    @NotNull(message = "Die Elementdaten dürfen nicht null sein.")
    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonObjectConverter.class)
    private Map<String, Object> nodeData;

    @Nonnull
    @NotNull(message = "Die Vorgangsdaten dürfen nicht null sein.")
    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonObjectConverter.class)
    private Map<String, Object> processData;

    @Nullable
    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonObjectConverter.class)
    private Map<String, Object> processDataDiff;

    @Nullable
    @Size(max = 36, message = "Die zugewiesene Benutzer-ID darf maximal 36 Zeichen lang sein.")
    private String assignedUserId;

    @Nullable
    @Column(columnDefinition = "timestamp with time zone")
    private Instant deadline;

    @Nullable
    @Column(columnDefinition = "timestamp with time zone")
    private Instant postponedUntil;

    @Nullable
    private Integer retryCount;

    @Nullable
    @Column(columnDefinition = "timestamp with time zone")
    private Instant nextRetryAt;

    // region Constructors

    // Empty constructor for JPA
    public ProcessInstanceTaskEntity() {

    }

    public ProcessInstanceTaskEntity(@Nonnull Long id,
                                     @Nonnull UUID accessKey,
                                     @Nonnull Long processInstanceId,
                                     @Nonnull Integer processId,
                                     @Nonnull Integer processVersion,
                                     @Nonnull Integer processNodeId,
                                     @Nullable Long previousProcessInstanceTaskId,
                                     @Nullable Integer previousProcessNodeId,
                                     @Nullable String previousProcessNodePortKey,
                                     @Nonnull ProcessTaskStatus status,
                                     @Nullable String statusOverride,
                                     @Nonnull Instant started,
                                     @Nonnull Instant updated,
                                     @Nullable Instant finished,
                                     @Nullable Duration runtime,
                                     @Nonnull Map<String, Object> runtimeData,
                                     @Nonnull Map<String, Object> nodeData,
                                     @Nonnull Map<String, Object> processData,
                                     @Nonnull Map<String, Object> processDataDiff,
                                     @Nullable String assignedUserId,
                                     @Nullable Instant deadline,
                                     @Nullable Instant postponedUntil,
                                     @Nullable Integer retryCount,
                                     @Nullable Instant nextRetryAt) {
        this.id = id;
        this.accessKey = accessKey;
        this.processInstanceId = processInstanceId;
        this.processId = processId;
        this.processVersion = processVersion;
        this.processNodeId = processNodeId;
        this.previousProcessInstanceTaskId = previousProcessInstanceTaskId;
        this.previousProcessNodeId = previousProcessNodeId;
        this.previousProcessNodePortKey = previousProcessNodePortKey;
        this.status = status;
        this.statusOverride = statusOverride;
        this.started = started;
        this.updated = updated;
        this.finished = finished;
        this.runtime = runtime;
        this.runtimeData = runtimeData;
        this.nodeData = nodeData;
        this.processData = processData;
        this.processDataDiff = processDataDiff;
        this.assignedUserId = assignedUserId;
        this.deadline = deadline;
        this.postponedUntil = postponedUntil;
        this.retryCount = retryCount;
        this.nextRetryAt = nextRetryAt;
    }

    // endregion

    // region HashCode & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ProcessInstanceTaskEntity that = (ProcessInstanceTaskEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(accessKey, that.accessKey) && Objects.equals(processInstanceId, that.processInstanceId) &&
                Objects.equals(processId, that.processId) && Objects.equals(processVersion, that.processVersion) &&
                Objects.equals(processNodeId, that.processNodeId) && Objects.equals(previousProcessInstanceTaskId, that.previousProcessInstanceTaskId) &&
                Objects.equals(previousProcessNodeId, that.previousProcessNodeId) && Objects.equals(previousProcessNodePortKey, that.previousProcessNodePortKey) &&
                status == that.status && Objects.equals(statusOverride, that.statusOverride) && Objects.equals(started, that.started) &&
                Objects.equals(updated, that.updated) && Objects.equals(finished, that.finished) && Objects.equals(runtime, that.runtime) &&
                Objects.equals(runtimeData, that.runtimeData) && Objects.equals(nodeData, that.nodeData) &&
                Objects.equals(processData, that.processData) && Objects.equals(processDataDiff, that.processDataDiff) &&
                Objects.equals(assignedUserId, that.assignedUserId) && Objects.equals(deadline, that.deadline) &&
                Objects.equals(postponedUntil, that.postponedUntil) && Objects.equals(retryCount, that.retryCount) &&
                Objects.equals(nextRetryAt, that.nextRetryAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, accessKey, processInstanceId, processId, processVersion, processNodeId, previousProcessInstanceTaskId, previousProcessNodeId, previousProcessNodePortKey, status, statusOverride, started, updated, finished, runtime, runtimeData, nodeData, processData, processDataDiff, assignedUserId, deadline, postponedUntil, retryCount, nextRetryAt);
    }

    // endregion

    // region Getters and Setters

    @Nonnull
    public Long getProcessInstanceId() {
        return processInstanceId;
    }

    public ProcessInstanceTaskEntity setProcessInstanceId(@Nonnull Long processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    @Nonnull
    public Long getId() {
        return id;
    }

    public ProcessInstanceTaskEntity setId(@Nonnull Long id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public UUID getAccessKey() {
        return accessKey;
    }

    public ProcessInstanceTaskEntity setAccessKey(@Nonnull UUID accessKey) {
        this.accessKey = accessKey;
        return this;
    }

    @Nonnull
    public Integer getProcessId() {
        return processId;
    }

    public ProcessInstanceTaskEntity setProcessId(@Nonnull Integer processDefinitionId) {
        this.processId = processDefinitionId;
        return this;
    }

    @Nonnull
    public Integer getProcessVersion() {
        return processVersion;
    }

    public ProcessInstanceTaskEntity setProcessVersion(@Nonnull Integer processDefinitionVersion) {
        this.processVersion = processDefinitionVersion;
        return this;
    }

    @Nonnull
    public Integer getProcessNodeId() {
        return processNodeId;
    }

    public ProcessInstanceTaskEntity setProcessNodeId(@Nonnull Integer processDefinitionNodeId) {
        this.processNodeId = processDefinitionNodeId;
        return this;
    }

    @Nullable
    public Long getPreviousProcessInstanceTaskId() {
        return previousProcessInstanceTaskId;
    }

    public ProcessInstanceTaskEntity setPreviousProcessInstanceTaskId(@Nullable Long previousProcessInstanceTaskId) {
        this.previousProcessInstanceTaskId = previousProcessInstanceTaskId;
        return this;
    }

    @Nullable
    public Integer getPreviousProcessNodeId() {
        return previousProcessNodeId;
    }

    public ProcessInstanceTaskEntity setPreviousProcessNodeId(@Nullable Integer previousProcessDefinitionNodeId) {
        this.previousProcessNodeId = previousProcessDefinitionNodeId;
        return this;
    }

    @Nullable
    public String getPreviousProcessNodePortKey() {
        return previousProcessNodePortKey;
    }

    public ProcessInstanceTaskEntity setPreviousProcessNodePortKey(@Nullable String previousProcessNodePortKey) {
        this.previousProcessNodePortKey = previousProcessNodePortKey;
        return this;
    }

    @Nonnull
    public ProcessTaskStatus getStatus() {
        return status;
    }

    public ProcessInstanceTaskEntity setStatus(@Nonnull ProcessTaskStatus status) {
        this.status = status;
        return this;
    }

    @Nullable
    public String getStatusOverride() {
        return statusOverride;
    }

    public ProcessInstanceTaskEntity setStatusOverride(@Nullable String statusOverride) {
        this.statusOverride = statusOverride;
        return this;
    }

    @Nonnull
    public Instant getStarted() {
        return started;
    }

    public ProcessInstanceTaskEntity setStarted(@Nonnull Instant started) {
        this.started = started;
        return this;
    }

    @Nonnull
    public Instant getUpdated() {
        return updated;
    }

    public ProcessInstanceTaskEntity setUpdated(@Nonnull Instant updated) {
        this.updated = updated;
        return this;
    }

    @Nullable
    public Instant getFinished() {
        return finished;
    }

    public ProcessInstanceTaskEntity setFinished(@Nullable Instant finished) {
        this.finished = finished;
        return this;
    }

    @Nullable
    public Duration getRuntime() {
        return runtime;
    }

    public ProcessInstanceTaskEntity setRuntime(@Nullable Duration runtime) {
        this.runtime = runtime;
        return this;
    }

    @Nonnull
    public Map<String, Object> getRuntimeData() {
        return runtimeData;
    }

    public ProcessInstanceTaskEntity setRuntimeData(@Nonnull Map<String, Object> runtimeData) {
        this.runtimeData = runtimeData;
        return this;
    }

    @Nonnull
    public Map<String, Object> getNodeData() {
        return nodeData;
    }

    public ProcessInstanceTaskEntity setNodeData(@Nonnull Map<String, Object> nodeData) {
        this.nodeData = nodeData;
        return this;
    }

    @Nonnull
    public Map<String, Object> getProcessData() {
        return processData;
    }

    public ProcessInstanceTaskEntity setProcessData(@Nonnull Map<String, Object> processData) {
        this.processData = processData;
        return this;
    }

    @Nullable
    public Map<String, Object> getProcessDataDiff() {
        return processDataDiff;
    }

    public ProcessInstanceTaskEntity setProcessDataDiff(@Nullable Map<String, Object> processDataDiff) {
        this.processDataDiff = processDataDiff;
        return this;
    }

    @Nullable
    public String getAssignedUserId() {
        return assignedUserId;
    }

    public ProcessInstanceTaskEntity setAssignedUserId(@Nullable String assignedUserId) {
        this.assignedUserId = assignedUserId;
        return this;
    }

    @Nullable
    public Instant getDeadline() {
        return deadline;
    }

    public ProcessInstanceTaskEntity setDeadline(@Nullable Instant deadline) {
        this.deadline = deadline;
        return this;
    }

    @Nullable
    public Instant getPostponedUntil() {
        return postponedUntil;
    }

    public ProcessInstanceTaskEntity setPostponedUntil(@Nullable Instant postponedUntil) {
        this.postponedUntil = postponedUntil;
        return this;
    }

    @Nullable
    public Integer getRetryCount() {
        return retryCount;
    }

    public ProcessInstanceTaskEntity setRetryCount(@Nullable Integer retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    @Nullable
    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public ProcessInstanceTaskEntity setNextRetryAt(@Nullable Instant nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
        return this;
    }

    // endregion
}
