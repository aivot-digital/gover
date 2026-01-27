package de.aivot.GoverBackend.process.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessVersionEntityId;
import de.aivot.GoverBackend.process.filters.ProcessDefinitionNodeFilter;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinitionContextConfig;
import de.aivot.GoverBackend.process.services.ProcessDefinitionNodeService;
import de.aivot.GoverBackend.process.services.ProcessDefinitionService;
import de.aivot.GoverBackend.process.services.ProcessDefinitionVersionService;
import de.aivot.GoverBackend.process.services.ProcessNodeDefinitionService;
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
@RequestMapping("/api/process-nodes/")
@Tag(
        name = OpenApiConstants.Tags.ProcessesDefinitionsName,
        description = OpenApiConstants.Tags.ProcessesDefinitionsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessNodeController {
    private final ScopedAuditService auditService;
    private final UserService userService;
    private final ProcessDefinitionNodeService processDefinitionNodeService;
    private final ProcessDefinitionService processDefinitionService;
    private final ProcessNodeDefinitionService processNodeProviderService;
    private final ProcessDefinitionVersionService processDefinitionVersionService;

    @Autowired
    public ProcessNodeController(AuditService auditService,
                                 UserService userService,
                                 ProcessDefinitionNodeService processDefinitionNodeService,
                                 ProcessDefinitionService processDefinitionService,
                                 ProcessNodeDefinitionService processNodeProviderService,
                                 ProcessDefinitionVersionService processDefinitionVersionService) {
        this.auditService = auditService.createScopedAuditService(ProcessNodeController.class);
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
    public Page<ProcessNodeEntity> list(
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
    public ProcessNodeEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid ProcessNodeEntity newNode
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var result = processDefinitionNodeService
                .create(newNode);

        auditService.logAction(execUser, AuditAction.Create, ProcessNodeEntity.class, Map.of(
                "id", result.getId(),
                "processDefinitionId", result.getProcessId(),
                "processDefinitionVersion", result.getProcessVersion()
        ));

        return result;
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Process Definition Node",
            description = "Retrieve a process definition node by its ID."
    )
    public ProcessNodeEntity retrieve(
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
    public ProcessNodeEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @RequestBody @Valid ProcessNodeEntity updateDTO
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

        auditService.logAction(execUser, AuditAction.Update, ProcessNodeEntity.class, Map.of(
                "id", result.getId(),
                "processDefinitionId", result.getProcessId(),
                "processDefinitionVersion", result.getProcessVersion()
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

        auditService.logAction(user, AuditAction.Delete, ProcessNodeEntity.class, Map.of(
                "id", deleted.getId(),
                "processDefinitionId", deleted.getProcessId(),
                "processDefinitionVersion", deleted.getProcessVersion()
        ));
    }


    @GetMapping("{id}/configuration/")
    @Operation(
            summary = "Retrieve Process Definition Node Configuration",
            description = "Retrieve the configuration of a process definition node by its ID."
    )
    public ConfigLayoutElement configuration(
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
                .getProcessNodeDefinition(node.getProcessNodeDefinitionKey(), node.getProcessNodeDefinitionVersion())
                .orElseThrow(ResponseException::badRequest);

        var processDefinition = processDefinitionService
                .retrieve(node.getProcessId())
                .orElseThrow(ResponseException::badRequest);

        var processVersion = processDefinitionVersionService
                .retrieve(ProcessVersionEntityId.of(processDefinition.getId(), node.getProcessVersion()))
                .orElseThrow(ResponseException::badRequest);

        var context = new ProcessNodeDefinitionContextConfig(
                user,
                processDefinition,
                processVersion,
                node
        );

        return provider
                .getConfigurationLayout(context);
    }
}

