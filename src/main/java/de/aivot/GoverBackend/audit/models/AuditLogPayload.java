package de.aivot.GoverBackend.audit.models;

import com.fasterxml.jackson.core.type.TypeReference;
import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.user.entities.UserEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;
import java.util.*;

public class AuditLogPayload {
    public static final String ACTOR_TYPE_USER = "User";
    public static final String ACTOR_TYPE_SYSTEM = "System";
    public static final String ACTOR_TYPE_PROCESS = "Process";

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
    private String message;

    @Nullable
    private Map<String, Object> diff;

    @Nullable
    private Map<String, Object> metadata;

    @Nullable
    private String ipAddress;


    private final ScopedAuditService service;

    private AuditLogPayload(ScopedAuditService service) {
        this.service = service;
    }

    // region Utils

    public static AuditLogPayload create(ScopedAuditService service) {
        return new AuditLogPayload(service)
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
                                           @Nonnull Object entityId,
                                           @Nonnull String entityRefType) {
        return this
                .withAuditAction(action, entityClass, entityId, entityRefType, Map.of());
    }

    public AuditLogPayload withAuditAction(@Nonnull AuditAction action,
                                           @Nonnull Class<?> entityClass,
                                           @Nonnull Object entityId,
                                           @Nonnull String entityRefType,
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
                .setEntityRefType(entityRefType)
                .setMessage(action.name() + " " + entityClass.getSimpleName() + " #" + entityId)
                .setMetadata(me);
    }

    public AuditLogPayload withException(@Nullable Throwable exception,
                                         @Nonnull Class<?> sourceClass) {
        return this
                .setTriggerType("Exception")
                .setEntityType("Exception")
                .setMessage(exception != null ? exception.getMessage() : "Unknown exception")
                .setMetadata(Map.of(
                        "exception", exception != null ? exception.getClass().getName() : "Unknown exception",
                        "class", sourceClass.getName()
                ));
    }

    public AuditLogPayload withDiff(@Nullable Map<String, Object> oldState,
                                    @Nullable Map<String, Object> newState) {
        return setDiff(createDiff(oldState, newState));
    }

    public void log() {
        service.addAuditEntry(this);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static Map<String, Object> createDiff(@Nullable Map<String, Object> oldState,
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
        oldValue = normalizeNestedValue(oldValue);
        newValue = normalizeNestedValue(newValue);

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

        if (oldValue instanceof List<?> && newValue instanceof List<?>) {
            var nestedDiff = createListDiff((List<Object>) oldValue, (List<Object>) newValue);
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
    private static Map<String, Object> createListDiff(@Nonnull List<Object> oldValue,
                                                      @Nonnull List<Object> newValue) {
        var diff = new HashMap<String, Object>();
        var maxSize = Math.max(oldValue.size(), newValue.size());

        for (var i = 0; i < maxSize; i++) {
            var oldItem = i < oldValue.size() ? oldValue.get(i) : null;
            var newItem = i < newValue.size() ? newValue.get(i) : null;

            var valueDiff = createValueDiff(oldItem, newItem);
            if (valueDiff != null) {
                diff.put(String.valueOf(i), valueDiff);
            }
        }

        return diff.isEmpty() ? null : diff;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static Object normalizeNestedValue(@Nullable Object value) {
        if (value == null || isSimpleValue(value)) {
            return value;
        }

        if (value instanceof Map<?, ?> rawMap) {
            var normalized = new HashMap<String, Object>();
            for (var entry : rawMap.entrySet()) {
                normalized.put(String.valueOf(entry.getKey()), normalizeNestedValue(entry.getValue()));
            }
            return normalized;
        }

        if (value instanceof List<?> list) {
            var normalized = new ArrayList<>();
            for (var item : list) {
                normalized.add(normalizeNestedValue(item));
            }
            return normalized;
        }

        if (value instanceof Iterable<?> iterable) {
            var normalized = new ArrayList<>();
            for (var item : iterable) {
                normalized.add(normalizeNestedValue(item));
            }
            return normalized;
        }

        if (value.getClass().isArray()) {
            var array = ObjectMapperFactory
                    .getInstance()
                    .convertValue(value, List.class);
            return normalizeNestedValue(array);
        }

        var converted = ObjectMapperFactory
                .getInstance().convertValue(value, Object.class);
        if (converted == value) {
            return value;
        }

        return normalizeNestedValue(converted);
    }

    private static boolean isSimpleValue(@Nonnull Object value) {
        return value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof Enum<?>
                || value instanceof LocalDateTime;
    }

    @Nullable
    public static Map<String, Object> toMap(@Nullable Object value) {
        if (value == null) {
            return null;
        }

        return ObjectMapperFactory
                .getInstance()
                .convertValue(value, new TypeReference<>() {
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

    public AuditLogPayload withMessage(String format, Object... args) {
        this.setMessage(String.format(format, args));
        return this;
    }

    // endregion
}
