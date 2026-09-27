package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceHistoryEventRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

@Service
public class ProcessNodeExecutionLoggerFactory {


    private final ProcessInstanceHistoryEventRepository processInstanceHistoryEventRepository;

    public ProcessNodeExecutionLoggerFactory(ProcessInstanceHistoryEventRepository processInstanceHistoryEventRepository) {
        this.processInstanceHistoryEventRepository = processInstanceHistoryEventRepository;
    }

    public ProcessNodeExecutionLogger create(@Nonnull Long processInstanceId,
                                             @Nullable Long processInstanceTaskId,
                                             @Nullable String userId,
                                             @Nullable String identityId) {
        return new ProcessNodeExecutionLogger(processInstanceId,
                processInstanceTaskId,
                userId,
                identityId,
                processInstanceHistoryEventRepository);
    }
}
