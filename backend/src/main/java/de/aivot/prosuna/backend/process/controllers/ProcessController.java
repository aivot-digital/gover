package de.aivot.prosuna.backend.process.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.prosuna.backend.audit.enums.AuditAction;
import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.audit.services.ScopedAuditService;
import de.aivot.prosuna.backend.department.services.DepartmentService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.openApi.OpenApiConstants;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.*;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.filters.ProcessFilter;
import de.aivot.prosuna.backend.process.filters.ProcessInstanceAccessControlPresetFilter;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.repositories.ProcessVersionRepository;
import de.aivot.prosuna.backend.process.services.*;
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

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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
    private final ProcessInstanceAccessControlPresetService processInstanceAccessControlPresetService;
    private final ProcessNodeDefinitionService processNodeProviderService;
    private final ObjectMapper objectMapper;

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
                             ProcessInstanceAccessControlPresetService processInstanceAccessControlPresetService,
                             ProcessNodeDefinitionService processNodeProviderService,
                             ObjectMapper objectMapper) {
        this.auditService = auditService.createScopedAuditService(ProcessController.class, "Prozesse");

        this.userService = userService;
        this.processDefinitionService = processDefinitionService;
        this.departmentService = departmentService;
        this.permissionService = permissionService;
        this.processExportService = processExportService;
        this.processDefinitionVersionRepository = processDefinitionVersionRepository;
        this.processDefinitionVersionService = processDefinitionVersionService;
        this.processDefinitionNodeService = processDefinitionNodeService;
        this.processDefinitionEdgeService = processDefinitionEdgeService;
        this.processInstanceAccessControlPresetService = processInstanceAccessControlPresetService;
        this.processNodeProviderService = processNodeProviderService;
        this.objectMapper = objectMapper;
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

    @GetMapping("slug-availability/")
    @Operation(
            summary = "Check Process Slug Availability",
            description = "Check whether a public URL namespace can be used by a process definition."
    )
    public ProcessSlugAvailabilityResponse checkSlugAvailability(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestParam String slug,
            @Nullable @RequestParam(required = false) Integer processId
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (processId != null) {
            var process = processDefinitionService
                    .retrieve(processId)
                    .orElseThrow(ResponseException::notFound);

            permissionService.requireProcessPermission(
                    execUser.getId(),
                    process.getId(),
                    ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE
            );
        }

        return new ProcessSlugAvailabilityResponse(
                processDefinitionService.isSlugAvailable(slug, processId)
        );
    }

    @PostMapping("")
    @Operation(
            summary = "Create Process Definition",
            description = "Create a new process definition. " +
                    "Requires the permission `" + ProcessPermissionProvider.PROCESS_DEFINITION_CREATE +
                    "` for the target organisation unit or at system level."
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
                .requireDepartmentPermission(
                        execUser.getId(),
                        department.getId(),
                        ProcessPermissionProvider.PROCESS_DEFINITION_CREATE
                );

        var result = processDefinitionService
                .create(newProcessDefinition);

        auditService.create()
                .withUser(execUser)
                .withAuditAction(AuditAction.Create, ProcessEntity.class,
                        result.getId(),
                        "id"
                ).withMessage(
                        "Der Prozess mit der ID %s wurde von der Mitarbeiter:in %s erstellt.",
                        StringUtils.quote(String.valueOf(result.getId())),
                        StringUtils.quote(execUser.getFullName())
                ).log();

        return result;
    }

    @PostMapping("import/")
    @Operation(
            summary = "Import Process Definition",
            description = "Import a process definition from exported data. " +
                    "Requires the permission `" + ProcessPermissionProvider.PROCESS_DEFINITION_CREATE +
                    "` for the target organisation unit or at system level."
    )
    public ProcessEntity importProc(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid ProcessExportService.ProcessExport exportData
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var department = departmentService
                .retrieve(exportData.process().getDepartmentId())
                .orElseThrow(ResponseException::badRequest);

        permissionService.requireDepartmentPermission(
                execUser.getId(),
                department.getId(),
                ProcessPermissionProvider.PROCESS_DEFINITION_CREATE
        );

        processDefinitionNodeService.validateNewProcessNodeBatch(exportData.nodes());

        var newProcess = processDefinitionService
                .create(
                        exportData
                                .process()
                                .setVersionCount(0)
                                .setDraftedVersion(null)
                                .setPublishedVersion(null)
                );

        var newVersion = processDefinitionVersionService
                .create(
                        exportData
                                .version()
                                .setProcessVersion(1)
                                .setStatus(ProcessVersionStatus.Drafted)
                                .setProcessId(newProcess.getId())
                                .setPublished(null)
                                .setRevoked(null)
                );

        var createdNodesIds = new LinkedList<Integer>();
        var createdNodes = new LinkedList<ProcessNodeEntity>();

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

            createdNodesIds.add(addedNode.getId());
            createdNodes.add(addedNode);
        }

        var createdEdgesIds = new LinkedList<Integer>();
        for (var edge : exportData.edges()) {
            var translatedFromNodeId = savedNodeIdMap.get(edge.getFromNodeId());
            var translatedToNodeId = savedNodeIdMap.get(edge.getToNodeId());

            var createdEdge = processDefinitionEdgeService
                    .create(edge
                            .setProcessId(newProcess.getId())
                            .setProcessVersion(newVersion.getProcessVersion())
                            .setFromNodeId(translatedFromNodeId)
                            .setToNodeId(translatedToNodeId)
                    );

            createdEdgesIds.add(createdEdge.getId());
        }

        for (var node : createdNodes) {
            var prov = processNodeProviderService
                    .getProcessNodeDefinition(node)
                    .orElseThrow(() -> ResponseException
                            .badRequest("Eine Prozesselementdefinition mit dem Schlüssel „%s“ und der Version „%d“ ist nicht verfügbar."
                                    .formatted(node.getProcessNodeDefinitionKey(), node.getProcessNodeDefinitionVersion())));

            processDefinitionNodeService
                    .validate(node, prov, true)
                    .ifPresent((ignored) -> {
                        node.setSavedWithErrors(true);
                        try {
                            processDefinitionNodeService.update(node.getId(), node);
                        } catch (ResponseException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }

        auditService.create()
                .withUser(execUser)
                .withAuditAction(AuditAction.Create, ProcessEntity.class,
                        newProcess.getId(),
                        "id",
                        Map.of(
                                "imported", true,
                                "version", newVersion.getProcessVersion(),
                                "newNodeIds", createdNodesIds,
                                "newEdgeIds", createdEdgesIds
                        )
                ).withMessage(
                        "Der Prozess mit der ID %s wurde von der Mitarbeiter:in %s aus einem Import erstellt (Version %s, %s Knoten, %s Kanten).",
                        StringUtils.quote(String.valueOf(newProcess.getId())),
                        StringUtils.quote(execUser.getFullName()),
                        StringUtils.quote(String.valueOf(newVersion.getProcessVersion())),
                        StringUtils.quote(String.valueOf(createdNodesIds.size())),
                        StringUtils.quote(String.valueOf(createdEdgesIds.size()))
                ).log();

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

        permissionService.requireProcessPermission(
                execUser.getId(),
                proc.getId(),
                ProcessPermissionProvider.PROCESS_DEFINITION_READ
        );

        return proc;
    }

    @PutMapping("{id}/")
    @Operation(
            summary = "Update Process Definition",
            description = "Update an existing process definition. " +
                    "Requires the permission `" + ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE +
                    "` for the affected process or at system level."
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

        var existingMap = objectMapper
                .convertValue(existing, Map.class);

        permissionService.requireProcessPermission(
                execUser.getId(),
                existing.getId(),
                ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE
        );

        updateDTO.setId(existing.getId());
        // Prevent department id overriding. This should be done via the move endpoint.
        updateDTO.setDepartmentId(existing.getDepartmentId());

        var result = processDefinitionService
                .update(id, updateDTO);

        var updatedMap = objectMapper
                .convertValue(result, Map.class);

        auditService.create()
                .withUser(execUser)
                .withAuditAction(AuditAction.Update, ProcessEntity.class,
                        result.getId(),
                        "id"
                )
                .withDiff(existingMap, updatedMap).withMessage(
                        "Der Prozess mit der ID %s wurde von der Mitarbeiter:in %s aktualisiert.",
                        StringUtils.quote(String.valueOf(result.getId())),
                        StringUtils.quote(execUser.getFullName())
                ).log();

        return result;
    }

    @GetMapping("{id}/slug-history/")
    @Operation(
            summary = "List Process Slug History",
            description = "List previous public URL namespaces for a process definition. " +
                    "Requires the permission `" + ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE +
                    "` for the affected process or at system level."
    )
    public List<ProcessSlugHistoryEntity> listSlugHistory(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var process = processDefinitionService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        permissionService.requireProcessPermission(
                execUser.getId(),
                process.getId(),
                ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE
        );

        return processDefinitionService.listSlugHistory(id);
    }

    @DeleteMapping("{id}/slug-history/")
    @Operation(
            summary = "Clear Process Slug History",
            description = "Delete previous public URL namespaces for a process definition. " +
                    "Requires the permission `" + ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE +
                    "` for the affected process or at system level."
    )
    public void clearSlugHistory(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var process = processDefinitionService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        permissionService.requireProcessPermission(
                execUser.getId(),
                process.getId(),
                ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE
        );

        processDefinitionService.clearSlugHistory(id);

        auditService.create()
                .withUser(execUser)
                .withAuditAction(AuditAction.Update, ProcessEntity.class,
                        process.getId(),
                        "id"
                ).withMessage(
                        "Die Slug-Historie des Prozesses mit der ID %s wurde von der Mitarbeiter:in %s geleert.",
                        StringUtils.quote(String.valueOf(process.getId())),
                        StringUtils.quote(execUser.getFullName())
                ).log();
    }

    @DeleteMapping("{id}/")
    @Operation(
            summary = "Delete Process Definition",
            description = "Delete a process definition by its ID. " +
                    "Requires the permission `" + ProcessPermissionProvider.PROCESS_DEFINITION_DELETE +
                    "` for the affected process or at system level."
    )
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService.requireProcessPermission(
                user.getId(),
                id,
                ProcessPermissionProvider.PROCESS_DEFINITION_DELETE
        );

        var deleted = processDefinitionService
                .delete(id);

        auditService.create()
                .withUser(user)
                .withAuditAction(AuditAction.Delete, ProcessEntity.class,
                        deleted.getId(),
                        "id"
                ).withMessage(
                        "Der Prozess mit der ID %s wurde von der Mitarbeiter:in %s gelöscht.",
                        StringUtils.quote(String.valueOf(deleted.getId())),
                        StringUtils.quote(user.getFullName())
                ).log();
    }

    @PutMapping("{id}/move/")
    @Operation(
            summary = "Move Process",
            description = "Move a process to another department. " +
                    "Requires the permission `" + ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE +
                    "` for the affected process and `" + ProcessPermissionProvider.PROCESS_DEFINITION_CREATE +
                    "` for the target organisation unit. System-level grants satisfy the corresponding requirement."
    )
    @SecurityRequirement(name = OpenApiConfiguration.Security)
    public ProcessEntity move(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @RequestParam Integer targetDepartmentId
    ) throws ResponseException {
        // Extract staff user
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Fetch process
        var process = processDefinitionService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        var existingMap = objectMapper
                .convertValue(process, Map.class);

        // Check if the user has edit permission for the process in the original department
        permissionService.requireProcessPermission(
                user.getId(),
                process.getId(),
                ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE
        );

        // Check if the user has create permission for the process in the target department
        permissionService.requireDepartmentPermission(
                user.getId(),
                targetDepartmentId,
                ProcessPermissionProvider.PROCESS_DEFINITION_CREATE
        );

        // Moving a process changes only the managing department. Version pointers must remain untouched.
        process.setDepartmentId(targetDepartmentId);

        // Persist through the regular update path so process metadata handling stays centralized.
        var result = processDefinitionService.update(process.getId(), process);

        var updatedMap = objectMapper
                .convertValue(result, Map.class);

        auditService.create()
                .withUser(user)
                .withAuditAction(AuditAction.Update, ProcessEntity.class,
                        result.getId(),
                        "id"
                )
                .withDiff(existingMap, updatedMap).withMessage(
                        "Der Prozess mit der ID %s wurde von der Mitarbeiter:in %s an die Organisationseinheit mit der ID %s übertragen.",
                        StringUtils.quote(String.valueOf(result.getId())),
                        StringUtils.quote(user.getFullName()),
                        StringUtils.quote(String.valueOf(targetDepartmentId))
                ).log();

        return result;
    }


    @PostMapping("{id}/new-version/latest/")
    @Operation(
            summary = "Create Process Definition Version from Latest",
            description = "Create a new draft from the latest version of a process definition. " +
                    "Requires the permission `" + ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE +
                    "` for the affected process or at system level."
    )
    public ProcessVersionEntity newVersionFromLatest(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var latestVersion = processDefinitionVersionRepository
                .maxVersionForProcessDefinition(id)
                .orElseThrow(ResponseException::notFound);

        return newVersionFromExisting(jwt, id, latestVersion);
    }

    @PostMapping("{id}/new-version/{version}/")
    @Operation(
            summary = "Create Process Definition Version from Existing",
            description = "Create a new draft from a specific version of a process definition. " +
                    "Requires the permission `" + ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE +
                    "` for the affected process or at system level."
    )
    public ProcessVersionEntity newVersionFromExisting(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @PathVariable Integer version
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var process = processDefinitionService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        permissionService.requireProcessPermission(
                user.getId(),
                process.getId(),
                ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE
        );

        var draftExists = processDefinitionVersionRepository
                .existsByProcessIdAndStatus(process.getId(), ProcessVersionStatus.Drafted);
        if (draftExists) {
            throw ResponseException.conflict("Es existiert bereits ein Entwurf für diesen Prozess. Bitte veröffentlichen oder löschen Sie den bestehenden Entwurf, bevor Sie einen neuen erstellen.");
        }

        var originalProcessVersion = processDefinitionVersionService
                .retrieve(ProcessVersionEntityId.of(process.getId(), version))
                .orElseThrow(ResponseException::notFound);

        if (
                originalProcessVersion.getStatus() != ProcessVersionStatus.Published &&
                        originalProcessVersion.getStatus() != ProcessVersionStatus.Revoked
        ) {
            throw ResponseException
                    .conflict("Neue Entwürfe können nur aus veröffentlichten oder zurückgezogenen Versionen erstellt werden.", version);
        }

        var nextProcessVersionNumber = processDefinitionVersionRepository
                .maxVersionForProcessDefinition(id)
                .orElse(0) + 1;

        var originalNodes = processDefinitionNodeService
                .findAllByProcessIdAndProcessVersion(process.getId(), originalProcessVersion.getProcessVersion());
        processDefinitionNodeService.validateNewProcessNodeBatch(originalNodes);

        var createdProcessVersion = processDefinitionVersionService
                .create(new ProcessVersionEntity(
                        process.getId(),
                        nextProcessVersionNumber,
                        ProcessVersionStatus.Drafted,
                        originalProcessVersion.getPublicTitle(),
                        originalProcessVersion.getCaseNumberTemplate(),
                        originalProcessVersion.getNotes(),
                        Instant.now(),
                        Instant.now(),
                        null,
                        null
                )
                        .setThemeId(originalProcessVersion.getThemeId())
                        .setLegalSupportDepartmentId(originalProcessVersion.getLegalSupportDepartmentId())
                        .setTechnicalSupportDepartmentId(originalProcessVersion.getTechnicalSupportDepartmentId())
                        .setImprintDepartmentId(originalProcessVersion.getImprintDepartmentId())
                        .setPrivacyDepartmentId(originalProcessVersion.getPrivacyDepartmentId())
                        .setAccessibilityDepartmentId(originalProcessVersion.getAccessibilityDepartmentId())
                        .setProcessSpecificPrivacyStatement(originalProcessVersion.getProcessSpecificPrivacyStatement())
                        .setProcessSpecificAccessibilityStatement(originalProcessVersion.getProcessSpecificAccessibilityStatement()));

        copyProcessInstanceAccessControlPresets(
                process.getId(),
                originalProcessVersion.getProcessVersion(),
                createdProcessVersion.getProcessVersion()
        );

        var nodesIdMap = new HashMap<Integer, Integer>();
        for (var originalNode : originalNodes) {
            var createdNode = processDefinitionNodeService
                    .create(new ProcessNodeEntity(
                            null,
                            process.getId(),
                            createdProcessVersion.getProcessVersion(),
                            originalNode.getName(),
                            originalNode.getDescription(),
                            originalNode.getDataKey(),
                            originalNode.getProcessNodeDefinitionKey(),
                            originalNode.getProcessNodeDefinitionVersion(),
                            originalNode.getConfiguration().clone(),
                            new HashMap<>(originalNode.getOutputMappings()),
                            originalNode.getTimeLimitDays(),
                            originalNode.getRequirements(),
                            originalNode.getNotes(),
                            originalNode.getSavedWithErrors()
                    ));
            nodesIdMap.put(originalNode.getId(), createdNode.getId());
        }

        var originalEdges = processDefinitionEdgeService
                .findAllByProcessIdAndProcessVersion(process.getId(), originalProcessVersion.getProcessVersion());
        for (var originalEdge : originalEdges) {
            processDefinitionEdgeService
                    .create(new ProcessEdgeEntity(
                            null,
                            process.getId(),
                            createdProcessVersion.getProcessVersion(),
                            nodesIdMap.get(originalEdge.getFromNodeId()),
                            nodesIdMap.get(originalEdge.getToNodeId()),
                            originalEdge.getViaPort()
                    ));
        }

        return createdProcessVersion;
    }

    private void copyProcessInstanceAccessControlPresets(
            @Nonnull Integer processId,
            @Nonnull Integer sourceProcessVersion,
            @Nonnull Integer targetProcessVersion
    ) throws ResponseException {
        var filter = ProcessInstanceAccessControlPresetFilter
                .create()
                .setTargetProcessId(processId)
                .setTargetProcessVersion(sourceProcessVersion);

        var originalPresets = processInstanceAccessControlPresetService
                .performList(Pageable.unpaged(), filter.build(), filter)
                .getContent();

        for (var originalPreset : originalPresets) {
            processInstanceAccessControlPresetService
                    .create(new ProcessInstanceAccessControlPresetEntity()
                            .setSourceTeamId(originalPreset.getSourceTeamId())
                            .setSourceDepartmentId(originalPreset.getSourceDepartmentId())
                            .setTargetProcessId(processId)
                            .setTargetProcessVersion(targetProcessVersion)
                            .setPermissions(new LinkedList<>(originalPreset.getPermissions())));
        }
    }

    @GetMapping("{id}/export/latest/")
    @Operation(
            summary = "Export Latest Process Definition Version",
            description = "Export the latest version of a process definition. " +
                    "Requires the permission `" + ProcessPermissionProvider.PROCESS_DEFINITION_READ +
                    "` for the affected process or at system level."
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
                    "Requires the permission `" + ProcessPermissionProvider.PROCESS_DEFINITION_READ +
                    "` for the affected process or at system level."
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

        permissionService.requireProcessPermission(
                execUser.getId(),
                existing.getId(),
                ProcessPermissionProvider.PROCESS_DEFINITION_READ
        );

        var result = processExportService
                .export(id, version);

        auditService.create()
                .withUser(execUser)
                .withAuditAction(AuditAction.Export, ProcessEntity.class,
                        existing.getId(),
                        "id"
                )
                .withMessage("Der Prozess %s (%d) wurde von der Mitarbeiter:in %s exportiert."
                        .formatted(
                                StringUtils.quote(existing.getInternalTitle()),
                                existing.getId(),
                                StringUtils.quote(execUser.getFullName())
                        )
                )
                .log();

        return result;
    }

    public record ProcessSlugAvailabilityResponse(boolean available) {
    }
}
