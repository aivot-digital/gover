package de.aivot.GoverBackend.audit.services;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.user.entities.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class ScopedAuditService {
    private final Logger logger;

    public ScopedAuditService(Class<?> cls) {
        this.logger = LoggerFactory.getLogger(cls);
    }

    public void logAction(@Nonnull UserEntity user, @Nonnull AuditAction action, @Nonnull Class<?> resource) {
        logAction(user, action, resource, Map.of());
    }

    public void logAction(@Nonnull UserEntity user, @Nonnull AuditAction action, @Nonnull Class<?> resource, @Nonnull Map<String, Object> values) {
        var actionLabel = getActionLabel(action);
        var message = "The staff user with the id " + user.getId() + " is " + actionLabel + " the resource " + resource.getName();

        var combinedValues = new HashMap<>(values);
        combinedValues.put("resource", resource.getName());
        combinedValues.put("userId", user.getId());

        logMessage(user, message, combinedValues);
    }

    public void logMessage(@Nonnull String message, @Nonnull Map<String, Object> values) {
        var loggingEventBuilder = logger
                .atInfo()
                .setMessage(message);

        for (var entry : values.entrySet()) {
            loggingEventBuilder = loggingEventBuilder
                    .addKeyValue(entry.getKey(), entry.getValue());
        }

        loggingEventBuilder.log();
    }

    public void debugMessage(@Nonnull String message, @Nonnull Map<String, Object> values) {
        var loggingEventBuilder = logger
                .atDebug()
                .setMessage(message);

        for (var entry : values.entrySet()) {
            loggingEventBuilder = loggingEventBuilder
                    .addKeyValue(entry.getKey(), entry.getValue());
        }

        loggingEventBuilder.log();
    }


    public void logMessage(@Nonnull UserEntity user, @Nonnull String message, @Nonnull Map<String, Object> values) {
        var loggingEventBuilder = logger
                .atInfo()
                .setMessage(message)
                .addKeyValue("userId", user.getId());

        for (var entry : values.entrySet()) {
            loggingEventBuilder = loggingEventBuilder
                    .addKeyValue(entry.getKey(), entry.getValue());
        }

        loggingEventBuilder.log();

        // TODO: Setup audit log persistence
    }

    public void logException(@Nonnull String message, @Nonnull Throwable throwable) {
        logException(message, throwable, Map.of());
    }

    public void logException(@Nonnull String message, @Nonnull Throwable throwable, @Nonnull Map<String, Object> values) {
        var loggingEventBuilder = logger
                .atError()
                .setMessage("An exception occurred")
                .setCause(throwable);

        for (var entry : values.entrySet()) {
            loggingEventBuilder = loggingEventBuilder
                    .addKeyValue(entry.getKey(), entry.getValue());
        }

        loggingEventBuilder.log();
    }

    public void logError(@Nonnull String message) {
        logError(message, Map.of());
    }

    public void logError(@Nonnull String message, @Nonnull Map<String, Object> values) {
        var loggingEventBuilder = logger
                .atError()
                .setMessage(message);

        for (var entry : values.entrySet()) {
            loggingEventBuilder = loggingEventBuilder
                    .addKeyValue(entry.getKey(), entry.getValue());
        }

        loggingEventBuilder.log();
    }

    private String getActionLabel(@Nonnull AuditAction action) {
        return switch (action) {
            case Create -> "creating";
            case List -> "listing";
            case Retrieve -> "retrieving";
            case Update -> "updating";
            case Delete -> "deleting";
        };
    }
}
