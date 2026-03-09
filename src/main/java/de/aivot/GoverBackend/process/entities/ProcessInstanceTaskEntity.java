package de.aivot.GoverBackend.process.entities;

import de.aivot.GoverBackend.core.converters.JsonObjectConverter;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
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
    private Integer previousProcessNodeId;

    @Nonnull
    @NotNull(message = "Der Aufgaben-Status darf nicht null sein.")
    @Column(columnDefinition = "int2")
    private ProcessTaskStatus status;

    @Nullable
    @Size(max = 96, message = "Die Aufgaben-Status-Überschreibung darf maximal 96 Zeichen lang sein.")
    private String statusOverride;

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
    @Size(max = 36, message = "Die zugewiesene Benutzer-ID darf maximal 36 Zeichen lang sein.")
    private String assignedUserId;

    @Nullable
    private LocalDateTime deadline;

    @Nullable
    private LocalDateTime postponedUntil;

    @Nullable
    private Integer retryCount;

    @Nullable
    private LocalDateTime nextRetryAt;

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
                                     @Nullable Integer previousProcessNodeId,
                                     @Nonnull ProcessTaskStatus status,
                                     @Nullable String statusOverride,
                                     @Nonnull LocalDateTime started,
                                     @Nonnull LocalDateTime updated,
                                     @Nullable LocalDateTime finished,
                                     @Nullable Duration runtime,
                                     @Nonnull Map<String, Object> runtimeData,
                                     @Nonnull Map<String, Object> nodeData,
                                     @Nonnull Map<String, Object> processData,
                                     @Nullable String assignedUserId,
                                     @Nullable LocalDateTime deadline,
                                     @Nullable LocalDateTime postponedUntil,
                                     @Nullable Integer retryCount,
                                     @Nullable LocalDateTime nextRetryAt) {
        this.id = id;
        this.accessKey = accessKey;
        this.processInstanceId = processInstanceId;
        this.processId = processId;
        this.processVersion = processVersion;
        this.processNodeId = processNodeId;
        this.previousProcessNodeId = previousProcessNodeId;
        this.status = status;
        this.statusOverride = statusOverride;
        this.started = started;
        this.updated = updated;
        this.finished = finished;
        this.runtime = runtime;
        this.runtimeData = runtimeData;
        this.nodeData = nodeData;
        this.processData = processData;
        this.assignedUserId = assignedUserId;
        this.deadline = deadline;
        this.postponedUntil = postponedUntil;
        this.retryCount = retryCount;
        this.nextRetryAt = nextRetryAt;
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
    public Integer getPreviousProcessNodeId() {
        return previousProcessNodeId;
    }

    public ProcessInstanceTaskEntity setPreviousProcessNodeId(@Nullable Integer previousProcessDefinitionNodeId) {
        this.previousProcessNodeId = previousProcessDefinitionNodeId;
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
    public LocalDateTime getStarted() {
        return started;
    }

    public ProcessInstanceTaskEntity setStarted(@Nonnull LocalDateTime started) {
        this.started = started;
        return this;
    }

    @Nonnull
    public LocalDateTime getUpdated() {
        return updated;
    }

    public ProcessInstanceTaskEntity setUpdated(@Nonnull LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    @Nullable
    public LocalDateTime getFinished() {
        return finished;
    }

    public ProcessInstanceTaskEntity setFinished(@Nullable LocalDateTime finished) {
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
    public String getAssignedUserId() {
        return assignedUserId;
    }

    public ProcessInstanceTaskEntity setAssignedUserId(@Nullable String assignedUserId) {
        this.assignedUserId = assignedUserId;
        return this;
    }

    @Nullable
    public LocalDateTime getDeadline() {
        return deadline;
    }

    public ProcessInstanceTaskEntity setDeadline(@Nullable LocalDateTime deadline) {
        this.deadline = deadline;
        return this;
    }

    @Nullable
    public LocalDateTime getPostponedUntil() {
        return postponedUntil;
    }

    public ProcessInstanceTaskEntity setPostponedUntil(@Nullable LocalDateTime postponedUntil) {
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
    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public ProcessInstanceTaskEntity setNextRetryAt(@Nullable LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
        return this;
    }

    // endregion
}