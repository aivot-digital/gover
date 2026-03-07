package de.aivot.GoverBackend.process.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.models.AuditLogPayload;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.process.entities.ProcessEdgeEntity;
import de.aivot.GoverBackend.process.filters.ProcessDefinitionEdgeFilter;
import de.aivot.GoverBackend.process.services.ProcessEdgeService;
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
    private final ProcessEdgeService processDefinitionEdgeService;
    private final ObjectMapper objectMapper;

    @Autowired
    public ProcessEdgeController(AuditService auditService,
                                 UserService userService,
                                 ProcessEdgeService processDefinitionEdgeService,
                                 ObjectMapper objectMapper) {
        this.auditService = auditService.createScopedAuditService(ProcessEdgeController.class);
        this.userService = userService;
        this.processDefinitionEdgeService = processDefinitionEdgeService;
        this.objectMapper = objectMapper;
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

        auditService.addAuditEntry(AuditLogPayload
                .create()
                .withUser(execUser)
                .withAuditAction(
                        AuditAction.Create,
                        ProcessEdgeEntity.class,
                        result.getId()
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

        var existingMap = objectMapper
                .convertValue(existing, java.util.Map.class);

        updateDTO.setId(existing.getId());

        var result = processDefinitionEdgeService
                .update(id, updateDTO);

        var updatedMap = objectMapper
                .convertValue(result, java.util.Map.class);

        auditService.addAuditEntry(AuditLogPayload
                .create()
                .withUser(execUser)
                .withAuditAction(
                        AuditAction.Update,
                        ProcessEdgeEntity.class,
                        result.getId()
                )
                .withDiffUndefined(existingMap, updatedMap));

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

        auditService.addAuditEntry(AuditLogPayload
                .create()
                .withUser(user)
                .withAuditAction(
                        AuditAction.Delete,
                        ProcessEdgeEntity.class,
                        deleted.getId()
                ));
    }
}
