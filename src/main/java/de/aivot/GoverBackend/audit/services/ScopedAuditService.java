package de.aivot.GoverBackend.audit.services;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.entities.AuditLogEntity;
import de.aivot.GoverBackend.audit.models.AuditLogPayload;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.*;

public class ScopedAuditService {
    private final Logger logger;
    private final String component;
    private final AuditLogService auditLogService;

    public ScopedAuditService(Class<?> cls, AuditLogService auditLogService) {
        this.logger = LoggerFactory.getLogger(cls);
        this.component = cls.getName();
        this.auditLogService = auditLogService;
    }

    /**
     * Canonical method for writing audit log entries.
     */
    public void addAuditEntry(@Nonnull AuditLogPayload payload) {
        var request = getCurrentRequest();
        var now = payload.getEventTs() != null ? payload.getEventTs() : LocalDateTime.now();
        var user = payload.getTriggeringUser();

        var actionType = payload.getActionType();
        if (actionType == null) {
            actionType = payload.getAction() != null ? payload.getAction().name() : "Message";
        }

        var message = payload.getMessage();
        if (message == null) {
            message = createDefaultMessage(payload);
        }

        var entityType = firstNonNull(
                payload.getEntityType(),
                payload.getResource() != null ? payload.getResource().getName() : null
        );

        var actorType = firstNonNull(payload.getActorType(), user != null ? "USER" : "SYSTEM");
        var actorId = firstNonNull(payload.getActorId(), user != null ? user.getId() : null);
        var actorLabel = payload.getActorLabel();
        if (actorLabel == null && user == null && "SYSTEM".equalsIgnoreCase(actorType)) {
            actorLabel = "system";
        }

        String triggeringUserId = null;
        if (user != null) {
            triggeringUserId = user.getId();
        } else if ("USER".equalsIgnoreCase(actorType) && actorId != null && actorId.length() == 36) {
            triggeringUserId = actorId;
        }

        var requestId = firstNonNull(
                payload.getRequestId(),
                request != null ? firstNonNull(request.getHeader("X-Request-Id"), request.getHeader("X-Correlation-Id")) : null
        );

        var sessionId = firstNonNull(
                payload.getSessionId(),
                request != null && request.getSession(false) != null ? request.getSession(false).getId() : null
        );

        var ipAddress = firstNonNull(payload.getIpAddress(), extractIpAddress(request));
        var userAgent = firstNonNull(payload.getUserAgent(), request != null ? request.getHeader("User-Agent") : null);
        var source = firstNonNull(payload.getSource(), request != null ? "api" : null);

        var beforeData = payload.getBeforeData();
        var afterData = payload.getAfterData();
        var dataDiff = createDataDiff(beforeData, afterData);

        var changedData = payload.getChangedData();
        if (changedData == null) {
            changedData = (
                    (dataDiff != null && !dataDiff.isEmpty()) ||
                    payload.getAction() == AuditAction.Create ||
                    payload.getAction() == AuditAction.Update ||
                    payload.getAction() == AuditAction.Delete
            );
        }

        var metadata = payload.getMetadata() != null ? payload.getMetadata() : Map.<String, Object>of();
        var tags = payload.getTags() != null ? payload.getTags() : List.<String>of();

        var auditLog = new AuditLogEntity()
                .setEventTs(now)
                .setCreatedAt(LocalDateTime.now())
                .setTriggeringUserId(triggeringUserId)
                .setActorType(actorType)
                .setActorId(actorId)
                .setActorLabel(actorLabel)
                .setTriggerType(payload.getTriggerType())
                .setTriggerRef(payload.getTriggerRef())
                .setServiceName(firstNonNull(payload.getServiceName(), "gover-backend"))
                .setInstanceId(firstNonNull(payload.getInstanceId(), System.getenv("HOSTNAME")))
                .setActionType(actionType)
                .setComponent(firstNonNull(payload.getComponent(), component))
                .setEntityType(entityType)
                .setEntityId(payload.getEntityId())
                .setMessage(message)
                .setChangedData(changedData)
                .setBeforeData(beforeData)
                .setAfterData(afterData)
                .setDataDiff(dataDiff)
                .setActionResult(firstNonNull(payload.getActionResult(), "success"))
                .setReason(payload.getReason())
                .setSource(source)
                .setRequestId(requestId)
                .setSessionId(sessionId)
                .setIpAddress(ipAddress)
                .setUserAgent(userAgent)
                .setSeverity(payload.getSeverity())
                .setTags(tags)
                .setMetadata(metadata);

        logger.atInfo()
                .setMessage(message)
                .addKeyValue("actionType", actionType)
                .addKeyValue("component", auditLog.getComponent())
                .addKeyValue("actorType", actorType)
                .addKeyValue("actorId", actorId)
                .addKeyValue("entityType", entityType)
                .addKeyValue("entityId", payload.getEntityId())
                .log();

        try {
            auditLogService.create(auditLog);
        } catch (Exception e) {
            logger.atError()
                    .setMessage("Failed to persist audit log")
                    .setCause(e)
                    .addKeyValue("component", component)
                    .addKeyValue("triggeringUserId", triggeringUserId)
                    .log();
        }
    }

