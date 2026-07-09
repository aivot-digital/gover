package de.aivot.gover.backend.process.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.gover.backend.audit.enums.AuditAction;
import de.aivot.gover.backend.audit.services.AuditService;
import de.aivot.gover.backend.audit.services.ScopedAuditService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.openApi.OpenApiConfiguration;
import de.aivot.gover.backend.openApi.OpenApiConstants;
import de.aivot.gover.backend.permissions.services.PermissionService;
import de.aivot.gover.backend.process.entities.ProcessEdgeEntity;
import de.aivot.gover.backend.process.filters.ProcessDefinitionEdgeFilter;
import de.aivot.gover.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.gover.backend.process.services.ProcessEdgeService;
import de.aivot.gover.backend.user.services.UserService;
import de.aivot.gover.backend.utils.StringUtils;
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

@RestController
@RequestMapping("/api/process-edges/")
@Tag(
        name = OpenApiConstants.Tags.ProcessesDefinitionsName,
        description = OpenApiConstants.Tags.ProcessesDefinitionsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessEdgeController {
    private static final String MODULE_NAME = "Prozesse";

    private final ScopedAuditService auditService;
    private final UserService userService;
    private final ProcessEdgeService processDefinitionEdgeService;
    private final ObjectMapper objectMapper;
    private final PermissionService permissionService;

    @Autowired
    public ProcessEdgeController(AuditService auditService,
                                 UserService userService,
                                 ProcessEdgeService processDefinitionEdgeService,
                                 ObjectMapper objectMapper,
                                 PermissionService permissionService) {
        this.auditService = auditService.createScopedAuditService(ProcessEdgeController.class, "Prozesse");
        this.userService = userService;
        this.processDefinitionEdgeService = processDefinitionEdgeService;
        this.objectMapper = objectMapper;
        this.permissionService = permissionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Process Definition Edges",
            description = "List all process definition edges with optional filtering and pagination."
    )
    public Page<ProcessEdgeEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid ProcessDefinitionEdgeFilter filter
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!permissionService.checkSystemPermission(user.getId(), ProcessPermissionProvider.PROCESS_DEFINITION_READ)) {
            if (filter.getProcessDefinitionId() != null) {
                permissionService.hasProcessPermission(
                        user.getId(),
                        filter.getProcessDefinitionId(),
                        ProcessPermissionProvider.PROCESS_DEFINITION_READ
                );
            } else {
                var accessibleProcessIds = permissionService
                        .getProcessesWithPermission(user.getId(), ProcessPermissionProvider.PROCESS_DEFINITION_READ);

                if (filter.getProcessDefinitionIds() != null) {
                    accessibleProcessIds = filter.getProcessDefinitionIds()
                            .stream()
                            .filter(accessibleProcessIds::contains)
                            .toList();
                }

                if (accessibleProcessIds.isEmpty()) {
                    return Page.empty(pageable);
                }

                filter.setProcessDefinitionIds(accessibleProcessIds);
            }
        }

        return processDefinitionEdgeService
                .list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Process Definition Edge",
            description = "Create a new process definition edge. Requires super admin privileges or a user role with create process permissions."
    )
    public ProcessEdgeEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid ProcessEdgeEntity newEdge
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService.hasProcessPermission(
                execUser.getId(),
                newEdge.getProcessId(),
                ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE
        );

        var result = processDefinitionEdgeService
                .create(newEdge);

        auditService.create()
                .withUser(execUser)
                .withAuditAction(AuditAction.Create, ProcessEdgeEntity.class,
                        result.getId(),
                        "id"
                ).withMessage(
                        "Die Prozesskante mit der ID %s wurde von der Mitarbeiter:in %s erstellt.",
                        StringUtils.quote(String.valueOf(result.getId())),
                        StringUtils.quote(execUser.getFullName())
                ).log();

        return result;
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Process Definition Edge",
            description = "Retrieve a process definition edge by its ID."
    )
    public ProcessEdgeEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var edge = processDefinitionEdgeService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        permissionService.hasProcessPermission(
                user.getId(),
                edge.getProcessId(),
                ProcessPermissionProvider.PROCESS_DEFINITION_READ
        );

        return edge;
    }

    @PutMapping("{id}/")
    @Operation(
            summary = "Update Process Definition Edge",
            description = "Update an existing process definition edge. Requires super admin privileges or a user role with edit process permissions."
    )
    public ProcessEdgeEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @RequestBody @Valid ProcessEdgeEntity updateDTO
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var existing = processDefinitionEdgeService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        permissionService.hasProcessPermission(
                execUser.getId(),
                existing.getProcessId(),
                ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE
        );

        var existingMap = objectMapper
                .convertValue(existing, java.util.Map.class);

        updateDTO.setId(existing.getId());

        var result = processDefinitionEdgeService
                .update(id, updateDTO);

        var updatedMap = objectMapper
                .convertValue(result, java.util.Map.class);

        auditService.create()
                .withUser(execUser)
                .withAuditAction(AuditAction.Update, ProcessEdgeEntity.class,
                        result.getId(),
                        "id"
                )
                .withDiff(existingMap, updatedMap).withMessage(
                        "Die Prozesskante mit der ID %s wurde von der Mitarbeiter:in %s aktualisiert.",
                        StringUtils.quote(String.valueOf(result.getId())),
                        StringUtils.quote(execUser.getFullName())
                ).log();

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
                .orElseThrow(ResponseException::unauthorized);

        var existing = processDefinitionEdgeService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        permissionService.hasProcessPermission(
                user.getId(),
                existing.getProcessId(),
                ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE
        );

        var deleted = processDefinitionEdgeService
                .delete(id);

        auditService.create()
                .withUser(user)
                .withAuditAction(AuditAction.Delete, ProcessEdgeEntity.class,
                        deleted.getId(),
                        "id"
                ).withMessage(
                        "Die Prozesskante mit der ID %s wurde von der Mitarbeiter:in %s gelöscht.",
                        StringUtils.quote(String.valueOf(deleted.getId())),
                        StringUtils.quote(user.getFullName())
                ).log();
    }
}
