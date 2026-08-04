package de.aivot.gover.backend.process.controllers;

import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.openApi.OpenApiConfiguration;
import de.aivot.gover.backend.openApi.OpenApiConstants;
import de.aivot.gover.backend.permissions.services.PermissionService;
import de.aivot.gover.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.gover.backend.process.entities.VUserProcessInstanceAccessPermissionsEntity;
import de.aivot.gover.backend.process.enums.ProcessTaskStatus;
import de.aivot.gover.backend.process.filters.ProcessInstanceTaskFilter;
import de.aivot.gover.backend.process.permissions.ProcessInstancePermissionProvider;
import de.aivot.gover.backend.process.services.ProcessInstanceTaskService;
import de.aivot.gover.backend.process.workers.ProcessWorker;
import de.aivot.gover.backend.user.services.UserService;
import de.aivot.gover.backend.utils.specification.SpecificationBuilderArrayContains;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/process-instance-tasks/")
@Tag(
        name = OpenApiConstants.Tags.ProcessesDefinitionsName,
        description = "Operations for managing process instance tasks."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessInstanceTaskController {
    // Tasks are created and progressed by the process engine. Generic write endpoints are intentionally not exposed;
    // staff interactions use the task-view commands and the explicit failed-task restart below.
    private final UserService userService;
    private final ProcessInstanceTaskService processInstanceTaskService;
    private final RabbitTemplate rabbitTemplate;
    private final PermissionService permissionService;

    @Autowired
    public ProcessInstanceTaskController(UserService userService,
                                         ProcessInstanceTaskService processInstanceTaskService,
                                         RabbitTemplate rabbitTemplate,
                                         PermissionService permissionService) {
        this.userService = userService;
        this.processInstanceTaskService = processInstanceTaskService;
        this.rabbitTemplate = rabbitTemplate;
        this.permissionService = permissionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Process Instance Tasks",
            description = "List all process instance tasks with optional filtering and pagination."
    )
    public Page<ProcessInstanceTaskEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid ProcessInstanceTaskFilter filter
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!permissionService.hasSystemPermission(execUser.getId(), ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ)) {
            // Tasks inherit visibility from their owning process instance.
            filter.addAdditionalSpecification((root, query, criteriaBuilder) -> {
                var subquery = query.subquery(VUserProcessInstanceAccessPermissionsEntity.class);
                var processRoot = subquery.from(VUserProcessInstanceAccessPermissionsEntity.class);

                subquery.select(processRoot).where(
                        criteriaBuilder.equal(processRoot.get("targetProcessInstanceId"), root.get("processInstanceId")),
                        criteriaBuilder.equal(processRoot.get("userId"), execUser.getId()),
                        criteriaBuilder.isTrue(SpecificationBuilderArrayContains.getFunc(
                                criteriaBuilder,
                                processRoot,
                                "permissions",
                                ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ
                        ))
                );

                return criteriaBuilder.exists(subquery);
            });
        }

        return processInstanceTaskService
                .list(pageable, filter);
    }

    @GetMapping("assigned-count/")
    @Operation(
            summary = "Count Assigned Process Instance Tasks",
            description = "Returns the number of currently assigned running tasks for the authenticated user."
    )
    public Map<String, Long> countAssignedTasks(
            @Nullable @AuthenticationPrincipal Jwt jwt
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return Map.of(
                "count",
                processInstanceTaskService.countAssignedTasks(
                        execUser.getId(),
                        List.of(ProcessTaskStatus.Running)
                )
        );
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Process Instance Task",
            description = "Retrieve a process instance task by its ID."
    )
    public ProcessInstanceTaskEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Long id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var task = processInstanceTaskService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        permissionService.requireProcessInstancePermission(
                user.getId(),
                task.getProcessInstanceId(),
                ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ
        );

        return task;
    }

    @PutMapping("{id}/rerun-failed/")
    @Operation(
            summary = "Update Process Instance Task",
            description = "Update an existing process instance task. Requires super admin privileges or a user role with edit process permissions."
    )
    public ProcessInstanceTaskEntity rerunFailedTask(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Long id
    ) throws ResponseException {
        var taskEntity = processInstanceTaskService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        if (taskEntity.getStatus() != ProcessTaskStatus.Failed) {
            throw ResponseException.badRequest("Nur Aufgaben im Status 'Fehlgeschlagen' können erneut ausgeführt werden.");
        }

        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService.requireProcessInstancePermission(
                user.getId(),
                taskEntity.getProcessInstanceId(),
                ProcessInstancePermissionProvider.PROCESS_INSTANCE_EDIT_TASK
        );

        taskEntity
                .setStatus(ProcessTaskStatus.Restarted)
                .setUpdated(Instant.now());

        taskEntity = processInstanceTaskService
                .update(taskEntity.getId(), taskEntity);

        var payload = new ProcessWorker.WorkerPayload(
                taskEntity.getProcessInstanceId(),
                taskEntity.getPreviousProcessInstanceTaskId(),
                taskEntity.getPreviousProcessNodeId(),
                taskEntity.getPreviousProcessNodePortKey(),
                taskEntity.getProcessNodeId()
        );

        rabbitTemplate.convertAndSend(ProcessWorker.DO_WORK_ON_INSTANCE_QUEUE, payload);

        return taskEntity;
    }
}
