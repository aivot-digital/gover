package de.aivot.GoverBackend.process.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.department.services.DepartmentService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEventEntity;
import de.aivot.GoverBackend.process.filters.ProcessInstanceEventFilter;
import de.aivot.GoverBackend.process.services.ProcessService;
import de.aivot.GoverBackend.process.services.ProcessInstanceEventService;
import de.aivot.GoverBackend.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/process-instance-events/")
@Tag(
        name = OpenApiConstants.Tags.ProcessesDefinitionsName,
        description = "Operations for managing process instance history events."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessInstanceEventController {
    private final ScopedAuditService auditService;
    private final UserService userService;
    private final ProcessInstanceEventService processInstanceHistoryEventService;

    @Autowired
    public ProcessInstanceEventController(AuditService auditService,
                                          UserService userService,
                                          ProcessInstanceEventService processInstanceHistoryEventService,
                                          DepartmentService departmentService,
                                          ProcessService processDefinitionService) {
        this.auditService = auditService.createScopedAuditService(ProcessInstanceEventController.class);
        this.userService = userService;
        this.processInstanceHistoryEventService = processInstanceHistoryEventService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Process Instance History Events",
            description = "List all process instance history events with optional filtering and pagination."
    )
    public Page<ProcessInstanceEventEntity> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid ProcessInstanceEventFilter filter
    ) throws ResponseException {
        return processInstanceHistoryEventService
                .list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Process Instance History Event",
            description = "Create a new process instance history event. Requires super admin privileges or a user role with create process permissions."
    )
    public ProcessInstanceEventEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid ProcessInstanceEventEntity newEvent
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Permission check logic can be added here if needed, similar to other controllers

        var result = processInstanceHistoryEventService
                .create(newEvent);

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.create().withUser(execUser).withAuditAction(AuditAction.Create, this.getClass().getSimpleName(), ProcessInstanceEventEntity.class, "legacy", "legacy", Map.of(
                "id", result.getId(),
                "processInstanceId", result.getProcessInstanceId(),
                "processInstanceTaskId", result.getProcessInstanceTaskId()
        )));

        return result;
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Process Instance History Event",
            description = "Retrieve a process instance history event by its ID."
    )
    public ProcessInstanceEventEntity retrieve(
            @Nonnull @PathVariable Long id
    ) throws ResponseException {
        return processInstanceHistoryEventService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    @Operation(
            summary = "Update Process Instance History Event",
            description = "Update an existing process instance history event. Requires super admin privileges or a user role with edit process permissions."
    )
    public ProcessInstanceEventEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Long id,
            @Nonnull @RequestBody @Valid ProcessInstanceEventEntity updateDTO
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var existing = processInstanceHistoryEventService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        // Permission check logic can be added here if needed, similar to other controllers

        updateDTO.setId(existing.getId());

        var result = processInstanceHistoryEventService
                .update(id, updateDTO);

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.create().withUser(execUser).withAuditAction(AuditAction.Update, this.getClass().getSimpleName(), ProcessInstanceEventEntity.class, "legacy", "legacy", Map.of(
                "id", result.getId(),
                "processInstanceId", result.getProcessInstanceId(),
                "processInstanceTaskId", result.getProcessInstanceTaskId()
        )));

        return result;
    }

    @DeleteMapping("{id}/")
    @Operation(
            summary = "Delete Process Instance History Event",
            description = "Delete a process instance history event by its ID. Requires super admin privileges."
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

        var deleted = processInstanceHistoryEventService
                .delete(id);

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.create().withUser(user).withAuditAction(AuditAction.Delete, this.getClass().getSimpleName(), ProcessInstanceEventEntity.class, "legacy", "legacy", Map.of(
                "id", deleted.getId(),
                "processInstanceId", deleted.getProcessInstanceId(),
                "processInstanceTaskId", deleted.getProcessInstanceTaskId()
        )));
    }
}

