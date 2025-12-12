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
import de.aivot.GoverBackend.process.entities.ProcessDefinitionEdgeEntity;
import de.aivot.GoverBackend.process.filters.ProcessDefinitionEdgeFilter;
import de.aivot.GoverBackend.process.services.ProcessDefinitionEdgeService;
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
@RequestMapping("/api/process-definition-edges/")
@Tag(
        name = OpenApiConstants.Tags.ProcessesDefinitionsName,
        description = OpenApiConstants.Tags.ProcessesDefinitionsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessDefinitionEdgeController {
    private final ScopedAuditService auditService;
    private final UserService userService;
    private final ProcessDefinitionEdgeService processDefinitionEdgeService;
    private final DepartmentService departmentService;
    private final VDepartmentMembershipWithPermissionsRepository vDepartmentMembershipWithPermissionsRepository;
    private final ProcessDefinitionService processDefinitionService;

    @Autowired
    public ProcessDefinitionEdgeController(AuditService auditService,
                                           UserService userService,
                                           ProcessDefinitionEdgeService processDefinitionEdgeService,
                                           DepartmentService departmentService,
                                           VDepartmentMembershipWithPermissionsRepository vDepartmentMembershipWithPermissionsRepository,
                                           ProcessDefinitionService processDefinitionService) {
        this.auditService = auditService.createScopedAuditService(ProcessDefinitionEdgeController.class);
        this.userService = userService;
        this.processDefinitionEdgeService = processDefinitionEdgeService;
        this.departmentService = departmentService;
        this.vDepartmentMembershipWithPermissionsRepository = vDepartmentMembershipWithPermissionsRepository;
        this.processDefinitionService = processDefinitionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Process Definition Edges",
            description = "List all process definition edges with optional filtering and pagination."
    )
    public Page<ProcessDefinitionEdgeEntity> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid ProcessDefinitionEdgeFilter filter
    ) throws ResponseException {
        return processDefinitionEdgeService
                .list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Process Definition Edge",
            description = "Create a new process definition edge. Requires super admin privileges or a user role with create process permissions."
    )
    public ProcessDefinitionEdgeEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid ProcessDefinitionEdgeEntity newEdge
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!execUser.getIsSuperAdmin()) {
            var processDefinition = processDefinitionService
                    .retrieve(newEdge.getProcessDefinitionId())
                    .orElseThrow(ResponseException::badRequest);

            var department = departmentService
                    .retrieve(processDefinition.getDepartmentId())
                    .orElseThrow(ResponseException::badRequest);

            var spec = VDepartmentMembershipWithPermissionsFilter
                    .create()
                    .setUserId(execUser.getId())
                    .setDepartmentId(department.getId())
                    .setProcessPermissionCreate(true)
                    .build();
            var hasPermission = vDepartmentMembershipWithPermissionsRepository
                    .exists(spec);
            if (!hasPermission) {
                throw ResponseException.noPermission(PermissionLabels.ProcessPermissionCreate);
            }
        }

        var result = processDefinitionEdgeService
                .create(newEdge);

        auditService.logAction(execUser, AuditAction.Create, ProcessDefinitionEdgeEntity.class, Map.of(
                "id", result.getId(),
                "processDefinitionId", result.getProcessDefinitionId(),
                "processDefinitionVersion", result.getProcessDefinitionVersion()
        ));

        return result;
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Process Definition Edge",
            description = "Retrieve a process definition edge by its ID."
    )
    public ProcessDefinitionEdgeEntity retrieve(
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        return processDefinitionEdgeService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    @Operation(
            summary = "Update Process Definition Edge",
            description = "Update an existing process definition edge. Requires super admin privileges or a user role with edit process permissions."
    )
    public ProcessDefinitionEdgeEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @RequestBody @Valid ProcessDefinitionEdgeEntity updateDTO
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var existing = processDefinitionEdgeService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        if (!execUser.getIsSuperAdmin()) {
            var processDefinition = processDefinitionService
                    .retrieve(existing.getProcessDefinitionId())
                    .orElseThrow(ResponseException::badRequest);

            var department = departmentService
                    .retrieve(processDefinition.getDepartmentId())
                    .orElseThrow(ResponseException::badRequest);

            var spec = VDepartmentMembershipWithPermissionsFilter
                    .create()
                    .setUserId(execUser.getId())
                    .setDepartmentId(department.getId())
                    .setProcessPermissionEdit(true)
                    .build();
            var hasPermission = vDepartmentMembershipWithPermissionsRepository
                    .exists(spec);
            if (!hasPermission) {
                throw ResponseException.noPermission(PermissionLabels.ProcessPermissionEdit);
            }
        }

        updateDTO.setId(existing.getId());

        var result = processDefinitionEdgeService
                .update(id, updateDTO);

        auditService.logAction(execUser, AuditAction.Update, ProcessDefinitionEdgeEntity.class, Map.of(
                "id", result.getId(),
                "processDefinitionId", result.getProcessDefinitionId(),
                "processDefinitionVersion", result.getProcessDefinitionVersion()
        ));

        return result;
    }

    @DeleteMapping("{id}/")
    @Operation(
            summary = "Delete Process Definition Edge",
            description = "Delete a process definition edge by its ID. Requires super admin privileges."
    )
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSuperAdmin()
                .orElseThrow(ResponseException::forbidden);

        var deleted = processDefinitionEdgeService
                .delete(id);

        auditService.logAction(user, AuditAction.Delete, ProcessDefinitionEdgeEntity.class, Map.of(
                "id", deleted.getId(),
                "processDefinitionId", deleted.getProcessDefinitionId(),
                "processDefinitionVersion", deleted.getProcessDefinitionVersion()
        ));
    }
}

