package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.audit.enums.AuditAction;
import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.audit.services.ScopedAuditService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.openApi.OpenApiConstants;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.dtos.ProcessInstanceReassignRequestDTO;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.entities.VUserProcessInstanceAccessPermissionsEntity;
import de.aivot.prosuna.backend.process.filters.ProcessInstanceFilter;
import de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider;
import de.aivot.prosuna.backend.process.services.ProcessInstanceService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceTaskService;
import de.aivot.prosuna.backend.process.services.ProcessNodeExecutionLoggerFactory;
import de.aivot.prosuna.backend.process.workers.ProcessWorker;
import de.aivot.prosuna.backend.user.services.UserService;
import de.aivot.prosuna.backend.utils.StringUtils;
import de.aivot.prosuna.backend.utils.specification.SpecificationBuilderArrayContains;
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
    // Process instances are created by trigger controllers and changed only through explicit domain commands.
    // Generic POST and PUT endpoints are intentionally not exposed because their runtime state is engine-owned.
    private final ScopedAuditService auditService;
    private final UserService userService;
    private final ProcessInstanceService processInstanceService;
    private final ProcessInstanceTaskService processInstanceTaskService;
    private final RabbitTemplate rabbitTemplate;
    private final ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory;
    private final PermissionService permissionService;

    @Autowired
    public ProcessInstanceController(AuditService auditService,
                                     UserService userService,
                                     ProcessInstanceService processInstanceService,
                                     ProcessInstanceTaskService processInstanceTaskService,
                                     RabbitTemplate rabbitTemplate,
                                     ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory,
                                     PermissionService permissionService) {
        this.auditService = auditService.createScopedAuditService(ProcessInstanceController.class, "Prozesse");
        this.userService = userService;
        this.processInstanceService = processInstanceService;
        this.processInstanceTaskService = processInstanceTaskService;
        this.rabbitTemplate = rabbitTemplate;
        this.processNodeExecutionLoggerFactory = processNodeExecutionLoggerFactory;
        this.permissionService = permissionService;
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

        if (!permissionService.hasSystemPermission(execUser.getId(), ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ)) {
            // Keep all caller filters and add an EXISTS guard against the resolved process-instance permission view.
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
                                ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ
                        ))
                );

                return criteriaBuilder.exists(subquery);
            });
        }

        return processInstanceService
                .list(pageable, filter);
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Process Instance",
            description = "Retrieve a process instance by its ID."
    )
    public ProcessInstanceEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Long id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var instance = processInstanceService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        permissionService.requireProcessInstancePermission(
                user.getId(),
                instance.getId(),
                ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ
        );

        return instance;
    }

    @PutMapping("{id}/reassign/")
    @Operation(
            summary = "Reassign Process Instance",
            description = "Assign an existing process instance to another user or clear its assignment."
    )
    public ProcessInstanceEntity reassign(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Long id,
            @Nonnull @RequestBody @Valid ProcessInstanceReassignRequestDTO request
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var existing = processInstanceService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        permissionService.requireProcessInstancePermission(
                execUser.getId(),
                existing.getId(),
                ProcessInstancePermissionProvider.PROCESS_INSTANCE_REASSIGN
        );

        if (request.assignedUserId() != null && userService.retrieve(request.assignedUserId()).isEmpty()) {
            throw ResponseException.badRequest("Die zuzuweisende Mitarbeiter:in wurde nicht gefunden.");
        }

        var result = processInstanceService.save(
                existing
                        .setAssignedUserId(request.assignedUserId())
                        .setUpdated(Instant.now())
        );

        auditService.create().withUser(execUser).withAuditAction(AuditAction.Update, ProcessInstanceEntity.class, result.getId(), "id", Map.of(
                "id", result.getId(),
                "processDefinitionId", result.getProcessId()
        )).withMessage(
                "Die Prozessinstanz mit der ID %s für den Prozess %s wurde von der Mitarbeiter:in %s neu zugewiesen.",
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
                .orElseThrow(ResponseException::unauthorized);

        var processInstance = processInstanceService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        permissionService.requireProcessInstancePermission(
                user.getId(),
                processInstance.getId(),
                ProcessInstancePermissionProvider.PROCESS_INSTANCE_UPDATE
        );

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
            description = "Delete a process instance by its ID. Requires the permission `" +
                    ProcessInstancePermissionProvider.PROCESS_INSTANCE_DELETE +
                    "` for the affected process instance or at system level."
    )
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Long id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService.requireProcessInstancePermission(
                user.getId(),
                id,
                ProcessInstancePermissionProvider.PROCESS_INSTANCE_DELETE
        );

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
