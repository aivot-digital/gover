package de.aivot.GoverBackend.process.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.elements.models.elements.form.layout.GroupLayout;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.process.entities.ProcessDefinitionNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessDefinitionVersionEntityId;
import de.aivot.GoverBackend.process.filters.ProcessDefinitionNodeFilter;
import de.aivot.GoverBackend.process.services.ProcessDefinitionNodeService;
import de.aivot.GoverBackend.process.services.ProcessDefinitionService;
import de.aivot.GoverBackend.process.services.ProcessDefinitionVersionService;
import de.aivot.GoverBackend.process.services.ProcessNodeProviderService;
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
@RequestMapping("/api/process-definition-nodes/")
@Tag(
        name = OpenApiConstants.Tags.ProcessesDefinitionsName,
        description = OpenApiConstants.Tags.ProcessesDefinitionsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessDefinitionNodeController {
    private final ScopedAuditService auditService;
    private final UserService userService;
    private final ProcessDefinitionNodeService processDefinitionNodeService;
    private final ProcessDefinitionService processDefinitionService;
    private final ProcessNodeProviderService processNodeProviderService;
    private final ProcessDefinitionVersionService processDefinitionVersionService;

    @Autowired
    public ProcessDefinitionNodeController(AuditService auditService,
                                           UserService userService,
                                           ProcessDefinitionNodeService processDefinitionNodeService,
                                           ProcessDefinitionService processDefinitionService,
                                           ProcessNodeProviderService processNodeProviderService,
                                           ProcessDefinitionVersionService processDefinitionVersionService) {
        this.auditService = auditService.createScopedAuditService(ProcessDefinitionNodeController.class);
        this.userService = userService;
        this.processDefinitionNodeService = processDefinitionNodeService;
        this.processDefinitionService = processDefinitionService;
        this.processNodeProviderService = processNodeProviderService;
        this.processDefinitionVersionService = processDefinitionVersionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Process Definition Nodes",
            description = "List all process definition nodes with optional filtering and pagination."
    )
    public Page<ProcessDefinitionNodeEntity> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid ProcessDefinitionNodeFilter filter
    ) throws ResponseException {
        return processDefinitionNodeService
                .list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Process Definition Node",
            description = "Create a new process definition node. Requires super admin privileges or a user role with create process permissions."
    )
    public ProcessDefinitionNodeEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid ProcessDefinitionNodeEntity newNode
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var result = processDefinitionNodeService
                .create(newNode);

        auditService.logAction(execUser, AuditAction.Create, ProcessDefinitionNodeEntity.class, Map.of(
                "id", result.getId(),
                "processDefinitionId", result.getProcessDefinitionId(),
                "processDefinitionVersion", result.getProcessDefinitionVersion()
        ));

        return result;
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Process Definition Node",
            description = "Retrieve a process definition node by its ID."
    )
    public ProcessDefinitionNodeEntity retrieve(
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        return processDefinitionNodeService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    @Operation(
            summary = "Update Process Definition Node",
            description = "Update an existing process definition node. Requires super admin privileges or a user role with edit process permissions."
    )
    public ProcessDefinitionNodeEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @RequestBody @Valid ProcessDefinitionNodeEntity updateDTO
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var existing = processDefinitionNodeService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        updateDTO.setId(existing.getId());

        var result = processDefinitionNodeService
                .update(id, updateDTO);

        auditService.logAction(execUser, AuditAction.Update, ProcessDefinitionNodeEntity.class, Map.of(
                "id", result.getId(),
                "processDefinitionId", result.getProcessDefinitionId(),
                "processDefinitionVersion", result.getProcessDefinitionVersion()
        ));

        return result;
    }

    @DeleteMapping("{id}/")
    @Operation(
            summary = "Delete Process Definition Node",
            description = "Delete a process definition node by its ID. Requires super admin privileges."
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

        var deleted = processDefinitionNodeService
                .delete(id);

        auditService.logAction(user, AuditAction.Delete, ProcessDefinitionNodeEntity.class, Map.of(
                "id", deleted.getId(),
                "processDefinitionId", deleted.getProcessDefinitionId(),
                "processDefinitionVersion", deleted.getProcessDefinitionVersion()
        ));
    }


    @GetMapping("{id}/configuration/")
    @Operation(
            summary = "Retrieve Process Definition Node Configuration",
            description = "Retrieve the configuration of a process definition node by its ID."
    )
    public GroupLayout configuration(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var node = processDefinitionNodeService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        var provider = processNodeProviderService
                .getProcessNodeProvider(node.getCodeKey())
                .orElseThrow(ResponseException::badRequest);

        var processDefinition = processDefinitionService
                .retrieve(node.getProcessDefinitionId())
                .orElseThrow(ResponseException::badRequest);

        var processVersion = processDefinitionVersionService
                .retrieve(ProcessDefinitionVersionEntityId.of(processDefinition.getId(), node.getProcessDefinitionVersion()))
                .orElseThrow(ResponseException::badRequest);

        return provider
                .getConfigurationLayout(user, processDefinition, processVersion, node);
    }
}

