package de.aivot.GoverBackend.audit.entities;

import de.aivot.GoverBackend.core.converters.JsonObjectConverter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {
    private static final String ID_SEQUENCE_NAME = "audit_logs_id_seq";

    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_SEQUENCE_NAME)
    @SequenceGenerator(name = ID_SEQUENCE_NAME, allocationSize = 1)
    private Long id;

    @Nonnull
    @NotNull(message = "Der Zeitstempel darf nicht null sein.")
    private LocalDateTime eventTs;

    @Nullable
    @Length(min = 36, max = 36, message = "Die Benutzer-ID muss genau 36 Zeichen lang sein.")
    private String triggeringUserId;

    @Nonnull
    @NotNull(message = "Der Akteur-Typ darf nicht null sein.")
    @Length(max = 32, message = "Der Akteur-Typ darf maximal 32 Zeichen lang sein.")
    private String actorType;

    @Nullable
    @Length(max = 255, message = "Die Akteur-ID darf maximal 255 Zeichen lang sein.")
    private String actorId;

    @Nullable
    @Length(max = 255, message = "Das Akteur-Label darf maximal 255 Zeichen lang sein.")
    private String actorLabel;

    @Nullable
    @Length(max = 32, message = "Der Trigger-Typ darf maximal 32 Zeichen lang sein.")
    private String triggerType;

    @Nullable
    @Length(max = 255, message = "Die Trigger-Referenz darf maximal 255 Zeichen lang sein.")
    private String triggerRef;

    @Nullable
    @Length(max = 128, message = "Der Service-Name darf maximal 128 Zeichen lang sein.")
    private String serviceName;

    @Nullable
    @Length(max = 128, message = "Die Instanz-ID darf maximal 128 Zeichen lang sein.")
    private String instanceId;

    @Nonnull
    @NotNull(message = "Der Aktionstyp darf nicht null sein.")
    @Length(max = 64, message = "Der Aktionstyp darf maximal 64 Zeichen lang sein.")
    private String actionType;

    @Nonnull
    @NotNull(message = "Die Komponente darf nicht null sein.")
    @Length(max = 255, message = "Die Komponente darf maximal 255 Zeichen lang sein.")
    private String component;

    @Nullable
    @Length(max = 255, message = "Der Entitätstyp darf maximal 255 Zeichen lang sein.")
    private String entityType;

    @Nullable
    @Length(max = 255, message = "Die Entitäts-ID darf maximal 255 Zeichen lang sein.")
    private String entityId;

    @Nonnull
    @NotNull(message = "Die Nachricht darf nicht null sein.")
    private String message;

    @Nonnull
    @NotNull(message = "ChangedData darf nicht null sein.")
    private Boolean changedData;

    @Nullable
    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonObjectConverter.class)
    private Map<String, Object> beforeData;

    @Nullable
    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonObjectConverter.class)
    private Map<String, Object> afterData;

    @Nullable
    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonObjectConverter.class)
    private Map<String, Object> dataDiff;

    @Nonnull
    @NotNull(message = "Das Ergebnis darf nicht null sein.")
    @Length(max = 32, message = "Das Ergebnis darf maximal 32 Zeichen lang sein.")
    private String actionResult;

    @Nullable
    @Length(max = 512, message = "Die Begründung darf maximal 512 Zeichen lang sein.")
    private String reason;

    @Nullable
    @Length(max = 32, message = "Die Quelle darf maximal 32 Zeichen lang sein.")
    private String source;

    @Nullable
    @Length(max = 128, message = "Die Request-ID darf maximal 128 Zeichen lang sein.")
    private String requestId;

    @Nullable
    @Length(max = 128, message = "Die Session-ID darf maximal 128 Zeichen lang sein.")
    private String sessionId;

    @Nullable
    @Length(max = 64, message = "Die IP-Adresse darf maximal 64 Zeichen lang sein.")
    private String ipAddress;

    @Nullable
    @Length(max = 1024, message = "Der User-Agent darf maximal 1024 Zeichen lang sein.")
    private String userAgent;

    @Nullable
    @Length(max = 32, message = "Die Schwere darf maximal 32 Zeichen lang sein.")
    private String severity;

    @Nonnull
    @NotNull(message = "Tags dürfen nicht null sein.")
    @Column(columnDefinition = "varchar(64)[]")
    private List<String> tags;

    @Nonnull
    @NotNull(message = "Metadata dürfen nicht null sein.")
    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonObjectConverter.class)
    private Map<String, Object> metadata;

    @Nonnull
    @NotNull(message = "Das Erstellungsdatum darf nicht null sein.")
    private LocalDateTime createdAt;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AuditLogEntity that = (AuditLogEntity) o;
        return Objects.equals(id, that.id)
                && Objects.equals(eventTs, that.eventTs)
                && Objects.equals(triggeringUserId, that.triggeringUserId)
                && Objects.equals(actorType, that.actorType)
                && Objects.equals(actorId, that.actorId)
                && Objects.equals(actorLabel, that.actorLabel)
                && Objects.equals(triggerType, that.triggerType)
                && Objects.equals(triggerRef, that.triggerRef)
                && Objects.equals(serviceName, that.serviceName)
                && Objects.equals(instanceId, that.instanceId)
                && Objects.equals(actionType, that.actionType)
                && Objects.equals(component, that.component)
                && Objects.equals(entityType, that.entityType)
                && Objects.equals(entityId, that.entityId)
                && Objects.equals(message, that.message)
                && Objects.equals(changedData, that.changedData)
                && Objects.equals(beforeData, that.beforeData)
                && Objects.equals(afterData, that.afterData)
                && Objects.equals(dataDiff, that.dataDiff)
                && Objects.equals(actionResult, that.actionResult)
                && Objects.equals(reason, that.reason)
                && Objects.equals(source, that.source)
                && Objects.equals(requestId, that.requestId)
                && Objects.equals(sessionId, that.sessionId)
                && Objects.equals(ipAddress, that.ipAddress)
                && Objects.equals(userAgent, that.userAgent)
                && Objects.equals(severity, that.severity)
                && Objects.equals(tags, that.tags)
                && Objects.equals(metadata, that.metadata)
                && Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                eventTs,
                triggeringUserId,
                actorType,
                actorId,
                actorLabel,
                triggerType,
                triggerRef,
                serviceName,
                instanceId,
                actionType,
                component,
                entityType,
                entityId,
                message,
                changedData,
                beforeData,
                afterData,
                dataDiff,
                actionResult,
                reason,
                source,
                requestId,
                sessionId,
                ipAddress,
                userAgent,
                severity,
                tags,
                metadata,
                createdAt
        );
    }

    @Nonnull
    public Long getId() {
        return id;
    }

    public AuditLogEntity setId(@Nullable Long id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public LocalDateTime getEventTs() {
        return eventTs;
    }

    public AuditLogEntity setEventTs(@Nonnull LocalDateTime eventTs) {
        this.eventTs = eventTs;
        return this;
    }

    @Nullable
    public String getTriggeringUserId() {
        return triggeringUserId;
    }

    public AuditLogEntity setTriggeringUserId(@Nullable String triggeringUserId) {
        this.triggeringUserId = triggeringUserId;
        return this;
    }

    @Nonnull
    public String getActorType() {
        return actorType;
    }

    public AuditLogEntity setActorType(@Nonnull String actorType) {
        this.actorType = actorType;
        return this;
    }

    @Nullable
    public String getActorId() {
        return actorId;
    }

    public AuditLogEntity setActorId(@Nullable String actorId) {
        this.actorId = actorId;
        return this;
    }

    @Nullable
    public String getActorLabel() {
        return actorLabel;
    }

    public AuditLogEntity setActorLabel(@Nullable String actorLabel) {
        this.actorLabel = actorLabel;
        return this;
    }

    @Nullable
    public String getTriggerType() {
        return triggerType;
    }

    public AuditLogEntity setTriggerType(@Nullable String triggerType) {
        this.triggerType = triggerType;
        return this;
    }

    @Nullable
    public String getTriggerRef() {
        return triggerRef;
    }

    public AuditLogEntity setTriggerRef(@Nullable String triggerRef) {
        this.triggerRef = triggerRef;
        return this;
    }

    @Nullable
    public String getServiceName() {
        return serviceName;
    }

    public AuditLogEntity setServiceName(@Nullable String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    @Nullable
    public String getInstanceId() {
        return instanceId;
    }

    public AuditLogEntity setInstanceId(@Nullable String instanceId) {
        this.instanceId = instanceId;
        return this;
    }

    @Nonnull
    public String getActionType() {
        return actionType;
    }

    public AuditLogEntity setActionType(@Nonnull String actionType) {
        this.actionType = actionType;
        return this;
    }

    @Nonnull
    public String getComponent() {
        return component;
    }

    public AuditLogEntity setComponent(@Nonnull String component) {
        this.component = component;
        return this;
    }

    @Nullable
    public String getEntityType() {
        return entityType;
    }

    public AuditLogEntity setEntityType(@Nullable String entityType) {
        this.entityType = entityType;
        return this;
    }

    @Nullable
    public String getEntityId() {
        return entityId;
    }

    public AuditLogEntity setEntityId(@Nullable String entityId) {
        this.entityId = entityId;
        return this;
    }

    @Nonnull
    public String getMessage() {
        return message;
    }

    public AuditLogEntity setMessage(@Nonnull String message) {
        this.message = message;
        return this;
    }

    @Nonnull
    public Boolean getChangedData() {
        return changedData;
    }

    public AuditLogEntity setChangedData(@Nonnull Boolean changedData) {
        this.changedData = changedData;
        return this;
    }

    @Nullable
    public Map<String, Object> getBeforeData() {
        return beforeData;
    }

    public AuditLogEntity setBeforeData(@Nullable Map<String, Object> beforeData) {
        this.beforeData = beforeData;
        return this;
    }

    @Nullable
    public Map<String, Object> getAfterData() {
        return afterData;
    }

    public AuditLogEntity setAfterData(@Nullable Map<String, Object> afterData) {
        this.afterData = afterData;
        return this;
    }

    @Nullable
    public Map<String, Object> getDataDiff() {
        return dataDiff;
    }

    public AuditLogEntity setDataDiff(@Nullable Map<String, Object> dataDiff) {
        this.dataDiff = dataDiff;
        return this;
    }

    @Nonnull
    public String getActionResult() {
        return actionResult;
    }

    public AuditLogEntity setActionResult(@Nonnull String actionResult) {
        this.actionResult = actionResult;
        return this;
    }

    @Nullable
    public String getReason() {
        return reason;
    }

    public AuditLogEntity setReason(@Nullable String reason) {
        this.reason = reason;
        return this;
    }

    @Nullable
    public String getSource() {
        return source;
    }

    public AuditLogEntity setSource(@Nullable String source) {
        this.source = source;
        return this;
    }

    @Nullable
    public String getRequestId() {
        return requestId;
    }

    public AuditLogEntity setRequestId(@Nullable String requestId) {
        this.requestId = requestId;
        return this;
    }

    @Nullable
    public String getSessionId() {
        return sessionId;
    }

    public AuditLogEntity setSessionId(@Nullable String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    @Nullable
    public String getIpAddress() {
        return ipAddress;
    }

    public AuditLogEntity setIpAddress(@Nullable String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }

    @Nullable
    public String getUserAgent() {
        return userAgent;
    }

    public AuditLogEntity setUserAgent(@Nullable String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    @Nullable
    public String getSeverity() {
        return severity;
    }

    public AuditLogEntity setSeverity(@Nullable String severity) {
        this.severity = severity;
        return this;
    }

    @Nonnull
    public List<String> getTags() {
        return tags;
    }

    public AuditLogEntity setTags(@Nonnull List<String> tags) {
        this.tags = tags;
        return this;
    }

    @Nonnull
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public AuditLogEntity setMetadata(@Nonnull Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }

    @Nonnull
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public AuditLogEntity setCreatedAt(@Nonnull LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }
}
