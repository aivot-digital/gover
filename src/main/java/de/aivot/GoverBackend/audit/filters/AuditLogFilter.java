package de.aivot.GoverBackend.audit.filters;

import de.aivot.GoverBackend.audit.entities.AuditLogEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.LocalDateTime;

public class AuditLogFilter implements Filter<AuditLogEntity> {
    private Long id;
    private String triggeringUserId;
    private String actorType;
    private String actorId;
    private String actorLabel;
    private String triggerType;
    private String triggerRef;
    private String serviceName;
    private String instanceId;
    private String actionType;
    private String component;
    private String entityType;
    private String entityId;
    private Boolean changedData;
    private String actionResult;
    private String source;
    private String requestId;
    private String sessionId;
    private String severity;
    private String tag;
    private LocalDateTime eventTsFrom;
    private LocalDateTime eventTsTo;

    public static AuditLogFilter create() {
        return new AuditLogFilter();
    }

    @Nonnull
    @Override
    public Specification<AuditLogEntity> build() {
        var builder = SpecificationBuilder
                .create(AuditLogEntity.class)
                .withEquals("id", id)
                .withContains("triggeringUserId", triggeringUserId)
                .withContains("actorType", actorType)
                .withContains("actorId", actorId)
                .withContains("actorLabel", actorLabel)
                .withContains("triggerType", triggerType)
                .withContains("triggerRef", triggerRef)
                .withContains("serviceName", serviceName)
                .withContains("instanceId", instanceId)
                .withContains("actionType", actionType)
                .withContains("component", component)
                .withContains("entityType", entityType)
                .withContains("entityId", entityId)
                .withEquals("changedData", changedData)
                .withContains("actionResult", actionResult)
                .withContains("source", source)
                .withContains("requestId", requestId)
                .withContains("sessionId", sessionId)
                .withContains("severity", severity)
                .withArrayContains("tags", tag);

        if (eventTsFrom != null) {
            builder.withSpecification((root, query, specBuilder) ->
                    specBuilder.greaterThanOrEqualTo(root.get("eventTs"), eventTsFrom)
            );
        }

        if (eventTsTo != null) {
            builder.withSpecification((root, query, specBuilder) ->
                    specBuilder.lessThanOrEqualTo(root.get("eventTs"), eventTsTo)
            );
        }

        return builder.build();
    }

    @Nullable
    public Long getId() {
        return id;
    }

    public AuditLogFilter setId(@Nullable Long id) {
        this.id = id;
        return this;
    }

    @Nullable
    public String getTriggeringUserId() {
        return triggeringUserId;
    }

    public AuditLogFilter setTriggeringUserId(@Nullable String triggeringUserId) {
        this.triggeringUserId = triggeringUserId;
        return this;
    }

    @Nullable
    public String getActorType() {
        return actorType;
    }

    public AuditLogFilter setActorType(@Nullable String actorType) {
        this.actorType = actorType;
        return this;
    }

    @Nullable
    public String getActorId() {
        return actorId;
    }

    public AuditLogFilter setActorId(@Nullable String actorId) {
        this.actorId = actorId;
        return this;
    }

    @Nullable
    public String getActorLabel() {
        return actorLabel;
    }

    public AuditLogFilter setActorLabel(@Nullable String actorLabel) {
        this.actorLabel = actorLabel;
        return this;
    }

    @Nullable
    public String getTriggerType() {
        return triggerType;
    }

    public AuditLogFilter setTriggerType(@Nullable String triggerType) {
        this.triggerType = triggerType;
        return this;
    }

    @Nullable
    public String getTriggerRef() {
        return triggerRef;
    }

    public AuditLogFilter setTriggerRef(@Nullable String triggerRef) {
        this.triggerRef = triggerRef;
        return this;
    }

    @Nullable
    public String getServiceName() {
        return serviceName;
    }

    public AuditLogFilter setServiceName(@Nullable String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    @Nullable
    public String getInstanceId() {
        return instanceId;
    }

    public AuditLogFilter setInstanceId(@Nullable String instanceId) {
        this.instanceId = instanceId;
        return this;
    }

    @Nullable
    public String getActionType() {
        return actionType;
    }

    public AuditLogFilter setActionType(@Nullable String actionType) {
        this.actionType = actionType;
        return this;
    }

    @Nullable
    public String getComponent() {
        return component;
    }

    public AuditLogFilter setComponent(@Nullable String component) {
        this.component = component;
        return this;
    }

    @Nullable
    public String getEntityType() {
        return entityType;
    }

    public AuditLogFilter setEntityType(@Nullable String entityType) {
        this.entityType = entityType;
        return this;
    }

    @Nullable
    public String getEntityId() {
        return entityId;
    }

    public AuditLogFilter setEntityId(@Nullable String entityId) {
        this.entityId = entityId;
        return this;
    }

    @Nullable
    public Boolean getChangedData() {
        return changedData;
    }

    public AuditLogFilter setChangedData(@Nullable Boolean changedData) {
        this.changedData = changedData;
        return this;
    }

    @Nullable
    public String getActionResult() {
        return actionResult;
    }

    public AuditLogFilter setActionResult(@Nullable String actionResult) {
        this.actionResult = actionResult;
        return this;
    }

    @Nullable
    public String getSource() {
        return source;
    }

    public AuditLogFilter setSource(@Nullable String source) {
        this.source = source;
        return this;
    }

    @Nullable
    public String getRequestId() {
        return requestId;
    }

    public AuditLogFilter setRequestId(@Nullable String requestId) {
        this.requestId = requestId;
        return this;
    }

    @Nullable
    public String getSessionId() {
        return sessionId;
    }

    public AuditLogFilter setSessionId(@Nullable String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    @Nullable
    public String getSeverity() {
        return severity;
    }

    public AuditLogFilter setSeverity(@Nullable String severity) {
        this.severity = severity;
        return this;
    }

    @Nullable
    public String getTag() {
        return tag;
    }

    public AuditLogFilter setTag(@Nullable String tag) {
        this.tag = tag;
        return this;
    }

    @Nullable
    public LocalDateTime getEventTsFrom() {
        return eventTsFrom;
    }

    public AuditLogFilter setEventTsFrom(@Nullable LocalDateTime eventTsFrom) {
        this.eventTsFrom = eventTsFrom;
        return this;
    }

    @Nullable
    public LocalDateTime getEventTsTo() {
        return eventTsTo;
    }

    public AuditLogFilter setEventTsTo(@Nullable LocalDateTime eventTsTo) {
        this.eventTsTo = eventTsTo;
        return this;
    }
}
