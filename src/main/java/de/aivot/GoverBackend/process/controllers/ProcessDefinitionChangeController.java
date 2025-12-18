package de.aivot.GoverBackend.process.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.core.services.PermissionService;
import de.aivot.GoverBackend.department.services.DepartmentService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.process.entities.ProcessDefinitionChangeEntity;
import de.aivot.GoverBackend.process.filters.ProcessDefinitionChangeFilter;
import de.aivot.GoverBackend.process.services.ProcessDefinitionChangeService;
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
@RequestMapping("/api/process-definition-changes/")
@Tag(
        name = OpenApiConstants.Tags.ProcessesDefinitionsName,
        description = "Operations for managing process definition changes."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessDefinitionChangeController {
    private final ScopedAuditService auditService;
    private final UserService userService;
    private final ProcessDefinitionChangeService processDefinitionChangeService;
    private final DepartmentService departmentService;
    private final ProcessDefinitionService processDefinitionService;
    private final PermissionService permissionService;

    @Autowired
    public ProcessDefinitionChangeController(AuditService auditService,
                                             UserService userService,
                                             ProcessDefinitionChangeService processDefinitionChangeService,
                                             DepartmentService departmentService,
                                             ProcessDefinitionService processDefinitionService, PermissionService permissionService) {
        this.auditService = auditService.createScopedAuditService(ProcessDefinitionChangeController.class);
        this.userService = userService;
        this.processDefinitionChangeService = processDefinitionChangeService;
        this.departmentService = departmentService;
        this.processDefinitionService = processDefinitionService;
        this.permissionService = permissionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Process Definition Changes",
            description = "List all process definition changes with optional filtering and pagination."
    )
    public Page<ProcessDefinitionChangeEntity> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid ProcessDefinitionChangeFilter filter
    ) throws ResponseException {
        return processDefinitionChangeService
                .list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Process Definition Change",
            description = "Create a new process definition change. Requires super admin privileges or a user role with create process permissions."
    )
    public ProcessDefinitionChangeEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid ProcessDefinitionChangeEntity newChange
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var processDefinition = processDefinitionService
                .retrieve(newChange.getProcessDefinitionId())
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

        var result = processDefinitionChangeService
                .create(newChange);

        auditService.logAction(execUser, AuditAction.Create, ProcessDefinitionChangeEntity.class, Map.of(
                "id", result.getId(),
                "processDefinitionId", result.getProcessDefinitionId(),
                "processDefinitionVersion", result.getProcessDefinitionVersion()
        ));

        return result;
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Process Definition Change",
            description = "Retrieve a process definition change by its ID."
    )
    public ProcessDefinitionChangeEntity retrieve(
            @Nonnull @PathVariable Long id
    ) throws ResponseException {
        return processDefinitionChangeService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    @Operation(
            summary = "Update Process Definition Change",
            description = "Update an existing process definition change. Requires super admin privileges or a user role with edit process permissions."
    )
    public ProcessDefinitionChangeEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Long id,
            @Nonnull @RequestBody @Valid ProcessDefinitionChangeEntity updateDTO
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var existing = processDefinitionChangeService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        var processDefinition = processDefinitionService
                .retrieve(existing.getProcessDefinitionId())
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

        var result = processDefinitionChangeService
                .update(id, updateDTO);

        auditService.logAction(execUser, AuditAction.Update, ProcessDefinitionChangeEntity.class, Map.of(
                "id", result.getId(),
                "processDefinitionId", result.getProcessDefinitionId(),
                "processDefinitionVersion", result.getProcessDefinitionVersion()
        ));

        return result;
    }

    @DeleteMapping("{id}/")
    @Operation(
            summary = "Delete Process Definition Change",
            description = "Delete a process definition change by its ID. Requires super admin privileges."
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

        var deleted = processDefinitionChangeService
                .delete(id);

        auditService.logAction(user, AuditAction.Delete, ProcessDefinitionChangeEntity.class, Map.of(
                "id", deleted.getId(),
                "processDefinitionId", deleted.getProcessDefinitionId(),
                "processDefinitionVersion", deleted.getProcessDefinitionVersion()
        ));
    }
}

