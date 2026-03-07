package de.aivot.GoverBackend.process.models;

import de.aivot.GoverBackend.process.entities.ProcessInstanceEventEntity;
import de.aivot.GoverBackend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceHistoryEventRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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
        return new ProcessNodeExecutionLogger(
                processInstanceId,
                taskId,
                userId,
                identityId,
                repository
        );
    }

    public void logf(@Nonnull ProcessNodeExecutionLogLevel level,
                     @Nonnull Boolean isTechnical,
                     @Nonnull Boolean isAuditable,
                     @Nonnull String format,
                     @Nullable Object... args) {
        String message = String.format(format, args);
        var details = new HashMap<String, Object>();
        if (identityId != null) {
            details.put("identityId", identityId);
        }
        saveEvent(level, isTechnical, isAuditable, level.name(), message, details);
    }

    public void logException(ProcessNodeExecutionException exception) {
        logger
                .atError()
                .setMessage(exception.getMessage())
                .setCause(exception)
                .log();

        saveExceptionEvent(exception);
    }

    public void logException(Exception exception) {
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
                "Prozessausfuehrungsfehler",
                exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage(),
                details
        );
    }

    private void saveEvent(@Nonnull ProcessNodeExecutionLogLevel level,
                           @Nonnull Boolean isTechnical,
                           @Nonnull Boolean isAuditable,
                           @Nonnull String title,
                           @Nonnull String message,
                           @Nonnull Map<String, Object> details) {
        try {
            repository.save(new ProcessInstanceEventEntity(
                    null,
                    processInstanceId,
                    processInstanceTaskId,
                    level,
                    isTechnical,
                    isAuditable,
                    title,
                    message,
                    details,
                    LocalDateTime.now(),
                    userId
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
