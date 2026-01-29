package de.aivot.GoverBackend.process.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.permissions.data.Permissions;
import de.aivot.GoverBackend.permissions.services.PermissionService;
import de.aivot.GoverBackend.department.services.DepartmentService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.process.entities.ProcessEntity;
import de.aivot.GoverBackend.process.filters.ProcessFilter;
import de.aivot.GoverBackend.process.repositories.ProcessVersionRepository;
import de.aivot.GoverBackend.process.services.*;
import de.aivot.GoverBackend.teams.entities.TeamEntity;
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

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/processes/")
@Tag(
        name = OpenApiConstants.Tags.ProcessesDefinitionsName,
        description = OpenApiConstants.Tags.ProcessesDefinitionsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessController {
    private final ScopedAuditService auditService;
    private final UserService userService;
    private final ProcessService processDefinitionService;
    private final DepartmentService departmentService;
    private final PermissionService permissionService;
    private final ProcessExportService processExportService;
    private final ProcessVersionRepository processDefinitionVersionRepository;
    private final ProcessVersionService processDefinitionVersionService;
    private final ProcessNodeService processDefinitionNodeService;
    private final ProcessEdgeService processDefinitionEdgeService;
    private final ProcessNodeDefinitionService processNodeProviderService;

    @Autowired
    public ProcessController(AuditService auditService,
                             UserService userService,
                             ProcessService processDefinitionService,
                             DepartmentService departmentService,
                             PermissionService permissionService,
                             ProcessExportService processExportService,
                             ProcessVersionRepository processDefinitionVersionRepository,
                             ProcessVersionService processDefinitionVersionService,
                             ProcessNodeService processDefinitionNodeService,
                             ProcessEdgeService processDefinitionEdgeService,
                             ProcessNodeDefinitionService processNodeProviderService) {
        this.auditService = auditService.createScopedAuditService(ProcessController.class);

        this.userService = userService;
        this.processDefinitionService = processDefinitionService;
        this.departmentService = departmentService;
        this.permissionService = permissionService;
        this.processExportService = processExportService;
        this.processDefinitionVersionRepository = processDefinitionVersionRepository;
        this.processDefinitionVersionService = processDefinitionVersionService;
        this.processDefinitionNodeService = processDefinitionNodeService;
        this.processDefinitionEdgeService = processDefinitionEdgeService;
        this.processNodeProviderService = processNodeProviderService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Process Definitions",
            description = "List all process definitions with optional filtering and pagination."
    )
    public Page<ProcessEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid ProcessFilter filter
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return processDefinitionService
                .listAllByAccessibleForUser(
                        pageable,
                        execUser.getId(),
                        filter.build()
                );
    }

    @PostMapping("")
    @Operation(
            summary = "Create Process Definition",
            description = "Create a new process definition. " +
                    "Requires super admin privileges or a user role with create process permissions."
    )
    public ProcessEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid ProcessEntity newProcessDefinition
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var department = departmentService
                .retrieve(newProcessDefinition.getDepartmentId())
                .orElseThrow(ResponseException::badRequest);

        permissionService
                .testDomainPermission(
                        execUser.getId(),
                        department.getId(),
                        Permissions.PROCESS_DEFINITION_CREATE
                );

        var result = processDefinitionService
                .create(newProcessDefinition);

        auditService.logAction(execUser, AuditAction.Create, ProcessEntity.class, Map.of(
                "id", result.getId(),
                "name", result.getInternalTitle()
        ));

        return result;
    }

    @PostMapping("import/")
    @Operation(
            summary = "Import Process Definition",
            description = "Import a process definition from exported data. " +
                    "Requires super admin privileges or a user role with create process permissions."
    )
    public ProcessEntity importProc(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid ProcessExportService.ProcessExport processExport
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var exportData = processExport.data();

        var department = departmentService
                .retrieve(exportData.process().getDepartmentId())
                .orElseThrow(ResponseException::badRequest);

        permissionService.testDomainPermission(
                execUser.getId(),
                department.getId(),
                Permissions.PROCESS_DEFINITION_CREATE
        );

        var newProcess = processDefinitionService
                .create(exportData.process());

        var newVersion = processDefinitionVersionService
                .create(exportData
                        .version()
                        .setProcessId(newProcess.getId())
                );

        var savedNodeIdMap = new HashMap<Integer, Integer>();
        for (var node : exportData.nodes()) {
            var originalId = node.getId();

            var provider = processNodeProviderService
                    .getProcessNodeDefinition(node.getProcessNodeDefinitionKey(), node.getProcessNodeDefinitionVersion())
                    .orElseThrow(() -> ResponseException
                            .badRequest("Eine Prozesselementdefinition mit dem Schlüssel „%s“ und der Version „%d“ ist nicht verfügbar."
                                    .formatted(node.getProcessNodeDefinitionKey(), node.getProcessNodeDefinitionVersion())));

            var config = provider
                    .prefillConfigurationOnImport(node.getConfiguration());

            var addedNode = processDefinitionNodeService
                    .create(node
                            .setProcessId(newProcess.getId())
                            .setProcessVersion(newVersion.getProcessVersion())
                            .setConfiguration(config)
                    );

            savedNodeIdMap
                    .put(originalId, addedNode.getId());
        }

        for (var edge : exportData.edges()) {
            var translatedFromNodeId = savedNodeIdMap.get(edge.getFromNodeId());
            var translatedToNodeId = savedNodeIdMap.get(edge.getToNodeId());

            processDefinitionEdgeService
                    .create(edge
                            .setProcessId(newProcess.getId())
                            .setProcessVersion(newVersion.getProcessVersion())
                            .setFromNodeId(translatedFromNodeId)
                            .setToNodeId(translatedToNodeId)
                    );
        }

        auditService.logAction(execUser, AuditAction.Create, ProcessEntity.class, Map.of(
                "id", newProcess.getId(),
                "name", newProcess.getInternalTitle()
        ));

        return newProcess;
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Process Definition",
            description = "Retrieve a process definition by its ID."
    )
    public ProcessEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var proc = processDefinitionService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        permissionService.testDomainPermission(
                execUser.getId(),
                proc.getDepartmentId(),
                Permissions.PROCESS_DEFINITION_READ
        );

        return proc;
    }

    @PutMapping("{id}/")
    @Operation(
            summary = "Update Process Definition",
            description = "Update an existing process definition. " +
                    "Requires super admin privileges or a user role with edit process permissions."
    )
    public ProcessEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @RequestBody @Valid ProcessEntity updateDTO
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var existing = processDefinitionService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        permissionService.testDomainPermission(
                execUser.getId(),
                updateDTO.getDepartmentId(),
                Permissions.PROCESS_DEFINITION_UPDATE
        );

        updateDTO.setId(existing.getId());

        var result = processDefinitionService
                .update(id, updateDTO);

        auditService.logAction(execUser, AuditAction.Update, ProcessEntity.class, Map.of(
                "id", result.getId(),
                "name", result.getInternalTitle()
        ));

        return result;
    }

    @DeleteMapping("{id}/")
    @Operation(
            summary = "Delete Process Definition",
            description = "Delete a process definition by its ID. " +
                    "Requires super admin privileges."
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

        var deleted = processDefinitionService
                .delete(id);

        auditService.logAction(user, AuditAction.Delete, TeamEntity.class, Map.of(
                "id", deleted.getId(),
                "name", deleted.getInternalTitle()
        ));
    }

    @GetMapping("{id}/export/latest/")
    @Operation(
            summary = "Export Latest Process Definition Version",
            description = "Export the latest version of a process definition. " +
                    "Requires read permissions for the process definition."
    )
    public ProcessExportService.ProcessExport exportVersion(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var latestVersion = processDefinitionVersionRepository
                .maxVersionForProcessDefinition(id)
                .orElseThrow(ResponseException::notFound);

        return exportVersion(jwt, id, latestVersion);
    }


    @GetMapping("{id}/export/{version}/")
    @Operation(
            summary = "Export Specific Process Definition Version",
            description = "Export a specific version of a process definition. " +
                    "Requires read permissions for the process definition."
    )
    public ProcessExportService.ProcessExport exportVersion(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @PathVariable Integer version
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var existing = processDefinitionService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        permissionService.testDomainPermission(
                execUser.getId(),
                existing.getDepartmentId(),
                Permissions.PROCESS_DEFINITION_READ
        );

        var result = processExportService
                .export(id, version);

        auditService.logAction(execUser, AuditAction.Retrieve, ProcessEntity.class, Map.of(
                "id", existing.getId(),
                "name", existing.getInternalTitle(),
                "export", true
        ));

        return result;
    }
}
