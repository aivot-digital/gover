package de.aivot.prosuna.backend.process.models;

import de.aivot.prosuna.backend.process.entities.ProcessInstanceEventEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceHistoryEventRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class ProcessNodeExecutionLogger {
    private static final Logger logger = LoggerFactory.getLogger(ProcessNodeExecutionLogger.class);

    @Nonnull
    private final Long processInstanceId;
    @Nullable
    private final Long processInstanceTaskId;
    @Nullable
    private final String userId;
    @Nullable
    private final String identityId;

    @Nonnull
    private final ProcessInstanceHistoryEventRepository repository;

    private Set<Exception> historyFailures = Collections.newSetFromMap(new IdentityHashMap<>());

    public ProcessNodeExecutionLogger(@Nonnull Long processInstanceId,
                                      @Nullable Long processInstanceTaskId,
                                      @Nullable String userId,
                                      @Nullable String identityId,
                                      @Nonnull ProcessInstanceHistoryEventRepository repository) {
        this.processInstanceId = processInstanceId;
        this.processInstanceTaskId = processInstanceTaskId;
        this.userId = userId;
        this.identityId = identityId;
        this.repository = repository;
    }

    public ProcessNodeExecutionLogger withTaskId(Long taskId) {
        var taskLogger = new ProcessNodeExecutionLogger(
                processInstanceId,
                taskId,
                userId,
                identityId,
                repository
        );
        taskLogger.historyFailures = historyFailures;
        return taskLogger;
    }

    public void history(@Nonnull ProcessNodeExecutionLogLevel level,
                        @Nonnull String title,
                        @Nonnull String message,
                        @Nonnull Map<String, Object> details,
                        @Nullable String concernedUserId,
                        @Nullable String concernedIdentityId,
                        @Nullable String concernedIdentityTitle) {
        saveEvent(level, false, true, true, title, message, details, Instant.now(),
                concernedUserId, concernedIdentityId, concernedIdentityTitle);
    }

    /** Call at execution failure boundaries, not for recoverable notification errors. */
    public void logFailure(@Nonnull Exception exception) {
        logException(exception);
        // Nested worker boundaries share this set, including loggers narrowed to a task.
        if (historyFailures.add(exception)) {
            history(ProcessNodeExecutionLogLevel.Error, "Ausführung fehlgeschlagen",
                    "Die Verarbeitung konnte nicht abgeschlossen werden.", Map.of(), null, null, null);
        }
    }

    public void logf(@Nonnull ProcessNodeExecutionLogLevel level,
                     @Nonnull Boolean isTechnical,
                     @Nonnull Boolean isAuditable,
                     @Nonnull String title,
                     @Nonnull String format,
                     @Nullable Object... args) {
        logf(level, isTechnical, isAuditable, title, Map.of(), format, args);
    }

    public void logf(@Nonnull ProcessNodeExecutionLogLevel level,
                     @Nonnull Boolean isTechnical,
                     @Nonnull Boolean isAuditable,
                     @Nonnull String title,
                     @Nonnull Map<String, Object> details,
                     @Nonnull String format,
                     @Nullable Object... args) {
        String message = String.format(format, args);
        var eventDetails = new HashMap<>(details);
        if (identityId != null) {
            eventDetails.put("identityId", identityId);
        }
        saveEvent(level, isTechnical, isAuditable, title, message, eventDetails);
    }

    public void logException(@Nonnull ProcessNodeExecutionException exception) {
        if (exception.isAlreadyLogged()) {
            return;
        }

        exception.setAlreadyLogged(true);
        logExceptionInternal(exception);
    }

    public void logException(@Nonnull Exception exception) {
        if (exception instanceof ProcessNodeExecutionException processNodeExecutionException) {
            logException(processNodeExecutionException);
            return;
        }

        logExceptionInternal(exception);
    }

    private void logExceptionInternal(@Nonnull Exception exception) {
        logger
                .atError()
                .setMessage(exception.getMessage())
                .setCause(exception)
                .log();

        saveExceptionEvent(exception);
    }

    private void saveExceptionEvent(@Nonnull Exception exception) {
        var details = new HashMap<String, Object>();
        details.put("exceptionType", exception.getClass().getName());
        details.put("message", exception.getMessage() == null ? "N/A" : exception.getMessage());
        if (identityId != null) {
            details.put("identityId", identityId);
        }
        if (exception.getCause() != null) {
            details.put("causeType", exception.getCause().getClass().getName());
            details.put("causeMessage", exception.getCause().getMessage());
        }

        saveEvent(
                ProcessNodeExecutionLogLevel.Error,
                true,
                true,
                "Prozessausführungsfehler",
                exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage(),
                details
        );
    }

    /**
     * @deprecated Use {@link #saveEvent(ProcessNodeExecutionLogLevel, Boolean, Boolean, Boolean, String, String, Map, Instant, String, String, String)}
     * to specify history relevance, timestamp, and concerned users or identities.
     */
    @Deprecated
    private void saveEvent(@Nonnull ProcessNodeExecutionLogLevel level,
                           @Nonnull Boolean isTechnical,
                           @Nonnull Boolean isAuditable,
                           @Nonnull String title,
                           @Nonnull String message,
                           @Nonnull Map<String, Object> details) {
        saveEvent(level, isTechnical, isAuditable, false, title, message, details, Instant.now(), null, null, null);
    }

    /**
     * Uses the logger's process, task, and triggering user context. Details are copied without
     * identity enrichment; concerned identities are independent of the triggering identity.
     * Persistence failures are logged without interrupting the calling operation.
     */
    public void saveEvent(@Nonnull ProcessNodeExecutionLogLevel level,
                          @Nonnull Boolean isTechnical,
                          @Nonnull Boolean isAuditable,
                          @Nonnull Boolean isHistoryRelevant,
                          @Nonnull String title,
                          @Nonnull String message,
                          @Nonnull Map<String, Object> details,
                          @Nonnull Instant timestamp,
                          @Nullable String concernedUserId,
                          @Nullable String concernedIdentityId,
                          @Nullable String concernedIdentityTitle) {
        try {
            repository.save(new ProcessInstanceEventEntity(
                    null,
                    processInstanceId,
                    processInstanceTaskId,
                    level,
                    isTechnical,
                    isAuditable,
                    isHistoryRelevant,
                    title,
                    message,
                    new LinkedHashMap<>(details),
                    timestamp,
                    userId,
                    concernedUserId,
                    concernedIdentityId,
                    concernedIdentityTitle
            ));
        } catch (Exception e) {
            logger
                    .atError()
                    .setMessage("Failed to persist process execution event")
                    .setCause(e)
                    .addKeyValue("processInstanceId", processInstanceId)
                    .addKeyValue("processInstanceTaskId", processInstanceTaskId)
                    .log();
        }
    }
}
