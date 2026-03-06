package de.aivot.GoverBackend.audit.models;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.user.entities.UserEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class AuditLogPayload {
    public static final String ACTOR_TYPE_USER = "User";
    public static final String ACTOR_TYPE_SYSTEM = "System";
    public static final String ACTOR_TYPE_PROCESS = "Process";

    public static final String TRIGGER_REF_TYPE = "PK";

    @Nullable
    private LocalDateTime timestamp;

    @Nullable
    private String actorType;

    @Nullable
    private String actorId;

    @Nullable
    private String triggerType;

    @Nullable
    private String triggerRef;

    @Nullable
    private String triggerRefType;

    @Nullable
    private String module;

    @Nullable
    private Map<String, Object> diff;

    @Nullable
    private Map<String, Object> metadata;

    @Nullable
    private String ipAddress;

    // region Utils

    public static AuditLogPayload create() {
        return new AuditLogPayload()
                .setTimestamp(LocalDateTime.now());
    }

    public AuditLogPayload withUser(@Nonnull UserEntity user) {
        return this
                .setActorType(ACTOR_TYPE_USER)
                .setActorId(user.getId());
    }

    public AuditLogPayload withSystem() {
        return this
                .setActorType(ACTOR_TYPE_SYSTEM);
    }

    public AuditLogPayload withProcess() {
        return this
                .setActorType(ACTOR_TYPE_PROCESS);
    }

    public AuditLogPayload withAuditAction(@Nonnull AuditAction action,
                                           @Nonnull Class<?> entityClass,
                                           @Nullable Object entityId,
                                           @Nullable Map<String, Object> metadata) {
        var me = new HashMap<String, Object>();
        if (metadata != null) {
            me.putAll(metadata);
        }
        me.put("entity", entityClass.getSimpleName());
        me.put("entityId", entityId);

        return this
                .setTriggerType(action.name())
                .setMetadata(me);
    }

    // endregion

    @Nullable
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public AuditLogPayload setTimestamp(@Nullable LocalDateTime timestamp) {
        this.timestamp = timestamp;
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
    public String getTriggerRefType() {
        return triggerRefType;
    }

    public AuditLogPayload setTriggerRefType(@Nullable String triggerRefType) {
        this.triggerRefType = triggerRefType;
        return this;
    }

    @Nullable
    public String getModule() {
        return module;
    }

    public AuditLogPayload setModule(@Nullable String module) {
        this.module = module;
        return this;
    }

    @Nullable
    public Map<String, Object> getDiff() {
        return diff;
    }

    public AuditLogPayload setDiff(@Nullable Map<String, Object> diff) {
        this.diff = diff;
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

    @Nullable
    public String getIpAddress() {
        return ipAddress;
    }

    public AuditLogPayload setIpAddress(@Nullable String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }
}
