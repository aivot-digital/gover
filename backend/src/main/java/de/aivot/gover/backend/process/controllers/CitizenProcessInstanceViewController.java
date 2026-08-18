package de.aivot.gover.backend.process.controllers;

import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.openApi.OpenApiConstants;
import de.aivot.gover.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.gover.backend.process.entities.ProcessVersionEntityId;
import de.aivot.gover.backend.process.enums.ProcessInstanceStatus;
import de.aivot.gover.backend.process.enums.ProcessTaskStatus;
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
            @Nonnull @PathVariable String procAccess
    ) throws ResponseException {
        var instance = processInstanceService
                .retrieveByAccessKey(procAccess)
                .orElseThrow(ResponseException::notFound);

        var processVersion = processVersionService
                .retrieve(ProcessVersionEntityId.of(instance.getProcessId(), instance.getInitialProcessVersion()))
                .orElseThrow(ResponseException::notFound);

        var taskFilter = new ProcessInstanceTaskFilter()
                .setProcessInstanceId(instance.getId());

        var taskPagination = Pageable
                .unpaged(Sort.by(Sort.Direction.ASC, "started"));

        var tasks = processInstanceTaskService
                .list(taskPagination, taskFilter)
                .map(ProcessInstanceTaskStatusResponse::of)
                .toList();

        return new ProcessInstanceStatusResponse(
                processVersion.getPublicTitle(),
                instance.getStatus(),
                instance.getStatusOverride(),
                tasks
        );
    }

    public record ProcessInstanceStatusResponse(
            @Nonnull
            String title,
            @Nonnull
            ProcessInstanceStatus status,
            @Nullable
            String statusOverride,
            @Nullable
            List<ProcessInstanceTaskStatusResponse> tasks
    ) {
    }

    public record  ProcessInstanceTaskStatusResponse(
            @Nonnull
            String accessKey,
            @Nonnull
            ProcessTaskStatus status,
            @Nullable
            String statusOverride
    ) {
        public static ProcessInstanceTaskStatusResponse of(ProcessInstanceTaskEntity task) {
            return new ProcessInstanceTaskStatusResponse(
                    task.getAccessKey(),
                    task.getStatus(),
                    task.getStatusOverride()
            );
        }
    }
}
