package de.aivot.GoverBackend.process.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.core.services.LegacyPermissionService;
import de.aivot.GoverBackend.department.services.DepartmentService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.process.entities.ProcessVersionEntity;
import de.aivot.GoverBackend.process.entities.ProcessVersionEntityId;
import de.aivot.GoverBackend.process.filters.ProcessVersionFilter;
import de.aivot.GoverBackend.process.services.ProcessService;
import de.aivot.GoverBackend.process.services.ProcessVersionService;
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
@RequestMapping("/api/process-versions/")
@Tag(
        name = OpenApiConstants.Tags.ProcessesDefinitionsName,
        description = OpenApiConstants.Tags.ProcessesDefinitionsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessVersionController {
    private final ScopedAuditService auditService;
    private final UserService userService;
    private final ProcessVersionService processDefinitionVersionService;
    private final DepartmentService departmentService;
    private final ProcessService processDefinitionService;
    private final LegacyPermissionService permissionService;

    @Autowired
    public ProcessVersionController(AuditService auditService,
                                    UserService userService,
                                    ProcessVersionService processDefinitionVersionService,
                                    DepartmentService departmentService,
                                    ProcessService processDefinitionService, LegacyPermissionService permissionService) {
        this.auditService = auditService.createScopedAuditService(ProcessVersionController.class);
        this.userService = userService;
        this.processDefinitionVersionService = processDefinitionVersionService;
        this.departmentService = departmentService;
        this.processDefinitionService = processDefinitionService;
        this.permissionService = permissionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Process Definition Versions",
            description = "List all process definition versions with optional filtering and pagination."
    )
    public Page<ProcessVersionEntity> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid ProcessVersionFilter filter
    ) throws ResponseException {
        return processDefinitionVersionService
                .list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Process Definition Version",
            description = "Create a new process definition version. Requires super admin privileges or a user role with create process permissions."
    )
    public ProcessVersionEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid ProcessVersionEntity newVersion
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Retrieve the process definition to get its department ID
        var processDefinition = processDefinitionService
                .retrieve(newVersion.getProcessId())
                .orElseThrow(ResponseException::badRequest);

        // Check department permission for the process definition this version belongs to
        var department = departmentService
                .retrieve(processDefinition.getDepartmentId())
                .orElseThrow(ResponseException::badRequest);

        permissionService
                .hasDepartmentPermissionThrows(
                        execUser.getId(),
                        department.getId(),
                        PermissionLabels.ProcessPermissionCreate
                );

        var result = processDefinitionVersionService
                .create(newVersion);

        auditService.logAction(execUser, AuditAction.Create, ProcessVersionEntity.class, Map.of(
                "processDefinitionId", result.getProcessId(),
                "processDefinitionVersion", result.getProcessVersion()
        ));

        return result;
    }

    @GetMapping("{processDefinitionId}/{processDefinitionVersion}/")
    @Operation(
            summary = "Retrieve Process Definition Version",
            description = "Retrieve a process definition version by its composite ID."
    )
    public ProcessVersionEntity retrieve(
            @Nonnull @PathVariable Integer processDefinitionId,
            @Nonnull @PathVariable Integer processDefinitionVersion
    ) throws ResponseException {
        var id = new ProcessVersionEntityId(processDefinitionId, processDefinitionVersion);
        return processDefinitionVersionService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{processDefinitionId}/{processDefinitionVersion}/")
    @Operation(
            summary = "Update Process Definition Version",
            description = "Update an existing process definition version. Requires super admin privileges or a user role with edit process permissions."
    )
    public ProcessVersionEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer processDefinitionId,
            @Nonnull @PathVariable Integer processDefinitionVersion,
            @Nonnull @RequestBody @Valid ProcessVersionEntity updateDTO
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var id = new ProcessVersionEntityId(processDefinitionId, processDefinitionVersion);

        // Retrieve existing version to get process definition ID
        var existing = processDefinitionVersionService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        // Check department permission for the process definition this version belongs to
        var department = departmentService
                .retrieve(existing.getProcessId())
                .orElseThrow(ResponseException::badRequest);

        permissionService
                .hasDepartmentPermissionThrows(
                        execUser.getId(),
                        department.getId(),
                        PermissionLabels.ProcessPermissionCreate
                );

        updateDTO.setProcessId(existing.getProcessId());
        updateDTO.setProcessVersion(existing.getProcessVersion());

        var result = processDefinitionVersionService
                .update(id, updateDTO);

        auditService.logAction(execUser, AuditAction.Update, ProcessVersionEntity.class, Map.of(
                "processDefinitionId", result.getProcessId(),
                "processDefinitionVersion", result.getProcessVersion()
        ));

        return result;
    }

    @DeleteMapping("{processDefinitionId}/{processDefinitionVersion}/")
    @Operation(
            summary = "Delete Process Definition Version",
            description = "Delete a process definition version by its composite ID. Requires super admin privileges."
    )
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer processDefinitionId,
            @Nonnull @PathVariable Integer processDefinitionVersion
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSuperAdmin()
                .orElseThrow(ResponseException::forbidden);

        var id = new ProcessVersionEntityId(processDefinitionId, processDefinitionVersion);

        var deleted = processDefinitionVersionService
                .delete(id);

        auditService.logAction(user, AuditAction.Delete, ProcessVersionEntity.class, Map.of(
                "processDefinitionId", deleted.getProcessId(),
                "processDefinitionVersion", deleted.getProcessVersion()
        ));
    }
}

