package de.aivot.gover.backend.process.controllers;

import de.aivot.gover.backend.audit.enums.AuditAction;
import de.aivot.gover.backend.audit.services.AuditService;
import de.aivot.gover.backend.audit.services.ScopedAuditService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.openApi.OpenApiConfiguration;
import de.aivot.gover.backend.openApi.OpenApiConstants;
import de.aivot.gover.backend.permissions.services.PermissionService;
import de.aivot.gover.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.gover.backend.process.filters.ProcessInstanceAttachmentFilter;
import de.aivot.gover.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.gover.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.gover.backend.user.services.UserService;
import de.aivot.gover.backend.utils.StringUtils;
import de.aivot.gover.backend.storage.services.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
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
    private final StorageService storageService;
    private final PermissionService permissionService;

    @Autowired
    public ProcessInstanceAttachmentController(AuditService auditService,
                                               UserService userService,
                                               ProcessInstanceAttachmentService processInstanceAttachmentService,
                                               StorageService storageService,
                                               PermissionService permissionService) {
        this.auditService = auditService.createScopedAuditService(ProcessInstanceAttachmentController.class, "Prozesse");
        this.userService = userService;
        this.processInstanceAttachmentService = processInstanceAttachmentService;
        this.storageService = storageService;
        this.permissionService = permissionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Process Instance Attachments",
            description = "List all process instance attachments with optional filtering and pagination."
    )
    public Page<ProcessInstanceAttachmentEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid ProcessInstanceAttachmentFilter filter
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!permissionService.hasSystemPermission(user.getId(), ProcessPermissionProvider.PROCESS_INSTANCE_READ)) {
            if (filter.getProcessInstanceId() != null) {
                permissionService.requireProcessInstancePermission(
                        user.getId(),
                        filter.getProcessInstanceId(),
                        ProcessPermissionProvider.PROCESS_INSTANCE_READ
                );
            } else {
                var accessibleProcessInstanceIds = permissionService
                        .getProcessInstancesWithPermission(user.getId(), ProcessPermissionProvider.PROCESS_INSTANCE_READ);

                if (filter.getProcessInstanceIds() != null) {
                    accessibleProcessInstanceIds = filter.getProcessInstanceIds()
                            .stream()
                            .filter(accessibleProcessInstanceIds::contains)
                            .toList();
                }

                if (accessibleProcessInstanceIds.isEmpty()) {
                    return Page.empty(pageable);
                }

                filter.setProcessInstanceIds(accessibleProcessInstanceIds);
            }
        }

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

        permissionService.requireProcessInstancePermission(
                execUser.getId(),
                processInstanceId,
                ProcessPermissionProvider.PROCESS_INSTANCE_UPDATE
        );

        // Save attachment entity (actual file storage logic should be implemented in service)
        var attachment = new ProcessInstanceAttachmentEntity()
                .setKey(UUID.randomUUID())
                .setProcessInstanceId(processInstanceId)
                .setProcessInstanceTaskId(processInstanceTaskId)
                .setUploadedByUserId(execUser.getId());

        // TODO: Store the file bytes somewhere, e.g. in a storage service

        processInstanceAttachmentService.create(attachment);

        auditService.create().withUser(execUser).withAuditAction(AuditAction.Create, ProcessInstanceAttachmentEntity.class, attachment.getKey(), "key", Map.of(
                "key", attachment.getKey(),
                "processInstanceId", attachment.getProcessInstanceId(),
                "processInstanceTaskId", attachment.getProcessInstanceTaskId()
        )).withMessage(
                "Der Anhang mit dem Schlüssel %s für die Prozessinstanz %s wurde von der Mitarbeiter:in %s erstellt.",
                StringUtils.quote(String.valueOf(attachment.getKey())),
                StringUtils.quote(String.valueOf(attachment.getProcessInstanceId())),
                StringUtils.quote(execUser.getFullName())
        ).log();

        return attachment;
    }

    @GetMapping("{key}/")
    @Operation(
            summary = "Retrieve Process Instance Attachment",
            description = "Retrieve a process instance attachment by its key."
    )
    public ProcessInstanceAttachmentEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID key
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var attachment = processInstanceAttachmentService
                .retrieve(key)
                .orElseThrow(ResponseException::notFound);

        permissionService.requireProcessInstancePermission(
                user.getId(),
                attachment.getProcessInstanceId(),
                ProcessPermissionProvider.PROCESS_INSTANCE_READ
        );

        return attachment;
    }

    @GetMapping("{key}/file/")
    @Operation(
            summary = "Download Process Instance Attachment",
            description = "Streams the file of a process instance attachment by its key."
    )
    public ResponseEntity<InputStreamResource> download(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID key,
            @RequestParam(defaultValue = "true") boolean download
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var attachment = processInstanceAttachmentService
                .retrieve(key)
                .orElseThrow(ResponseException::notFound);

        permissionService.requireProcessInstancePermission(
                user.getId(),
                attachment.getProcessInstanceId(),
                ProcessPermissionProvider.PROCESS_INSTANCE_READ
        );

        var inputStream = storageService
                .getDocumentContent(attachment.getStorageProviderId(), attachment.getStoragePathFromRoot());

        MediaType mediaType;
        try {
            var mimeType = URLConnection.guessContentTypeFromName(attachment.getFileName());
            mediaType = mimeType != null ? MediaType.parseMediaType(mimeType) : MediaType.APPLICATION_OCTET_STREAM;
        } catch (InvalidMediaTypeException e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok().contentType(mediaType);
        var contentDispositionType = download ? "attachment" : "inline";
        var contentDisposition = ContentDisposition
                .builder(contentDispositionType)
                .filename(attachment.getFileName(), StandardCharsets.UTF_8)
                .build();
        responseBuilder.header("Content-Disposition", contentDisposition.toString());

        return responseBuilder.body(new InputStreamResource(inputStream));
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

        permissionService.requireProcessInstancePermission(
                execUser.getId(),
                existing.getProcessInstanceId(),
                ProcessPermissionProvider.PROCESS_INSTANCE_UPDATE
        );

        updateDTO.setKey(existing.getKey());

        var result = processInstanceAttachmentService
                .update(key, updateDTO);

        auditService.create().withUser(execUser).withAuditAction(AuditAction.Update, ProcessInstanceAttachmentEntity.class, result.getKey(), "key", Map.of(
                "key", result.getKey(),
                "processInstanceId", result.getProcessInstanceId(),
                "processInstanceTaskId", result.getProcessInstanceTaskId()
        )).withMessage(
                "Der Anhang mit dem Schlüssel %s für die Prozessinstanz %s wurde von der Mitarbeiter:in %s aktualisiert.",
                StringUtils.quote(String.valueOf(result.getKey())),
                StringUtils.quote(String.valueOf(result.getProcessInstanceId())),
                StringUtils.quote(execUser.getFullName())
        ).log();

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
                .orElseThrow(ResponseException::unauthorized);

        var existing = processInstanceAttachmentService
                .retrieve(key)
                .orElseThrow(ResponseException::notFound);

        permissionService.requireProcessInstancePermission(
                user.getId(),
                existing.getProcessInstanceId(),
                ProcessPermissionProvider.PROCESS_INSTANCE_UPDATE
        );

        var deleted = processInstanceAttachmentService
                .delete(key);

        auditService.create().withUser(user).withAuditAction(AuditAction.Delete, ProcessInstanceAttachmentEntity.class, deleted.getKey(), "key", Map.of(
                "key", deleted.getKey(),
                "processInstanceId", deleted.getProcessInstanceId(),
                "processInstanceTaskId", deleted.getProcessInstanceTaskId()
        )).withMessage(
                "Der Anhang mit dem Schlüssel %s für die Prozessinstanz %s wurde von der Mitarbeiter:in %s gelöscht.",
                StringUtils.quote(String.valueOf(deleted.getKey())),
                StringUtils.quote(String.valueOf(deleted.getProcessInstanceId())),
                StringUtils.quote(user.getFullName())
        ).log();
    }
}
