package de.aivot.GoverBackend.process.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.department.filters.VDepartmentMembershipWithPermissionsFilter;
import de.aivot.GoverBackend.department.repositories.VDepartmentMembershipWithPermissionsRepository;
import de.aivot.GoverBackend.department.services.DepartmentService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.process.entities.ProcessInstanceHistoryEventEntity;
import de.aivot.GoverBackend.process.filters.ProcessInstanceHistoryEventFilter;
import de.aivot.GoverBackend.process.services.ProcessInstanceHistoryEventService;
import de.aivot.GoverBackend.process.services.ProcessDefinitionService;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.userRoles.data.PermissionLabels;
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
@RequestMapping("/api/process-instance-history-events/")
@Tag(
        name = OpenApiConstants.Tags.ProcessesDefinitionsName,
        description = "Operations for managing process instance history events."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessInstanceHistoryEventController {
    private final ScopedAuditService auditService;
    private final UserService userService;
    private final ProcessInstanceHistoryEventService processInstanceHistoryEventService;
    private final DepartmentService departmentService;
    private final VDepartmentMembershipWithPermissionsRepository vDepartmentMembershipWithPermissionsRepository;
    private final ProcessDefinitionService processDefinitionService;

    @Autowired
    public ProcessInstanceHistoryEventController(AuditService auditService,
                                                UserService userService,
                                                ProcessInstanceHistoryEventService processInstanceHistoryEventService,
                                                DepartmentService departmentService,
                                                VDepartmentMembershipWithPermissionsRepository vDepartmentMembershipWithPermissionsRepository,
                                                ProcessDefinitionService processDefinitionService) {
        this.auditService = auditService.createScopedAuditService(ProcessInstanceHistoryEventController.class);
        this.userService = userService;
        this.processInstanceHistoryEventService = processInstanceHistoryEventService;
        this.departmentService = departmentService;
        this.vDepartmentMembershipWithPermissionsRepository = vDepartmentMembershipWithPermissionsRepository;
        this.processDefinitionService = processDefinitionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Process Instance History Events",
            description = "List all process instance history events with optional filtering and pagination."
    )
    public Page<ProcessInstanceHistoryEventEntity> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid ProcessInstanceHistoryEventFilter filter
    ) throws ResponseException {
        return processInstanceHistoryEventService
                .list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Process Instance History Event",
            description = "Create a new process instance history event. Requires super admin privileges or a user role with create process permissions."
    )
    public ProcessInstanceHistoryEventEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid ProcessInstanceHistoryEventEntity newEvent
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Permission check logic can be added here if needed, similar to other controllers

        var result = processInstanceHistoryEventService
                .create(newEvent);

        auditService.logAction(execUser, AuditAction.Create, ProcessInstanceHistoryEventEntity.class, Map.of(
                "id", result.getId(),
                "processInstanceId", result.getProcessInstanceId(),
                "processInstanceTaskId", result.getProcessInstanceTaskId()
        ));

        return result;
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Process Instance History Event",
            description = "Retrieve a process instance history event by its ID."
    )
    public ProcessInstanceHistoryEventEntity retrieve(
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
    public ProcessInstanceHistoryEventEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Long id,
            @Nonnull @RequestBody @Valid ProcessInstanceHistoryEventEntity updateDTO
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

        auditService.logAction(execUser, AuditAction.Update, ProcessInstanceHistoryEventEntity.class, Map.of(
                "id", result.getId(),
                "processInstanceId", result.getProcessInstanceId(),
                "processInstanceTaskId", result.getProcessInstanceTaskId()
        ));

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

        auditService.logAction(user, AuditAction.Delete, ProcessInstanceHistoryEventEntity.class, Map.of(
                "id", deleted.getId(),
                "processInstanceId", deleted.getProcessInstanceId(),
                "processInstanceTaskId", deleted.getProcessInstanceTaskId()
        ));
    }
}

