package de.aivot.gover.backend.process.controllers;

import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.openApi.OpenApiConstants;
import de.aivot.gover.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.gover.backend.process.entities.ProcessVersionEntityId;
import de.aivot.gover.backend.process.enums.ProcessInstanceStatus;
import de.aivot.gover.backend.process.enums.ProcessTaskStatus;
import de.aivot.gover.backend.process.filters.ProcessInstanceFilter;
import de.aivot.gover.backend.process.filters.ProcessInstanceTaskFilter;
import de.aivot.gover.backend.process.services.ProcessInstanceService;
import de.aivot.gover.backend.process.services.ProcessInstanceTaskService;
import de.aivot.gover.backend.process.services.ProcessVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/public/processes/{procAccess}/")
@Tag(
        name = OpenApiConstants.Tags.ProcessesDefinitionsName,
        description = "Operations for managing process instance tasks."
)
public class CitizenProcessInstanceViewController {
    private final ProcessInstanceService processInstanceService;
    private final ProcessInstanceTaskService processInstanceTaskService;
    private final ProcessVersionService processVersionService;

    public CitizenProcessInstanceViewController(ProcessInstanceService processInstanceService,
                                                ProcessInstanceTaskService processInstanceTaskService,
                                                ProcessVersionService processVersionService) {
        this.processInstanceService = processInstanceService;
        this.processInstanceTaskService = processInstanceTaskService;
        this.processVersionService = processVersionService;
    }

    @GetMapping("")
    @Operation(
            summary = "Retrieve Process Instance Task View Layout",
            description = "Retrieves the view layout for a specific task within a process instance. " +
                    "The layout defines how the task is presented to the user, including form fields and structure."
    )
    public ProcessInstanceStatusResponse retrieve(
            @Nonnull @PathVariable UUID procAccess
    ) throws ResponseException {
        var instanceFilter = new ProcessInstanceFilter()
                .setAccessKey(procAccess);

        var instance = processInstanceService
                .retrieve(instanceFilter)
                .orElseThrow(ResponseException::notFound);

        var processVersion = processVersionService
                .retrieve(ProcessVersionEntityId.of(instance.getProcessId(), instance.getInitialProcessVersion()))
                .orElseThrow(ResponseException::notFound);

        var taskFilter = new ProcessInstanceTaskFilter()
                .setProcessInstanceId(instance.getId())
                .setAnyStatus(List.of(
                        ProcessTaskStatus.AwaitingPayment,
                        ProcessTaskStatus.AwaitingCustomer
                ));

        var taskPagination = Pageable
                .unpaged(Sort.by(Sort.Direction.ASC, "started"));

        var tasks = processInstanceTaskService
                .list(taskPagination, taskFilter)
                .map(ProcessInstanceTaskEntity::getAccessKey)
                .toList();

        return new ProcessInstanceStatusResponse(
                processVersion.getPublicTitle(),
                instance.getStatus(),
                tasks
        );
    }

    public record ProcessInstanceStatusResponse(
            @Nonnull
            String title,
            @Nonnull
            ProcessInstanceStatus status,
            @Nullable
            List<UUID> currentTasks
    ) {
    }
}
