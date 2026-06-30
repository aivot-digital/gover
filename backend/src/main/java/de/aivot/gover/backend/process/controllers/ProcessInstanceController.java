package de.aivot.gover.backend.process.controllers;

import de.aivot.gover.backend.audit.enums.AuditAction;
import de.aivot.gover.backend.audit.services.AuditService;
import de.aivot.gover.backend.audit.services.ScopedAuditService;
import de.aivot.gover.backend.department.services.DepartmentService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.openApi.OpenApiConfiguration;
import de.aivot.gover.backend.openApi.OpenApiConstants;
import de.aivot.gover.backend.process.entities.ProcessInstanceEntity;
import de.aivot.gover.backend.process.enums.ProcessInstanceStatus;
import de.aivot.gover.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.gover.backend.process.enums.ProcessTaskStatus;
import de.aivot.gover.backend.process.entities.VUserProcessInstanceAccessPermissionsEntity;
import de.aivot.gover.backend.process.filters.ProcessInstanceFilter;
import de.aivot.gover.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.gover.backend.process.services.ProcessInstanceService;
import de.aivot.gover.backend.process.services.ProcessInstanceTaskService;
import de.aivot.gover.backend.process.services.ProcessNodeExecutionLoggerFactory;
import de.aivot.gover.backend.process.services.ProcessService;
import de.aivot.gover.backend.process.workers.ProcessWorker;
import de.aivot.gover.backend.user.services.UserService;
import de.aivot.gover.backend.utils.StringUtils;
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
import java.util.Map;

