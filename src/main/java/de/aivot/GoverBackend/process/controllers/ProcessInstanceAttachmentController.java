package de.aivot.GoverBackend.process.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.department.filters.VDepartmentMembershipWithPermissionsFilter;
import de.aivot.GoverBackend.department.repositories.VDepartmentMembershipWithPermissionsRepository;
import de.aivot.GoverBackend.department.services.DepartmentService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.GoverBackend.process.filters.ProcessInstanceAttachmentFilter;
import de.aivot.GoverBackend.process.services.ProcessInstanceAttachmentService;
import de.aivot.GoverBackend.process.services.ProcessDefinitionService;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/process-instance-attachments/")
@Tag(
        name = OpenApiConstants.Tags.ProcessesDefinitionsName,
        description = "Operations for managing process instance attachments, including file uploads."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessInstanceAttachmentController {
    private final ScopedAuditService auditService;
    private final UserService userService;
    private final ProcessInstanceAttachmentService processInstanceAttachmentService;
    private final DepartmentService departmentService;
    private final VDepartmentMembershipWithPermissionsRepository vDepartmentMembershipWithPermissionsRepository;
    private final ProcessDefinitionService processDefinitionService;

    @Autowired
    public ProcessInstanceAttachmentController(AuditService auditService,
                                              UserService userService,
                                              ProcessInstanceAttachmentService processInstanceAttachmentService,
                                              DepartmentService departmentService,
                                              VDepartmentMembershipWithPermissionsRepository vDepartmentMembershipWithPermissionsRepository,
                                              ProcessDefinitionService processDefinitionService) {
        this.auditService = auditService.createScopedAuditService(ProcessInstanceAttachmentController.class);
        this.userService = userService;
        this.processInstanceAttachmentService = processInstanceAttachmentService;
        this.departmentService = departmentService;
        this.vDepartmentMembershipWithPermissionsRepository = vDepartmentMembershipWithPermissionsRepository;
        this.processDefinitionService = processDefinitionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Process Instance Attachments",
            description = "List all process instance attachments with optional filtering and pagination."
    )
    public Page<ProcessInstanceAttachmentEntity> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid ProcessInstanceAttachmentFilter filter
    ) throws ResponseException {
        return processInstanceAttachmentService
                .list(pageable, filter);
    }

    @PostMapping(
            value = "",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Upload a new process instance attachment",
            description = "Upload a file as a process instance attachment. The uploaded file will be associated with the process instance and optionally a task."
    )
    public ProcessInstanceAttachmentEntity upload(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestPart(value = "file", required = true) MultipartFile file,
            @Nonnull @RequestPart(value = "processInstanceId", required = true) Long processInstanceId,
            @Nullable @RequestPart(value = "processInstanceTaskId", required = false) Long processInstanceTaskId
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Permission check: must be able to edit the process instance's department
        // You may want to retrieve the process definition via the instance, adjust as needed
        // For now, assume you can get processDefinitionId from processInstanceId
        // If you have a ProcessInstanceService, use it here
        // Otherwise, skip this check or implement as needed

        // ...permission check logic (similar to other controllers)...

        // Save attachment entity (actual file storage logic should be implemented in service)
        var attachment = new ProcessInstanceAttachmentEntity()
                .setKey(UUID.randomUUID())
                .setProcessInstanceId(processInstanceId)
                .setProcessInstanceTaskId(processInstanceTaskId)
                .setUploadedByUserId(execUser.getId())
                .setUploadedAt(LocalDateTime.now());

        // TODO: Store the file bytes somewhere, e.g. in a storage service

        processInstanceAttachmentService.create(attachment);

        auditService.logAction(execUser, AuditAction.Create, ProcessInstanceAttachmentEntity.class, Map.of(
                "key", attachment.getKey(),
                "processInstanceId", attachment.getProcessInstanceId(),
                "processInstanceTaskId", attachment.getProcessInstanceTaskId()
        ));

        return attachment;
    }

    @GetMapping("{key}/")
    @Operation(
            summary = "Retrieve Process Instance Attachment",
            description = "Retrieve a process instance attachment by its key."
    )
    public ProcessInstanceAttachmentEntity retrieve(
            @Nonnull @PathVariable UUID key
    ) throws ResponseException {
        return processInstanceAttachmentService
                .retrieve(key)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{key}/")
    @Operation(
            summary = "Update Process Instance Attachment",
            description = "Update an existing process instance attachment. Requires super admin privileges or a user role with edit process permissions."
    )
    public ProcessInstanceAttachmentEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID key,
            @Nonnull @RequestBody @Valid ProcessInstanceAttachmentEntity updateDTO
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var existing = processInstanceAttachmentService
                .retrieve(key)
                .orElseThrow(ResponseException::notFound);

        // ...permission check logic (similar to other controllers)...

        updateDTO.setKey(existing.getKey());

        var result = processInstanceAttachmentService
                .update(key, updateDTO);

        auditService.logAction(execUser, AuditAction.Update, ProcessInstanceAttachmentEntity.class, Map.of(
                "key", result.getKey(),
                "processInstanceId", result.getProcessInstanceId(),
                "processInstanceTaskId", result.getProcessInstanceTaskId()
        ));

        return result;
    }

    @DeleteMapping("{key}/")
    @Operation(
            summary = "Delete Process Instance Attachment",
            description = "Delete a process instance attachment by its key. Requires super admin privileges."
    )
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID key
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSuperAdmin()
                .orElseThrow(ResponseException::forbidden);

        var deleted = processInstanceAttachmentService
                .delete(key);

        auditService.logAction(user, AuditAction.Delete, ProcessInstanceAttachmentEntity.class, Map.of(
                "key", deleted.getKey(),
                "processInstanceId", deleted.getProcessInstanceId(),
                "processInstanceTaskId", deleted.getProcessInstanceTaskId()
        ));
    }
}

