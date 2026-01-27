package de.aivot.GoverBackend.process.models;

import de.aivot.GoverBackend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceHistoryEventRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class ProcessNodeExecutionLogger {
    @Nonnull
    private final Long processInstanceId;
    @Nullable
    private Long processInstanceTaskId;
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
        this.processInstanceTaskId = taskId;
        return this;
    }

    public void logf(@Nonnull ProcessNodeExecutionLogLevel level,
                     @Nonnull Boolean isTechnical,
                     @Nonnull Boolean isAuditable,
                     @Nonnull String format,
                     @Nullable Object... args) {
        String message = String.format(format, args);
        // Here you would implement the actual logging logic, e.g., writing to a database or a log file.
        System.out.printf("ProcessInstanceId: %d, ProcessInstanceTaskId: %d, UserId: %s, IdentityId: %s - %s%n",
                processInstanceId, processInstanceTaskId,
                userId != null ? userId : "N/A",
                identityId != null ? identityId : "N/A",
                message);
    }

    public void logException(ProcessNodeExecutionException exception) {
        // TODO
    }

    public void logException(Exception exception) {
        // TODO
    }
}