@RestController
@RequestMapping("/api/process-instances/")
@Tag(
        name = OpenApiConstants.Tags.ProcessesDefinitionsName,
        description = "Operations for managing process instances."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessInstanceController {
    private final ScopedAuditService auditService;
    private final UserService userService;
    private final ProcessInstanceService processInstanceService;
    private final ProcessInstanceTaskService processInstanceTaskService;
    private final DepartmentService departmentService;
    private final ProcessService processDefinitionService;
    private final RabbitTemplate rabbitTemplate;
    private final ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory;

    @Autowired
    public ProcessInstanceController(AuditService auditService,
                                     UserService userService,
                                     ProcessInstanceService processInstanceService,
                                     DepartmentService departmentService,
                                     ProcessService processDefinitionService,
                                     ProcessInstanceTaskService processInstanceTaskService,
                                     RabbitTemplate rabbitTemplate,
                                     ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory) {
        this.auditService = auditService.createScopedAuditService(ProcessInstanceController.class, "Prozesse");
        this.userService = userService;
        this.processInstanceService = processInstanceService;
        this.processInstanceTaskService = processInstanceTaskService;
        this.departmentService = departmentService;
        this.processDefinitionService = processDefinitionService;
        this.rabbitTemplate = rabbitTemplate;
        this.processNodeExecutionLoggerFactory = processNodeExecutionLoggerFactory;
    }

    @GetMapping("")
    @Operation(
            summary = "List Process Instances",
            description = "List all process instances with optional filtering and pagination."
    )
    public Page<ProcessInstanceEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid ProcessInstanceFilter filter
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        filter.addAdditionalSpecification((root, query, criteriaBuilder) -> {
            var subquery = query.subquery(VUserProcessInstanceAccessPermissionsEntity.class);
            var processRoot = subquery.from(VUserProcessInstanceAccessPermissionsEntity.class);

            subquery.select(processRoot).where(
                    criteriaBuilder.equal(processRoot.get("targetProcessInstanceId"), root.get("id")),
                    criteriaBuilder.equal(processRoot.get("userId"), execUser.getId()),
                    criteriaBuilder.isTrue(SpecificationBuilderArrayContains.getFunc(
                            criteriaBuilder,
                            processRoot,
                            "permissions",
                            ProcessPermissionProvider.PROCESS_INSTANCE_READ
                    ))
            );

            return criteriaBuilder.exists(subquery);
        });

        return processInstanceService
                .list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Process Instance",
            description = "Create a new process instance. Requires super admin privileges or a user role with create process permissions."
    )
    public ProcessInstanceEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid ProcessInstanceEntity newInstance
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var result = processInstanceService
                .create(newInstance);

        auditService.create().withUser(execUser).withAuditAction(AuditAction.Create, ProcessInstanceEntity.class, result.getId(), "id", Map.of(
                "id", result.getId(),
                "processDefinitionId", result.getProcessId()
        )).withMessage(
                "Die Prozessinstanz mit der ID %s für den Prozess %s wurde von der Mitarbeiter:in %s erstellt.",
                StringUtils.quote(String.valueOf(result.getId())),
                StringUtils.quote(String.valueOf(result.getProcessId())),
                StringUtils.quote(execUser.getFullName())
        ).log();

        return result;
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Process Instance",
            description = "Retrieve a process instance by its ID."
    )
    public ProcessInstanceEntity retrieve(
            @Nonnull @PathVariable Long id
    ) throws ResponseException {
        return processInstanceService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    @Operation(
            summary = "Update Process Instance",
            description = "Update an existing process instance. Requires super admin privileges or a user role with edit process permissions."
    )
    public ProcessInstanceEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Long id,
            @Nonnull @RequestBody @Valid ProcessInstanceEntity updateDTO
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var existing = processInstanceService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        updateDTO.setId(existing.getId());

        var result = processInstanceService
                .update(id, updateDTO);

        auditService.create().withUser(execUser).withAuditAction(AuditAction.Update, ProcessInstanceEntity.class, result.getId(), "id", Map.of(
                "id", result.getId(),
                "processDefinitionId", result.getProcessId()
        )).withMessage(
                "Die Prozessinstanz mit der ID %s für den Prozess %s wurde von der Mitarbeiter:in %s aktualisiert.",
                StringUtils.quote(String.valueOf(result.getId())),
                StringUtils.quote(String.valueOf(result.getProcessId())),
                StringUtils.quote(execUser.getFullName())
        ).log();

        return result;
    }

    @PutMapping("{id}/restart-failed/")
    @Operation(
            summary = "Restart Failed Process Instance",
            description = "Restart a failed process instance when it has no tasks yet or when the latest task failed."
    )
    public ProcessInstanceEntity restartFailed(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Long id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSuperAdmin()
                .orElseThrow(ResponseException::noSuperAdminPermission);

        var processInstance = processInstanceService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        if (processInstance.getStatus() != ProcessInstanceStatus.Failed) {
            throw ResponseException.badRequest("Nur Vorgänge im Status 'Fehlgeschlagen' können neu gestartet werden.");
        }

        var latestTask = processInstanceTaskService
                .retrieveLatestForInstanceId(processInstance.getId())
                .orElse(null);

        var now = Instant.now();
        ProcessWorker.WorkerPayload payload;

        if (latestTask == null) {
            if (processInstance.getInitialPayload() == null || processInstance.getInitialPayload().isEmpty()) {
                throw ResponseException.conflict("Der Vorgang kann nicht neu gestartet werden, weil keine Initialdaten für einen erneuten Start vorliegen.");
            }

            payload = new ProcessWorker.WorkerPayload(
                    processInstance.getId(),
                    null,
                    null,
                    null,
                    processInstance.getInitialNodeId()
            );
        } else {
            if (latestTask.getStatus() != ProcessTaskStatus.Failed) {
                throw ResponseException.conflict("Der Vorgang kann derzeit nur neu gestartet werden, wenn die letzte Aufgabe fehlgeschlagen ist oder noch keine Aufgabe angelegt wurde.");
            }

            payload = new ProcessWorker.WorkerPayload(
                    latestTask.getProcessInstanceId(),
                    latestTask.getPreviousProcessInstanceTaskId(),
                    latestTask.getPreviousProcessNodeId(),
                    latestTask.getPreviousProcessNodePortKey(),
                    latestTask.getProcessNodeId()
            );
        }

        try {
            rabbitTemplate.convertAndSend(ProcessWorker.DO_WORK_ON_INSTANCE_QUEUE, payload);
        } catch (Exception e) {
            throw ResponseException.internalServerError("Der Vorgang konnte nicht zum Neustart eingeplant werden.", e);
        }

        processInstance
                .setStatus(ProcessInstanceStatus.Running)
                .setUpdated(now);
        var updatedInstance = processInstanceService.save(processInstance);

        if (latestTask != null) {
            latestTask
                    .setStatus(ProcessTaskStatus.Restarted)
                    .setUpdated(now);
            processInstanceTaskService.save(latestTask);
        }

        processNodeExecutionLoggerFactory
                .create(updatedInstance.getId(), latestTask != null ? latestTask.getId() : null, user.getId(), null)
                .logf(
                        ProcessNodeExecutionLogLevel.Info,
                        true,
                        true,
                        "Vorgang neu gestartet",
                        latestTask == null
                                ? "Der fehlgeschlagene Vorgang wurde manuell neu gestartet, bevor eine erste Aufgabe angelegt wurde."
                                : "Der fehlgeschlagene Vorgang wurde manuell neu gestartet. Die letzte fehlgeschlagene Aufgabe wird erneut ausgeführt."
                );

        auditService.create().withUser(user).withAuditAction(AuditAction.Update, ProcessInstanceEntity.class, updatedInstance.getId(), "id", Map.of(
                "id", updatedInstance.getId(),
                "processDefinitionId", updatedInstance.getProcessId()
        )).withMessage(
                "Die fehlgeschlagene Prozessinstanz mit der ID %s für den Prozess %s wurde von der Mitarbeiter:in %s neu gestartet.",
                StringUtils.quote(String.valueOf(updatedInstance.getId())),
                StringUtils.quote(String.valueOf(updatedInstance.getProcessId())),
                StringUtils.quote(user.getFullName())
        ).log();

        return updatedInstance;
    }

    @DeleteMapping("{id}/")
    @Operation(
            summary = "Delete Process Instance",
            description = "Delete a process instance by its ID. Requires super admin privileges."
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

        var deleted = processInstanceService
                .delete(id);

        auditService.create().withUser(user).withAuditAction(AuditAction.Delete, ProcessInstanceEntity.class, deleted.getId(), "id", Map.of(
                "id", deleted.getId(),
                "processDefinitionId", deleted.getProcessId()
        )).withMessage(
                "Die Prozessinstanz mit der ID %s für den Prozess %s wurde von der Mitarbeiter:in %s gelöscht.",
                StringUtils.quote(String.valueOf(deleted.getId())),
                StringUtils.quote(String.valueOf(deleted.getProcessId())),
                StringUtils.quote(user.getFullName())
        ).log();
    }
}
