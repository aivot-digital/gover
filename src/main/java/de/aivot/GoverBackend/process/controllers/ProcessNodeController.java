package de.aivot.GoverBackend.process.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.permissions.services.PermissionService;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessVersionEntityId;
import de.aivot.GoverBackend.process.filters.ProcessNodeFilter;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinitionContextConfig;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinitionContextTesting;
import de.aivot.GoverBackend.process.permissions.ProcessPermissionProvider;
import de.aivot.GoverBackend.process.repositories.ProcessNodeRepository;
import de.aivot.GoverBackend.process.repositories.ProcessTestClaimRepository;
import de.aivot.GoverBackend.process.services.*;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.utils.StringUtils;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

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
    private final ProcessNodeService processDefinitionNodeService;
    private final ProcessService processDefinitionService;
    private final PermissionService permissionService;
    private final ProcessNodeDefinitionService processNodeProviderService;
    private final ProcessNodeExportService processNodeExportService;
    private final ProcessVersionService processDefinitionVersionService;
    private final ProcessTestClaimRepository processTestClaimRepository;
    private final ObjectMapper objectMapper;
    private final ProcessNodeRepository processNodeRepository;

    @Nonnull
    private static String createAvailableDataKey(@Nonnull String requestedDataKey,
                                                 @Nonnull Set<String> occupiedDataKeys) {
        if (!occupiedDataKeys.contains(requestedDataKey)) {
            return requestedDataKey;
        }

        var normalizedBase = requestedDataKey.length() > 27
                ? requestedDataKey.substring(0, 27)
                : requestedDataKey;

        for (var copyIndex = 2; copyIndex < 10_000; copyIndex++) {
            var suffix = "-%d".formatted(copyIndex);
            var truncatedBase = normalizedBase.length() > 32 - suffix.length()
                    ? normalizedBase.substring(0, 32 - suffix.length())
                    : normalizedBase;
            var candidate = truncatedBase + suffix;

            if (!occupiedDataKeys.contains(candidate)) {
                return candidate;
            }
        }

        throw new IllegalStateException("Kein freier Datenschluessel verfuegbar.");
    }

    @Autowired
    public ProcessNodeController(AuditService auditService,
                                 UserService userService,
                                 ProcessNodeService processDefinitionNodeService,
                                 ProcessService processDefinitionService,
                                 PermissionService permissionService,
                                 ProcessNodeDefinitionService processNodeProviderService,
                                 ProcessNodeExportService processNodeExportService,
                                 ProcessVersionService processDefinitionVersionService,
                                 ProcessTestClaimRepository processTestClaimRepository,
                                 ObjectMapper objectMapper, ProcessNodeRepository processNodeRepository) {
        this.auditService = auditService.createScopedAuditService(ProcessNodeController.class, "Prozesse");
        this.userService = userService;
        this.processDefinitionNodeService = processDefinitionNodeService;
        this.processDefinitionService = processDefinitionService;
        this.permissionService = permissionService;
        this.processNodeProviderService = processNodeProviderService;
        this.processNodeExportService = processNodeExportService;
        this.processDefinitionVersionService = processDefinitionVersionService;
        this.processTestClaimRepository = processTestClaimRepository;
        this.objectMapper = objectMapper;
        this.processNodeRepository = processNodeRepository;
    }

    @GetMapping("")
    @Operation(
            summary = "List Process Definition Nodes",
            description = "List all process definition nodes with optional filtering and pagination."
    )
    public Page<ProcessNodeEntity> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid ProcessNodeFilter filter
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

        auditService.create()
                .withUser(execUser)
                .withAuditAction(AuditAction.Create, ProcessNodeEntity.class,
                        result.getId(),
                        "id"
                ).withMessage(
                        "Der Prozessknoten mit der ID %s wurde von der Mitarbeiter:in %s erstellt.",
                        StringUtils.quote(String.valueOf(result.getId())),
                        StringUtils.quote(execUser.getFullName())
                ).log();

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

        if (processNodeRepository.existsByDataKeyAndIdIsNotAndProcessIdAndProcessVersion(
                updateDTO.getDataKey(),
                existing.getId(),
                existing.getProcessId(),
                existing.getProcessVersion()
        )) {
            throw ResponseException.badRequest(
                    String.format(
                            "Der Datenschlüssel %s wird innerhalb dieses Prozesses bereits verwendet. Bitte vergeben Sie einen eindeutigen Datenschlüssel.",
                            StringUtils.quote(updateDTO.getDataKey())
                    ));
        }

        var existingMap = objectMapper
                .convertValue(existing, Map.class);

        updateDTO.setId(existing.getId());

        var result = processDefinitionNodeService
                .update(id, updateDTO);

        var updatedMap = objectMapper
                .convertValue(result, Map.class);

        auditService.create()
                .withUser(execUser)
                .withAuditAction(AuditAction.Update, ProcessNodeEntity.class,
                        result.getId(),
                        "id"
                )
                .withDiff(existingMap, updatedMap).withMessage(
                        "Der Prozessknoten mit der ID %s wurde von der Mitarbeiter:in %s aktualisiert.",
                        StringUtils.quote(String.valueOf(result.getId())),
                        StringUtils.quote(execUser.getFullName())
                ).log();

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

        auditService.create()
                .withUser(user)
                .withAuditAction(AuditAction.Delete, ProcessNodeEntity.class,
                        deleted.getId(),
                        "id"
                ).withMessage(
                        "Der Prozessknoten mit der ID %s wurde von der Mitarbeiter:in %s gelöscht.",
                        StringUtils.quote(String.valueOf(deleted.getId())),
                        StringUtils.quote(user.getFullName())
                ).log();
    }

    @GetMapping("{id}/export/")
    @Operation(
            summary = "Export Process Definition Node",
            description = "Export a process definition node including its cleaned configuration. " +
                    "Requires read permissions for the owning process definition."
    )
    public ProcessNodeExportService.ProcessNodeExport export(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var existingNode = processDefinitionNodeService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        var existingProcess = processDefinitionService
                .retrieve(existingNode.getProcessId())
                .orElseThrow(ResponseException::badRequest);

        permissionService.testDepartmentPermission(
                execUser.getId(),
                existingProcess.getDepartmentId(),
                ProcessPermissionProvider.PROCESS_DEFINITION_READ
        );

        var result = processNodeExportService
                .export(id);

        auditService.create()
                .withUser(execUser)
                .withAuditAction(AuditAction.Export, ProcessNodeEntity.class,
                        existingNode.getId(),
                        "id"
                )
                .withMessage("Das Prozesselement %s (%d) wurde von der Mitarbeiter:in %s exportiert."
                        .formatted(
                                StringUtils.quote(existingNode.getDataKey()),
                                existingNode.getId(),
                                StringUtils.quote(execUser.getFullName())
                        )
                )
                .log();

        return result;
    }

    @PostMapping("import/{processId}/{processVersion}/")
    @Operation(
            summary = "Import Process Definition Node",
            description = "Import a process definition node into a specific process version. " +
                    "Requires edit permissions for the target process definition."
    )
    public ProcessNodeEntity importNode(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer processId,
            @Nonnull @PathVariable Integer processVersion,
            @Nonnull @RequestBody @Valid ProcessNodeExportService.ProcessNodeExport processNodeExport
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var targetProcess = processDefinitionService
                .retrieve(processId)
                .orElseThrow(ResponseException::notFound);

        permissionService.testDepartmentPermission(
                execUser.getId(),
                targetProcess.getDepartmentId(),
                ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE
        );

        processDefinitionVersionService
                .retrieve(ProcessVersionEntityId.of(processId, processVersion))
                .orElseThrow(ResponseException::badRequest);

        var exportData = processNodeExport.data();
        var sourceNode = exportData.node();
        var occupiedDataKeys = processDefinitionNodeService
                .getAllUsedDataKeys(processId, processVersion);

        var provider = processNodeProviderService
                .getProcessNodeDefinition(
                        sourceNode.getProcessNodeDefinitionKey(),
                        sourceNode.getProcessNodeDefinitionVersion()
                )
                .orElseThrow(() -> ResponseException.badRequest(
                        "Eine Prozesselementdefinition mit dem Schlüssel „%s“ und der Version „%d“ ist nicht verfügbar."
                                .formatted(sourceNode.getProcessNodeDefinitionKey(), sourceNode.getProcessNodeDefinitionVersion())
                ));

        var importedNode = processDefinitionNodeService
                .create(new ProcessNodeEntity()
                        .setProcessId(processId)
                        .setProcessVersion(processVersion)
                        .setName(sourceNode.getName())
                        .setDescription(sourceNode.getDescription())
                        .setDataKey(createAvailableDataKey(sourceNode.getDataKey(), occupiedDataKeys))
                        .setProcessNodeDefinitionKey(sourceNode.getProcessNodeDefinitionKey())
                        .setProcessNodeDefinitionVersion(sourceNode.getProcessNodeDefinitionVersion())
                        .setConfiguration(provider.prefillConfigurationOnImport(sourceNode.getConfiguration()))
                        .setOutputMappings(sourceNode.getOutputMappings())
                        .setTimeLimitDays(sourceNode.getTimeLimitDays())
                        .setRequirements(sourceNode.getRequirements())
                        .setNotes(sourceNode.getNotes())
                );

        auditService.create()
                .withUser(execUser)
                .withAuditAction(AuditAction.Create, ProcessNodeEntity.class,
                        importedNode.getId(),
                        "id",
                        Map.of(
                                "imported", true,
                                "sourceProcessId", exportData.process().getId(),
                                "sourceProcessVersion", exportData.version().getProcessVersion(),
                                "sourceNodeId", sourceNode.getId()
                        )
                )
                .withMessage(
                        "Das Prozesselement mit der ID %s wurde von der Mitarbeiter:in %s aus einem Import in den Prozess %s (Version %s) erstellt.",
                        StringUtils.quote(String.valueOf(importedNode.getId())),
                        StringUtils.quote(execUser.getFullName()),
                        StringUtils.quote(String.valueOf(processId)),
                        StringUtils.quote(String.valueOf(processVersion))
                ).log();

        return importedNode;
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

    @GetMapping("{id}/testing/")
    @Operation(
            summary = "Retrieve Process Definition Node Testing Layout",
            description = "Retrieve the testing layout of a process definition node by its ID."
    )
    public GroupLayoutElement testing(
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

        var testClaim = processTestClaimRepository
                .findByProcessIdAndProcessVersion(node.getProcessId(), node.getProcessVersion())
                .orElseThrow(ResponseException::badRequest);

        var configuration = processDefinitionNodeService
                .deriveConfiguration(node, false, user);

        var context = new ProcessNodeDefinitionContextTesting(
                user,
                processDefinition,
                processVersion,
                node,
                testClaim,
                configuration
        );

        return provider
                .getTestingLayout(context);
    }

    @GetMapping("{id}/problems/")
    @Operation(
            summary = "Retrieve Process Definition Node Testing Layout",
            description = "Retrieve the testing layout of a process definition node by its ID."
    )
    public Object problems(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var res = processDefinitionNodeService
                .validate(id, false);

        if (res.isPresent()) {
            return res.get();
        }

        return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
    }
}
