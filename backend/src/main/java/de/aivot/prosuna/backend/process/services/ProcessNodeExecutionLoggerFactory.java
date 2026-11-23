package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceHistoryEventRepository;
import de.aivot.prosuna.backend.user.services.UserDeputyService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

@Service
public class ProcessNodeExecutionLoggerFactory {


    private final ProcessInstanceHistoryEventRepository processInstanceHistoryEventRepository;
    private final UserDeputyService deputyService;

    public ProcessNodeExecutionLoggerFactory(ProcessInstanceHistoryEventRepository processInstanceHistoryEventRepository,
                                             UserDeputyService deputyService) {
        this.processInstanceHistoryEventRepository = processInstanceHistoryEventRepository;
        this.deputyService = deputyService;
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

    /**
     * Logger for a staff member working on a task. If they are not the assignee but currently represent them, the
     * events record that they acted on behalf of the assignee.
     */
    public ProcessNodeExecutionLogger createForStaffTask(@Nonnull Long processInstanceId,
                                                         @Nonnull ProcessInstanceTaskEntity task,
                                                         @Nonnull String userId) {
        var assignedUserId = task.getAssignedUserId();
        var representedUserId = assignedUserId != null && !assignedUserId.equals(userId)
                && deputyService.isActiveDeputyFor(userId, assignedUserId) ? assignedUserId : null;
        return new ProcessNodeExecutionLogger(processInstanceId,
                task.getId(),
                userId,
                null,
                representedUserId,
                processInstanceHistoryEventRepository);
    }
}
