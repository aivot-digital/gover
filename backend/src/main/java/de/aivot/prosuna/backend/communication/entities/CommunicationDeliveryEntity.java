package de.aivot.prosuna.backend.communication.entities;

import de.aivot.prosuna.backend.communication.models.CommunicationDeliveryStatus;
import de.aivot.prosuna.backend.communication.converters.CommunicationWorkConverter;
import de.aivot.prosuna.backend.core.converters.AuthoredElementValuesConverter;
import de.aivot.prosuna.backend.core.converters.JsonObjectConverter;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "communication_deliveries")
public class CommunicationDeliveryEntity {
    @Nullable
    @Column(columnDefinition = "jsonb")
    @Convert(converter = CommunicationWorkConverter.class)
    private Map<String, Object> nextWork;

    @Nullable
    private Long nextTaskId;

    @Nullable
    public Map<String, Object> getNextWork() { return nextWork; }
    public void setNextWork(@Nullable Map<String, Object> nextWork) { this.nextWork = nextWork; }
    @Nullable
    public Long getNextTaskId() { return nextTaskId; }
    public void setNextTaskId(@Nullable Long nextTaskId) { this.nextTaskId = nextTaskId; }

    @Id
    @Nonnull
    private UUID id;

    @Nullable
    private Integer providerId;

    @Nullable
    private Long processInstanceId;

    @Nullable
    private Long taskId;

    @Nonnull
    private String definitionKey;

    @Nonnull
    private Integer definitionVersion;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = AuthoredElementValuesConverter.class)
    @Nonnull
    private AuthoredElementValues configuration;

    @Enumerated(EnumType.STRING)
    @Nonnull
    private CommunicationDeliveryStatus status;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonObjectConverter.class)
    @Nonnull
    private Map<String, Object> receipt;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonObjectConverter.class)
    @Nonnull
    private Map<String, Object> continuation;

    @Nonnull
    private Instant created;

    @Nonnull
    private Instant updated;

    @Nullable
    private Instant nextCheckAt;

    @Nonnull
    private Integer checkFailures;

    @Nullable
    private String statusMessage;

    @Nonnull
    private boolean continuationApplied;

    @Nonnull
    private boolean overdueNotified;

    @Nonnull
    public UUID getId() { return id; }

    @Nonnull
    public CommunicationDeliveryEntity setId(@Nonnull UUID id) {
        this.id = id;
        return this;
    }

    @Nullable
    public Integer getProviderId() { return providerId; }

    @Nonnull
    public CommunicationDeliveryEntity setProviderId(@Nullable Integer providerId) {
        this.providerId = providerId;
        return this;
    }

    @Nullable
    public Long getProcessInstanceId() { return processInstanceId; }

    @Nonnull
    public CommunicationDeliveryEntity setProcessInstanceId(@Nullable Long processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    @Nullable
    public Long getTaskId() { return taskId; }

    @Nonnull
    public CommunicationDeliveryEntity setTaskId(@Nullable Long taskId) {
        this.taskId = taskId;
        return this;
    }

    @Nonnull
    public String getDefinitionKey() { return definitionKey; }

    @Nonnull
    public CommunicationDeliveryEntity setDefinitionKey(@Nonnull String definitionKey) {
        this.definitionKey = definitionKey;
        return this;
    }

    @Nonnull
    public Integer getDefinitionVersion() { return definitionVersion; }

    @Nonnull
    public CommunicationDeliveryEntity setDefinitionVersion(@Nonnull Integer definitionVersion) {
        this.definitionVersion = definitionVersion;
        return this;
    }

    @Nonnull
    public AuthoredElementValues getConfiguration() { return configuration; }

    @Nonnull
    public CommunicationDeliveryEntity setConfiguration(@Nonnull AuthoredElementValues configuration) {
        this.configuration = configuration;
        return this;
    }

    @Nonnull
    public CommunicationDeliveryStatus getStatus() { return status; }

    @Nonnull
    public CommunicationDeliveryEntity setStatus(@Nonnull CommunicationDeliveryStatus status) {
        this.status = status;
        return this;
    }

    @Nonnull
    public Map<String, Object> getReceipt() { return receipt; }

    @Nonnull
    public CommunicationDeliveryEntity setReceipt(@Nonnull Map<String, Object> receipt) {
        this.receipt = receipt;
        return this;
    }

    @Nonnull
    public Map<String, Object> getContinuation() { return continuation; }

    @Nonnull
    public CommunicationDeliveryEntity setContinuation(@Nonnull Map<String, Object> continuation) {
        this.continuation = continuation;
        return this;
    }

    @Nonnull
    public Instant getCreated() { return created; }

    @Nonnull
    public CommunicationDeliveryEntity setCreated(@Nonnull Instant created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public Instant getUpdated() { return updated; }

    @Nonnull
    public CommunicationDeliveryEntity setUpdated(@Nonnull Instant updated) {
        this.updated = updated;
        return this;
    }

    @Nullable
    public Instant getNextCheckAt() { return nextCheckAt; }

    @Nonnull
    public CommunicationDeliveryEntity setNextCheckAt(@Nullable Instant nextCheckAt) {
        this.nextCheckAt = nextCheckAt;
        return this;
    }

    @Nonnull
    public Integer getCheckFailures() { return checkFailures; }

    @Nonnull
    public CommunicationDeliveryEntity setCheckFailures(@Nonnull Integer checkFailures) {
        this.checkFailures = checkFailures;
        return this;
    }

    @Nullable
    public String getStatusMessage() { return statusMessage; }

    @Nonnull
    public CommunicationDeliveryEntity setStatusMessage(@Nullable String statusMessage) {
        this.statusMessage = statusMessage;
        return this;
    }

    @Nonnull
    public boolean getContinuationApplied() { return continuationApplied; }

    @Nonnull
    public CommunicationDeliveryEntity setContinuationApplied(@Nonnull boolean continuationApplied) {
        this.continuationApplied = continuationApplied;
        return this;
    }

    @Nonnull
    public boolean getOverdueNotified() { return overdueNotified; }

    @Nonnull
    public CommunicationDeliveryEntity setOverdueNotified(@Nonnull boolean overdueNotified) {
        this.overdueNotified = overdueNotified;
        return this;
    }

}
