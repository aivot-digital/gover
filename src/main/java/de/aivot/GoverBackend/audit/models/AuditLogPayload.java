package de.aivot.GoverBackend.audit.models;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.user.entities.UserEntity;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AuditLogPayload {
    @Nullable
    private LocalDateTime eventTs;

    @Nullable
    private UserEntity triggeringUser;

    @Nullable
    private String actorType;

    @Nullable
    private String actorId;

    @Nullable
    private String actorLabel;

    @Nullable
    private String triggerType;

    @Nullable
    private String triggerRef;

    @Nullable
    private String serviceName;

    @Nullable
    private String instanceId;

    @Nullable
    private AuditAction action;

    @Nullable
    private String actionType;

    @Nullable
    private Class<?> resource;

    @Nullable
    private String component;

    @Nullable
    private String entityType;

    @Nullable
    private String entityId;

    @Nullable
    private String message;

    @Nullable
    private Boolean changedData;

    @Nullable
    private Map<String, Object> beforeData;

    @Nullable
    private Map<String, Object> afterData;

    @Nullable
    private String actionResult;

    @Nullable
    private String reason;

    @Nullable
    private String source;

    @Nullable
    private String requestId;

    @Nullable
    private String sessionId;

    @Nullable
    private String ipAddress;

    @Nullable
    private String userAgent;

    @Nullable
    private String severity;

    @Nullable
    private List<String> tags;

    @Nullable
    private Map<String, Object> metadata;

    public static AuditLogPayload create() {
        return new AuditLogPayload();
    }

    public static AuditLogPayload ofLegacyAction(
            UserEntity user,
            AuditAction action,
            Class<?> resource
    ) {
        return create()
                .setTriggeringUser(user)
                .setAction(action)
                .setResource(resource);
    }

    public static AuditLogPayload ofLegacyAction(
            UserEntity user,
            AuditAction action,
            Class<?> resource,
            Map<String, Object> metadata
    ) {
        return ofLegacyAction(user, action, resource)
                .setMetadata(metadata);
    }

    public static AuditLogPayload ofLegacyMessage(
            String message,
            Map<String, Object> metadata
    ) {
        return create()
                .setMessage(message)
                .setMetadata(metadata);
    }

    public static AuditLogPayload ofLegacyMessage(
            UserEntity user,
            String message,
            Map<String, Object> metadata
    ) {
        return ofLegacyMessage(message, metadata)
                .setTriggeringUser(user);
    }

    @Nullable
    public LocalDateTime getEventTs() {
        return eventTs;
    }

    public AuditLogPayload setEventTs(@Nullable LocalDateTime eventTs) {
        this.eventTs = eventTs;
        return this;
    }

    @Nullable
    public UserEntity getTriggeringUser() {
        return triggeringUser;
    }

    public AuditLogPayload setTriggeringUser(@Nullable UserEntity triggeringUser) {
        this.triggeringUser = triggeringUser;
        return this;
    }

    @Nullable
    public String getActorType() {
        return actorType;
    }

    public AuditLogPayload setActorType(@Nullable String actorType) {
        this.actorType = actorType;
        return this;
    }

    @Nullable
    public String getActorId() {
        return actorId;
    }

    public AuditLogPayload setActorId(@Nullable String actorId) {
        this.actorId = actorId;
        return this;
    }

    @Nullable
    public String getActorLabel() {
        return actorLabel;
    }

    public AuditLogPayload setActorLabel(@Nullable String actorLabel) {
        this.actorLabel = actorLabel;
        return this;
    }

    @Nullable
    public String getTriggerType() {
        return triggerType;
    }

    public AuditLogPayload setTriggerType(@Nullable String triggerType) {
        this.triggerType = triggerType;
        return this;
    }

    @Nullable
    public String getTriggerRef() {
        return triggerRef;
    }

    public AuditLogPayload setTriggerRef(@Nullable String triggerRef) {
        this.triggerRef = triggerRef;
        return this;
    }

    @Nullable
    public String getServiceName() {
        return serviceName;
    }

    public AuditLogPayload setServiceName(@Nullable String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    @Nullable
    public String getInstanceId() {
        return instanceId;
    }

    public AuditLogPayload setInstanceId(@Nullable String instanceId) {
        this.instanceId = instanceId;
        return this;
    }

    @Nullable
    public AuditAction getAction() {
        return action;
    }

    public AuditLogPayload setAction(@Nullable AuditAction action) {
        this.action = action;
        return this;
    }

    @Nullable
    public String getActionType() {
        return actionType;
    }

    public AuditLogPayload setActionType(@Nullable String actionType) {
        this.actionType = actionType;
        return this;
    }

    @Nullable
    public Class<?> getResource() {
        return resource;
    }

    public AuditLogPayload setResource(@Nullable Class<?> resource) {
        this.resource = resource;
        return this;
    }

    @Nullable
    public String getComponent() {
        return component;
    }

    public AuditLogPayload setComponent(@Nullable String component) {
        this.component = component;
        return this;
    }

    @Nullable
    public String getEntityType() {
        return entityType;
    }

    public AuditLogPayload setEntityType(@Nullable String entityType) {
        this.entityType = entityType;
        return this;
    }

    @Nullable
    public String getEntityId() {
        return entityId;
    }

    public AuditLogPayload setEntityId(@Nullable String entityId) {
        this.entityId = entityId;
        return this;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    public AuditLogPayload setMessage(@Nullable String message) {
        this.message = message;
        return this;
    }

    @Nullable
    public Boolean getChangedData() {
        return changedData;
    }

    public AuditLogPayload setChangedData(@Nullable Boolean changedData) {
        this.changedData = changedData;
        return this;
    }

    @Nullable
    public Map<String, Object> getBeforeData() {
        return beforeData;
    }

    public AuditLogPayload setBeforeData(@Nullable Map<String, Object> beforeData) {
        this.beforeData = beforeData;
        return this;
    }

    @Nullable
    public Map<String, Object> getAfterData() {
        return afterData;
    }

    public AuditLogPayload setAfterData(@Nullable Map<String, Object> afterData) {
        this.afterData = afterData;
        return this;
    }

    @Nullable
    public String getActionResult() {
        return actionResult;
    }

    public AuditLogPayload setActionResult(@Nullable String actionResult) {
        this.actionResult = actionResult;
        return this;
    }

    @Nullable
    public String getReason() {
        return reason;
    }

    public AuditLogPayload setReason(@Nullable String reason) {
        this.reason = reason;
        return this;
    }

    @Nullable
    public String getSource() {
        return source;
    }

    public AuditLogPayload setSource(@Nullable String source) {
        this.source = source;
        return this;
    }

    @Nullable
    public String getRequestId() {
        return requestId;
    }

    public AuditLogPayload setRequestId(@Nullable String requestId) {
        this.requestId = requestId;
        return this;
    }

    @Nullable
    public String getSessionId() {
        return sessionId;
    }

    public AuditLogPayload setSessionId(@Nullable String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    @Nullable
    public String getIpAddress() {
        return ipAddress;
    }

    public AuditLogPayload setIpAddress(@Nullable String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }

    @Nullable
    public String getUserAgent() {
        return userAgent;
    }

    public AuditLogPayload setUserAgent(@Nullable String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    @Nullable
    public String getSeverity() {
        return severity;
    }

    public AuditLogPayload setSeverity(@Nullable String severity) {
        this.severity = severity;
        return this;
    }

    @Nullable
    public List<String> getTags() {
        return tags;
    }

    public AuditLogPayload setTags(@Nullable List<String> tags) {
        this.tags = tags;
        return this;
    }

    @Nullable
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public AuditLogPayload setMetadata(@Nullable Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }
}
