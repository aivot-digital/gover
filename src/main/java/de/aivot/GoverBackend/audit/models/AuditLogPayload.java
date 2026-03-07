package de.aivot.GoverBackend.audit.models;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.user.entities.UserEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

public class AuditLogPayload {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
    private String message;

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
                                           @Nullable Object entityId) {
        return this
                .withAuditAction(action, entityClass, entityId, Map.of());
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
                .setTriggerRef(entityId != null ? String.valueOf(entityId) : null)
                .setTriggerRefType(entityId != null ? TRIGGER_REF_TYPE : null)
                .setModule(entityClass.getSimpleName())
                .setMessage(action.name() + " " + entityClass.getSimpleName() + (entityId != null ? " #" + entityId : ""))
                .setMetadata(me);
    }

    @SuppressWarnings("unchecked")
    public AuditLogPayload withDiffUndefined(@Nullable Map<?, ?> oldState,
                                             @Nullable Map<?, ?> newState) {
        return setDiff(createDiff((Map<String, Object>) oldState, (Map<String, Object>) newState));
    }

    public AuditLogPayload withDiff(@Nullable Map<String, Object> oldState,
                                    @Nullable Map<String, Object> newState) {
        return setDiff(createDiff(oldState, newState));
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static Map<String, Object> createDiff(@Nullable Map<String, Object> oldState,
                                                 @Nullable Map<String, Object> newState) {
        if (oldState == null && newState == null) {
            return null;
        }

        if (oldState == null || newState == null) {
            var map = new HashMap<String, Object>();
            map.put("old", oldState);
            map.put("new", newState);

            return Map.of(
                    "value", map
            );
        }

        var allKeys = new HashSet<String>();
        allKeys.addAll(oldState.keySet());
        allKeys.addAll(newState.keySet());

        var diff = new HashMap<String, Object>();

        for (var key : allKeys) {
            var oldValue = oldState.get(key);
            var newValue = newState.get(key);

            var valueDiff = createValueDiff(oldValue, newValue);
            if (valueDiff != null) {
                diff.put(key, valueDiff);
            }
        }

        return diff.isEmpty() ? null : diff;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static Object createValueDiff(@Nullable Object oldValue,
                                          @Nullable Object newValue) {
        if (Objects.equals(oldValue, newValue)) {
            return null;
        }

        if (oldValue instanceof Map<?, ?> && newValue instanceof Map<?, ?>) {
            var nestedDiff = createDiff((Map<String, Object>) oldValue, (Map<String, Object>) newValue);
            if (nestedDiff != null && !nestedDiff.isEmpty()) {
                return nestedDiff;
            }
            return null;
        }

        var map = new HashMap<String, Object>();
        map.put("old", oldValue);
        map.put("new", newValue);

        return map;
    }

    public static AuditLogPayload ofLegacyAction(@Nonnull UserEntity user,
                                                 @Nonnull AuditAction action,
                                                 @Nonnull Class<?> entityClass) {
        return create()
                .withUser(user)
                .withAuditAction(action, entityClass, null, null);
    }

    public static AuditLogPayload ofLegacyAction(@Nonnull UserEntity user,
                                                 @Nonnull AuditAction action,
                                                 @Nonnull Class<?> entityClass,
                                                 @Nullable Map<String, Object> metadata) {
        return create()
                .withUser(user)
                .withAuditAction(action, entityClass, extractEntityId(metadata), metadata);
    }

    public static AuditLogPayload ofLegacyMessage(@Nonnull String message,
                                                  @Nullable Map<String, Object> metadata) {
        var me = new HashMap<String, Object>();
        if (metadata != null) {
            me.putAll(metadata);
        }
        me.put("message", message);

        return create()
                .withSystem()
                .setTriggerType("Message")
                .setMessage(message)
                .setMetadata(me);
    }

    @Nullable
    public static Map<String, Object> toMap(@Nullable Object value) {
        if (value == null) {
            return null;
        }

        return OBJECT_MAPPER.convertValue(value, new TypeReference<>() {
        });
    }

    @Nullable
    private static Object extractEntityId(@Nullable Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }

        if (metadata.containsKey("id")) {
            return metadata.get("id");
        }
        if (metadata.containsKey("entityId")) {
            return metadata.get("entityId");
        }
        return null;
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
    public String getMessage() {
        return message;
    }

    public AuditLogPayload setMessage(@Nullable String message) {
        this.message = message;
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

    // region Compatibility setters

    public AuditLogPayload setActionType(@Nullable String actionType) {
        return setTriggerType(actionType);
    }

    public AuditLogPayload setAction(@Nullable AuditAction action) {
        return setTriggerType(action != null ? action.name() : null);
    }

    public AuditLogPayload setTriggeringUser(@Nullable UserEntity user) {
        if (user == null) {
            return this;
        }
        return withUser(user);
    }

    public AuditLogPayload setResource(@Nullable Class<?> entityClass) {
        if (entityClass == null) {
            return this;
        }
        return setModule(entityClass.getSimpleName());
    }

    public AuditLogPayload setReason(@Nullable String reason) {
        return appendMetadata("reason", reason);
    }

    public AuditLogPayload setSeverity(@Nullable String severity) {
        return appendMetadata("severity", severity);
    }

    public AuditLogPayload setActionResult(@Nullable String actionResult) {
        return appendMetadata("actionResult", actionResult);
    }

    public AuditLogPayload setComponent(@Nullable String component) {
        return setModule(component);
    }

    public AuditLogPayload setEntityId(@Nullable String entityId) {
        return this
                .setTriggerRef(entityId)
                .setTriggerRefType(entityId != null ? TRIGGER_REF_TYPE : null);
    }

    public AuditLogPayload setEntityType(@Nullable String entityType) {
        return appendMetadata("entityType", entityType);
    }

    public AuditLogPayload setInstanceId(@Nullable String instanceId) {
        if (instanceId == null) {
            return this;
        }

        if (triggerRef == null) {
            setTriggerRef(instanceId);
            setTriggerRefType(TRIGGER_REF_TYPE);
        }

        return appendMetadata("instanceId", instanceId);
    }

    public AuditLogPayload setSource(@Nullable String source) {
        return appendMetadata("source", source);
    }

    public AuditLogPayload setRequestId(@Nullable String requestId) {
        return appendMetadata("requestId", requestId);
    }

    public AuditLogPayload setSessionId(@Nullable String sessionId) {
        return appendMetadata("sessionId", sessionId);
    }

    public AuditLogPayload setUserAgent(@Nullable String userAgent) {
        return appendMetadata("userAgent", userAgent);
    }

    public AuditLogPayload setBeforeData(@Nullable Map<String, Object> beforeData) {
        return appendMetadata("beforeData", beforeData);
    }

    public AuditLogPayload setAfterData(@Nullable Map<String, Object> afterData) {
        return appendMetadata("afterData", afterData);
    }

    public AuditLogPayload setChangedData(@Nullable Boolean changedData) {
        return appendMetadata("changedData", changedData);
    }

    private AuditLogPayload appendMetadata(@Nonnull String key, @Nullable Object value) {
        if (value == null) {
            return this;
        }

        if (metadata == null) {
            metadata = new HashMap<>();
        }

        metadata.put(key, value);
        return this;
    }

    // endregion
}
