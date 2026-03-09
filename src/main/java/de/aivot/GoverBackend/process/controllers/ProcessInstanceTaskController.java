package de.aivot.GoverBackend.process.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.department.services.DepartmentService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.permissions.services.PermissionService;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import de.aivot.GoverBackend.process.filters.ProcessInstanceTaskFilter;
import de.aivot.GoverBackend.process.services.ProcessInstanceTaskService;
import de.aivot.GoverBackend.process.services.ProcessService;
import de.aivot.GoverBackend.process.workers.ProcessWorker;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.userRoles.data.PermissionLabels;
import de.aivot.GoverBackend.utils.StringUtils;
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

import java.util.Map;

@RestController
@RequestMapping("/api/process-instance-tasks/")
@Tag(
        name = OpenApiConstants.Tags.ProcessesDefinitionsName,
        description = "Operations for managing process instance tasks."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessInstanceTaskController {
    private final ScopedAuditService auditService;
    private final UserService userService;
    private final ProcessInstanceTaskService processInstanceTaskService;
    private final DepartmentService departmentService;
    private final ProcessService processDefinitionService;
    private final RabbitTemplate rabbitTemplate;
    private final PermissionService permissionService;

    @Autowired
    public ProcessInstanceTaskController(AuditService auditService,
                                         UserService userService,
                                         ProcessInstanceTaskService processInstanceTaskService,
                                         DepartmentService departmentService,
                                         ProcessService processDefinitionService,
                                         RabbitTemplate rabbitTemplate,
                                         PermissionService permissionService) {
        this.auditService = auditService.createScopedAuditService(ProcessInstanceTaskController.class, "Prozesse");
        this.userService = userService;
        this.processInstanceTaskService = processInstanceTaskService;
        this.departmentService = departmentService;
        this.processDefinitionService = processDefinitionService;
        this.rabbitTemplate = rabbitTemplate;
        this.permissionService = permissionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Process Instance Tasks",
            description = "List all process instance tasks with optional filtering and pagination."
    )
    public Page<ProcessInstanceTaskEntity> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid ProcessInstanceTaskFilter filter
    ) throws ResponseException {
        return processInstanceTaskService
                .list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Process Instance Task",
            description = "Create a new process instance task. Requires super admin privileges or a user role with create process permissions."
    )
    public ProcessInstanceTaskEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid ProcessInstanceTaskEntity newTask
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var processDefinition = processDefinitionService
                .retrieve(newTask.getProcessId())
                .orElseThrow(ResponseException::badRequest);

        var department = departmentService
                .retrieve(processDefinition.getDepartmentId())
                .orElseThrow(ResponseException::badRequest);

        permissionService
                .hasDepartmentPermissionThrows(
                        execUser.getId(),
                        department.getId(),
                        PermissionLabels.ProcessPermissionCreate
                );

        var result = processInstanceTaskService
                .create(newTask);

        auditService.create().withUser(execUser).withAuditAction(AuditAction.Create, ProcessInstanceTaskEntity.class, result.getId(), "id", Map.of(
                "id", result.getId(),
                "processInstanceId", result.getProcessInstanceId(),
                "processDefinitionId", result.getProcessId(),
                "processDefinitionVersion", result.getProcessVersion()
        )).withMessage(
                "Die Instanzaufgabe mit der ID %s für die Prozessinstanz %s (Prozess %s, Version %s) wurde von der Mitarbeiter:in %s erstellt.",
                StringUtils.quote(String.valueOf(result.getId())),
                StringUtils.quote(String.valueOf(result.getProcessInstanceId())),
                StringUtils.quote(String.valueOf(result.getProcessId())),
                StringUtils.quote(String.valueOf(result.getProcessVersion())),
                StringUtils.quote(execUser.getFullName())
        ).log();

        return result;
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Process Instance Task",
            description = "Retrieve a process instance task by its ID."
    )
    public ProcessInstanceTaskEntity retrieve(
            @Nonnull @PathVariable Long id
    ) throws ResponseException {
        return processInstanceTaskService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    @Operation(
            summary = "Update Process Instance Task",
            description = "Update an existing process instance task. Requires super admin privileges or a user role with edit process permissions."
    )
    public ProcessInstanceTaskEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Long id,
            @Nonnull @RequestBody @Valid ProcessInstanceTaskEntity updateDTO
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var existing = processInstanceTaskService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        var processDefinition = processDefinitionService
                .retrieve(existing.getProcessId())
                .orElseThrow(ResponseException::badRequest);

        var department = departmentService
                .retrieve(processDefinition.getDepartmentId())
                .orElseThrow(ResponseException::badRequest);

        permissionService
                .hasDepartmentPermissionThrows(
                        execUser.getId(),
                        department.getId(),
                        PermissionLabels.ProcessPermissionCreate
                );

        updateDTO.setId(existing.getId());

        var result = processInstanceTaskService
                .update(id, updateDTO);

        auditService.create().withUser(execUser).withAuditAction(AuditAction.Update, ProcessInstanceTaskEntity.class, result.getId(), "id", Map.of(
                "id", result.getId(),
                "processInstanceId", result.getProcessInstanceId(),
                "processDefinitionId", result.getProcessId(),
                "processDefinitionVersion", result.getProcessVersion()
        )).withMessage(
                "Die Instanzaufgabe mit der ID %s für die Prozessinstanz %s (Prozess %s, Version %s) wurde von der Mitarbeiter:in %s aktualisiert.",
                StringUtils.quote(String.valueOf(result.getId())),
                StringUtils.quote(String.valueOf(result.getProcessInstanceId())),
                StringUtils.quote(String.valueOf(result.getProcessId())),
                StringUtils.quote(String.valueOf(result.getProcessVersion())),
                StringUtils.quote(execUser.getFullName())
        ).log();

        return result;
    }

    @DeleteMapping("{id}/")
    @Operation(
            summary = "Delete Process Instance Task",
            description = "Delete a process instance task by its ID. Requires super admin privileges."
    )
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Long id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSuperAdmin()
                .orElseThrow(ResponseException::forbidden);

        var deleted = processInstanceTaskService
                .delete(id);

        auditService.create().withUser(user).withAuditAction(AuditAction.Delete, ProcessInstanceTaskEntity.class, deleted.getId(), "id", Map.of(
                "id", deleted.getId(),
                "processInstanceId", deleted.getProcessInstanceId(),
                "processDefinitionId", deleted.getProcessId(),
                "processDefinitionVersion", deleted.getProcessVersion()
        )).withMessage(
                "Die Instanzaufgabe mit der ID %s für die Prozessinstanz %s (Prozess %s, Version %s) wurde von der Mitarbeiter:in %s gelöscht.",
                StringUtils.quote(String.valueOf(deleted.getId())),
                StringUtils.quote(String.valueOf(deleted.getProcessInstanceId())),
                StringUtils.quote(String.valueOf(deleted.getProcessId())),
                StringUtils.quote(String.valueOf(deleted.getProcessVersion())),
                StringUtils.quote(user.getFullName())
        ).log();
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

        userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSuperAdmin()
                .orElseThrow(ResponseException::noSuperAdminPermission);

        var payload = new ProcessWorker.WorkerPayload(
                taskEntity.getProcessInstanceId(),
                taskEntity.getPreviousProcessNodeId(),
                taskEntity.getProcessNodeId()
        );

        rabbitTemplate.convertAndSend(ProcessWorker.DO_WORK_ON_INSTANCE_QUEUE, payload);

        return taskEntity;
    }
}

