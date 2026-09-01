package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.audit.enums.AuditAction;
import de.aivot.prosuna.backend.audit.models.AuditLogPayload;
import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.audit.services.ScopedAuditService;
import de.aivot.prosuna.backend.department.services.DepartmentService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.openApi.OpenApiConstants;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntityId;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.filters.ProcessVersionFilter;
import de.aivot.prosuna.backend.process.models.ProcessVersionProblems;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.services.ProcessService;
import de.aivot.prosuna.backend.process.services.ProcessVersionService;
import de.aivot.prosuna.backend.user.services.UserService;
import de.aivot.prosuna.backend.utils.StringUtils;
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

import java.util.List;
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
    private final PermissionService permissionService;

    @Autowired
    public ProcessVersionController(AuditService auditService,
                                    UserService userService,
                                    ProcessVersionService processDefinitionVersionService,
                                    DepartmentService departmentService,
                                    ProcessService processDefinitionService,
                                    PermissionService permissionService) {
        this.auditService = auditService.createScopedAuditService(ProcessVersionController.class, "Prozesse");
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
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid ProcessVersionFilter filter
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!permissionService.hasSystemPermission(user.getId(), ProcessPermissionProvider.PROCESS_DEFINITION_READ)) {
            if (filter.getProcessId() != null) {
                permissionService.requireProcessPermission(
                        user.getId(),
                        filter.getProcessId(),
                        ProcessPermissionProvider.PROCESS_DEFINITION_READ
                );
            } else {
                var accessibleProcessIds = permissionService
                        .getProcessesWithPermission(user.getId(), ProcessPermissionProvider.PROCESS_DEFINITION_READ);

                if (filter.getProcessIds() != null) {
                    accessibleProcessIds = filter.getProcessIds()
                            .stream()
                            .filter(accessibleProcessIds::contains)
                            .toList();
                }

                if (accessibleProcessIds.isEmpty()) {
                    return Page.empty(pageable);
                }

                filter.setProcessIds(accessibleProcessIds);
            }
        }

        return processDefinitionVersionService
                .list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Process Definition Version",
            description = "Create a new process definition version. Requires the permission `" +
                    ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE +
                    "` for the affected process or at system level."
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

        permissionService
                .requireProcessPermission(
                        execUser.getId(),
                        processDefinition.getId(),
                        ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE
                );

        newVersion
                .setStatus(ProcessVersionStatus.Drafted)
                .setPublished(null)
                .setRevoked(null);

        var result = processDefinitionVersionService
                .create(newVersion);

        auditService.create()
                .withUser(execUser)
                .withAuditAction(AuditAction.Create, ProcessVersionEntity.class,
                        result.getProcessVersion(),
                        "processVersion",
                        Map.of(
                                "processId", result.getProcessId(),
                                "processVersion", result.getProcessVersion()
                        )).withMessage(
                        "Die Prozessversion %s für den Prozess %s wurde von der Mitarbeiter:in %s erstellt.",
                        StringUtils.quote(String.valueOf(result.getProcessVersion())),
                        StringUtils.quote(String.valueOf(result.getProcessId())),
                        StringUtils.quote(execUser.getFullName())
                ).log();

        return result;
    }

    @GetMapping("{processDefinitionId}/latest/")
    @Operation(
            summary = "Retrieve Latest Process Definition Version",
            description = "Retrieve the latest version of a process definition. Requires the permission `" +
                    ProcessPermissionProvider.PROCESS_DEFINITION_READ +
                    "` for the affected process or at system level."
    )
    public ProcessVersionEntity retrieveLatest(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer processDefinitionId
    ) throws ResponseException {
        // Get the latest version number for the form
        var latestVersion = processDefinitionVersionService
                .getLatestVersion(processDefinitionId)
                .orElseThrow(ResponseException::notFound)
                .getProcessVersion();

        return retrieve(jwt, processDefinitionId, latestVersion);
    }

    @GetMapping("{processDefinitionId}/{processDefinitionVersion}/")
    @Operation(
            summary = "Retrieve Process Definition Version",
            description = "Retrieve a process definition version by its composite ID."
    )
    public ProcessVersionEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer processDefinitionId,
            @Nonnull @PathVariable Integer processDefinitionVersion
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService.requireProcessPermission(
                user.getId(),
                processDefinitionId,
                ProcessPermissionProvider.PROCESS_DEFINITION_READ
        );

        var id = new ProcessVersionEntityId(processDefinitionId, processDefinitionVersion);
        return processDefinitionVersionService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{processDefinitionId}/{processDefinitionVersion}/")
    @Operation(
            summary = "Update Process Definition Version",
            description = "Update an existing process definition version. Requires the permission `" +
                    ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE +
                    "` for the affected process or at system level."
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
        var existingMap = AuditLogPayload.toMap(existing);

        permissionService
                .requireProcessPermission(
                        execUser.getId(),
                        existing.getProcessId(),
                        ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE
                );

        updateDTO.setProcessId(existing.getProcessId());
        updateDTO.setProcessVersion(existing.getProcessVersion());
        updateDTO.setStatus(existing.getStatus());

        var result = processDefinitionVersionService
                .update(id, updateDTO);
        var resultMap = AuditLogPayload.toMap(result);

        auditService.create()
                .withUser(execUser)
                .withAuditAction(AuditAction.Update, ProcessVersionEntity.class,
                        result.getProcessVersion(),
                        "processVersion",
                        Map.of(
                                "processId", result.getProcessId(),
                                "processVersion", result.getProcessVersion()
                        ))
                .withDiff(existingMap, resultMap).withMessage(
                        "Die Prozessversion %s für den Prozess %s wurde von der Mitarbeiter:in %s aktualisiert.",
                        StringUtils.quote(String.valueOf(result.getProcessVersion())),
                        StringUtils.quote(String.valueOf(result.getProcessId())),
                        StringUtils.quote(execUser.getFullName())
                ).log();

        return result;
    }

    @DeleteMapping("{processDefinitionId}/{processDefinitionVersion}/")
    @Operation(
            summary = "Delete Process Definition Version",
            description = "Delete a process definition version by its composite ID. Requires the permission `" +
                    ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE +
                    "` for the affected process or at system level."
    )
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer processDefinitionId,
            @Nonnull @PathVariable Integer processDefinitionVersion
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService.requireProcessPermission(
                user.getId(),
                processDefinitionId,
                ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE
        );

        var id = new ProcessVersionEntityId(processDefinitionId, processDefinitionVersion);

        var deleted = processDefinitionVersionService
                .delete(id);

        auditService.create()
                .withUser(user)
                .withAuditAction(AuditAction.Delete, ProcessVersionEntity.class,
                        deleted.getProcessVersion(),
                        "processVersion",
                        Map.of(
                                "processId", deleted.getProcessId(),
                                "processVersion", deleted.getProcessVersion()
                        )).withMessage(
                        "Die Prozessversion %s für den Prozess %s wurde von der Mitarbeiter:in %s gelöscht.",
                        StringUtils.quote(String.valueOf(deleted.getProcessVersion())),
                        StringUtils.quote(String.valueOf(deleted.getProcessId())),
                        StringUtils.quote(user.getFullName())
                ).log();
    }

    @GetMapping("{processDefinitionId}/{processDefinitionVersion}/problems/")
    @Operation(
            summary = "Validate a Process Definition Version",
            description = "Validate a process definition version by its composite ID."
    )
    public ProcessVersionProblems validate(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer processDefinitionId,
            @Nonnull @PathVariable Integer processDefinitionVersion
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService.requireProcessPermission(
                user.getId(),
                processDefinitionId,
                ProcessPermissionProvider.PROCESS_DEFINITION_READ
        );

        var id = ProcessVersionEntityId.of(processDefinitionId, processDefinitionVersion);
        var version = processDefinitionVersionService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
        return processDefinitionVersionService
                .validate(version);
    }

    @PutMapping("{processDefinitionId}/{processDefinitionVersion}/publish/")
    @Operation(
            summary = "Publish a Process Definition Version",
            description = "Publish a process definition version by its composite ID."
    )
    public ProcessVersionEntity publish(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer processDefinitionId,
            @Nonnull @PathVariable Integer processDefinitionVersion
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var versionId = ProcessVersionEntityId.of(processDefinitionId, processDefinitionVersion);
        var version = processDefinitionVersionService
                .retrieve(versionId)
                .orElseThrow(ResponseException::notFound);

        permissionService.requireProcessPermission(
                user.getId(),
                version.getProcessId(),
                ProcessPermissionProvider.PROCESS_DEFINITION_PUBLISH_LOCAL
        );

        var hasErrors = processDefinitionVersionService
                .validate(version)
                .hasAnyProblems();
        if (hasErrors) {
            throw ResponseException
                    .notAcceptable("Die Prozessdefinition enthält Fehler und kann daher nicht veröffentlicht werden. Bitte beheben Sie alle Fehler, bevor Sie die Prozessdefinition veröffentlichen.");
        }

        if (version.getStatus() != ProcessVersionStatus.Drafted && version.getStatus() != ProcessVersionStatus.Revoked) {
            throw ResponseException.conflict("Es kann nur eine Prozessdefinition im Entwurfs- oder zurückgezogenen Status veröffentlicht werden.");
        }

        version.setStatus(ProcessVersionStatus.Published);

        return processDefinitionVersionService.update(versionId, version);
    }

    @PutMapping("{processDefinitionId}/{processDefinitionVersion}/revoke/")
    @Operation(
            summary = "Revoke a Process Definition Version",
            description = "Revoke a process definition version by its composite ID."
    )
    public ProcessVersionEntity revoke(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer processDefinitionId,
            @Nonnull @PathVariable Integer processDefinitionVersion
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var versionId = ProcessVersionEntityId.of(processDefinitionId, processDefinitionVersion);
        var version = processDefinitionVersionService
                .retrieve(versionId)
                .orElseThrow(ResponseException::notFound);

        permissionService.requireProcessPermission(
                user.getId(),
                version.getProcessId(),
                ProcessPermissionProvider.PROCESS_DEFINITION_PUBLISH_LOCAL
        );

        if (version.getStatus() != ProcessVersionStatus.Published) {
            throw ResponseException.conflict("Es kann nur eine veröffentlichte Prozessdefinition zurückgezogen werden.");
        }

        version.setStatus(ProcessVersionStatus.Revoked);

        return processDefinitionVersionService.update(versionId, version);
    }
}
