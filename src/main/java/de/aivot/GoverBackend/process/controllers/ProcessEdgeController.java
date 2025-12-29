package de.aivot.GoverBackend.process.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.process.entities.ProcessEdgeEntity;
import de.aivot.GoverBackend.process.filters.ProcessDefinitionEdgeFilter;
import de.aivot.GoverBackend.process.services.ProcessDefinitionEdgeService;
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
@RequestMapping("/api/process-edges/")
@Tag(
        name = OpenApiConstants.Tags.ProcessesDefinitionsName,
        description = OpenApiConstants.Tags.ProcessesDefinitionsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessEdgeController {
    private final ScopedAuditService auditService;
    private final UserService userService;
    private final ProcessDefinitionEdgeService processDefinitionEdgeService;

    @Autowired
    public ProcessEdgeController(AuditService auditService,
                                 UserService userService,
                                 ProcessDefinitionEdgeService processDefinitionEdgeService) {
        this.auditService = auditService.createScopedAuditService(ProcessEdgeController.class);
        this.userService = userService;
        this.processDefinitionEdgeService = processDefinitionEdgeService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Process Definition Edges",
            description = "List all process definition edges with optional filtering and pagination."
    )
    public Page<ProcessEdgeEntity> list(
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
    public ProcessEdgeEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid ProcessEdgeEntity newEdge
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var result = processDefinitionEdgeService
                .create(newEdge);

        auditService.logAction(execUser, AuditAction.Create, ProcessEdgeEntity.class, Map.of(
                "id", result.getId(),
                "processDefinitionId", result.getProcessId(),
                "processDefinitionVersion", result.getProcessVersion()
        ));

        return result;
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Process Definition Edge",
            description = "Retrieve a process definition edge by its ID."
    )
    public ProcessEdgeEntity retrieve(
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

        updateDTO.setId(existing.getId());

        var result = processDefinitionEdgeService
                .update(id, updateDTO);

        auditService.logAction(execUser, AuditAction.Update, ProcessEdgeEntity.class, Map.of(
                "id", result.getId(),
                "processDefinitionId", result.getProcessId(),
                "processDefinitionVersion", result.getProcessVersion()
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

        auditService.logAction(user, AuditAction.Delete, ProcessEdgeEntity.class, Map.of(
                "id", deleted.getId(),
                "processDefinitionId", deleted.getProcessId(),
                "processDefinitionVersion", deleted.getProcessVersion()
        ));
    }
}