    @Nullable
    private HttpServletRequest getCurrentRequest() {
        var requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }

        return null;
    }

    @Nullable
    private String extractIpAddress(@Nullable HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        var forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    @Nullable
    private String firstNonNull(@Nullable String first, @Nullable String second) {
        if (first != null) {
            return first;
        }
        return second;
    }

    @Nonnull
    private String createDefaultMessage(@Nonnull AuditLogPayload payload) {
        if (payload.getAction() == null || payload.getResource() == null) {
            return "Audit event";
        }

        var actionLabel = switch (payload.getAction()) {
            case Create -> "creating";
            case List -> "listing";
            case Retrieve -> "retrieving";
            case Update -> "updating";
            case Delete -> "deleting";
        };

        if (payload.getTriggeringUser() != null) {
            return "The staff user with the id " + payload.getTriggeringUser().getId() + " is " + actionLabel + " the resource " + payload.getResource().getName();
        }

        return "The actor is " + actionLabel + " the resource " + payload.getResource().getName();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private Map<String, Object> createDataDiff(
            @Nullable Map<String, Object> beforeData,
            @Nullable Map<String, Object> afterData
    ) {
        if (beforeData == null && afterData == null) {
            return null;
        }

        if (beforeData == null || afterData == null) {
            return Map.of("value", Map.of(
                    "before", beforeData,
                    "after", afterData
            ));
        }

        var keys = new HashSet<String>();
        keys.addAll(beforeData.keySet());
        keys.addAll(afterData.keySet());

        var diff = new HashMap<String, Object>();

        for (var key : keys) {
            var beforeValue = beforeData.get(key);
            var afterValue = afterData.get(key);

            var valueDiff = createValueDiff(beforeValue, afterValue);
            if (valueDiff != null) {
                diff.put(key, valueDiff);
            }
        }

        return diff.isEmpty() ? null : diff;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private Object createValueDiff(@Nullable Object beforeValue, @Nullable Object afterValue) {
        if (Objects.equals(beforeValue, afterValue)) {
            return null;
        }

        if (beforeValue instanceof Map<?, ?> && afterValue instanceof Map<?, ?>) {
            var beforeMap = (Map<String, Object>) beforeValue;
            var afterMap = (Map<String, Object>) afterValue;
            var nestedDiff = createDataDiff(beforeMap, afterMap);
            if (nestedDiff != null && !nestedDiff.isEmpty()) {
                return nestedDiff;
            }
            return null;
        }

        return Map.of(
                "before", beforeValue,
                "after", afterValue
        );
    }
}
