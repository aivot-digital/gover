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
    private String entityType;

    @Nullable
    private String entityRef;

    @Nullable
    private String entityRefType;

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
                                           @Nonnull String module,
                                           @Nonnull Class<?> entityClass,
                                           @Nonnull Object entityId) {
        return this
                .withAuditAction(action, module, entityClass, entityId, Map.of());
    }

    public AuditLogPayload withAuditAction(@Nonnull AuditAction action,
                                           @Nonnull String module,
                                           @Nonnull Class<?> entityClass,
                                           @Nonnull Object entityId,
                                           @Nullable Map<String, Object> metadata) {
        var me = new HashMap<String, Object>();
        if (metadata != null) {
            me.putAll(metadata);
        }
        me.put("entity", entityClass.getSimpleName());
        me.put("entityId", entityId);

        return this
                .setTriggerType(action.name())
                .setEntityType(entityClass.getSimpleName())
                .setEntityRef(String.valueOf(entityId))
                .setEntityRefType(TRIGGER_REF_TYPE)
                .setModule(module)
                .setMessage(action.name() + " " + entityClass.getSimpleName() + " #" + entityId)
                .setMetadata(me);
    }

    public AuditLogPayload withException(@Nullable Throwable exception,
                                         @Nonnull String module,
                                         @Nonnull Class<?> sourceClass) {
        return this
                .setModule(module)
                .setTriggerType("Exception")
                .setEntityType("Exception")
                .setMessage(exception != null ? exception.getMessage() : "Unknown exception")
                .setMetadata(Map.of(
                        "exception", exception != null ? exception.getClass().getName() : "Unknown exception",
                        "class", sourceClass.getName()
                ));
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

    @Nullable
    public static Map<String, Object> toMap(@Nullable Object value) {
        if (value == null) {
            return null;
        }

        return OBJECT_MAPPER.convertValue(value, new TypeReference<>() {
        });
    }

    // endregion

    // region Getters & Setters

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
    public String getEntityType() {
        return entityType;
    }

    public AuditLogPayload setEntityType(@Nullable String entityType) {
        this.entityType = entityType;
        return this;
    }

    @Nullable
    public String getEntityRef() {
        return entityRef;
    }

    public AuditLogPayload setEntityRef(@Nullable String entityRef) {
        this.entityRef = entityRef;
        return this;
    }

    @Nullable
    public String getEntityRefType() {
        return entityRefType;
    }

    public AuditLogPayload setEntityRefType(@Nullable String entityRefType) {
        this.entityRefType = entityRefType;
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

    // endregion
}
