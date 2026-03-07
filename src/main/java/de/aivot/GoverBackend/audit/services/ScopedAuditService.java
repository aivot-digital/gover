package de.aivot.GoverBackend.audit.services;

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
import java.util.HashMap;
import java.util.Map;

public class ScopedAuditService {
    private final Logger logger;
    private final String component;
    private final AuditLogService auditLogService;

    public ScopedAuditService(Class<?> cls, AuditLogService auditLogService) {
        this.logger = LoggerFactory.getLogger(cls);
        this.component = cls.getSimpleName();
        this.auditLogService = auditLogService;
    }

    public void addAuditEntry(@Nonnull AuditLogPayload payload) {
        var request = getCurrentRequest();

        var timestamp = payload.getTimestamp() != null ? payload.getTimestamp() : LocalDateTime.now();
        var actorType = firstNonBlank(payload.getActorType(), request != null ? AuditLogPayload.ACTOR_TYPE_USER : AuditLogPayload.ACTOR_TYPE_SYSTEM);
        var actorId = payload.getActorId();

        var triggerType = firstNonBlank(payload.getTriggerType(), "Message");
        var triggerRef = payload.getTriggerRef();
        var triggerRefType = payload.getTriggerRefType();

        var module = firstNonBlank(payload.getModule(), component);
        var message = firstNonBlank(payload.getMessage(), triggerType + " in " + module);
        var diff = payload.getDiff();

        var metadata = new HashMap<String, Object>();
        if (payload.getMetadata() != null) {
            metadata.putAll(payload.getMetadata());
        }

        var ipAddress = firstNonBlank(payload.getIpAddress(), extractIpAddress(request));

        var auditLog = new AuditLogEntity()
                .setTimestamp(timestamp)
                .setActorType(actorType)
                .setActorId(actorId)
                .setTriggerType(triggerType)
                .setTriggerRef(triggerRef)
                .setTriggerRefType(triggerRefType)
                .setModule(module)
                .setMessage(message)
                .setDiff(diff)
                .setMetadata(metadata)
                .setIpAddress(ipAddress);

        logger.atInfo()
                .setMessage(message)
                .addKeyValue("actorType", actorType)
                .addKeyValue("actorId", actorId)
                .addKeyValue("triggerType", triggerType)
                .addKeyValue("triggerRef", triggerRef)
                .addKeyValue("module", module)
                .log();

        try {
            auditLogService.create(auditLog);
        } catch (Exception e) {
            logger.atError()
                    .setMessage("Failed to persist audit log")
                    .setCause(e)
                    .addKeyValue("module", module)
                    .addKeyValue("triggerType", triggerType)
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

    @Nonnull
    private String firstNonBlank(@Nullable String first, @Nonnull String fallback) {
        if (first == null || first.isBlank()) {
            return fallback;
        }

        return first;
    }
}
