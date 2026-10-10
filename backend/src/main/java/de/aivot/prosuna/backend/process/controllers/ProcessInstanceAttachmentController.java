package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.openApi.OpenApiConstants;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.filters.ProcessInstanceAttachmentFilter;
import de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.prosuna.backend.user.services.UserService;
import de.aivot.prosuna.backend.storage.services.StorageService;
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

import java.io.IOException;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/process-instance-attachments/")
@Tag(
        name = OpenApiConstants.Tags.ProcessesDefinitionsName,
        description = "Operations for reading process instance attachments."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessInstanceAttachmentController {
    /**
     * Media types that browsers display without executing active content. All other attachments are served as
     * downloads with a generic media type.
     */
    private static final Set<MediaType> INLINE_MEDIA_TYPES = Set.of(
            MediaType.APPLICATION_PDF,
            MediaType.IMAGE_PNG,
            MediaType.IMAGE_JPEG,
            MediaType.IMAGE_GIF,
            new MediaType("image", "webp"),
            MediaType.TEXT_PLAIN
    );

    // Attachments are created only through validated form and task execution flows and share the lifecycle of their
    // process instance. They cannot be moved, replaced, or deleted independently through this controller.
    private final UserService userService;
    private final ProcessInstanceAttachmentService processInstanceAttachmentService;
    private final StorageService storageService;
    private final PermissionService permissionService;

    @Autowired
    public ProcessInstanceAttachmentController(UserService userService,
                                               ProcessInstanceAttachmentService processInstanceAttachmentService,
                                               StorageService storageService,
                                               PermissionService permissionService) {
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

        if (!permissionService.hasSystemPermission(user.getId(), ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ)) {
            if (filter.getProcessInstanceId() != null) {
                permissionService.requireProcessInstancePermission(
                        user.getId(),
                        filter.getProcessInstanceId(),
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ
                );
            } else {
                var accessibleProcessInstanceIds = permissionService
                        .getProcessInstancesWithPermission(user.getId(), ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ);

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
                ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ
        );

        return attachment;
    }

    @GetMapping("{key}/file/")
    @Operation(
            summary = "Download Process Instance Attachment",
            description = "Streams the file of a process instance attachment by its key. " +
                    "Only PDF documents, PNG, JPEG, GIF and WebP images and plain text can be displayed inline. " +
                    "All other files are always served as downloads with the media type `application/octet-stream`. " +
                    "Requires `" + ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ + "` for the attachment's process instance."
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
                ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ
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

        // Attachments can originate from external senders, for example through webhooks or FIT-Connect. Browsers
        // must therefore never interpret active content such as HTML, SVG or XML on the application origin.
        var inlineAllowed = isInlineMediaType(mediaType);
        if (!inlineAllowed) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok().contentType(mediaType);
        var contentDispositionType = download || !inlineAllowed ? "attachment" : "inline";
        var contentDisposition = ContentDisposition
                .builder(contentDispositionType)
                .filename(attachment.getFileName(), StandardCharsets.UTF_8)
                .build();
        responseBuilder.header("Content-Disposition", contentDisposition.toString());

        return responseBuilder.body(new InputStreamResource(inputStream));
    }

    private static boolean isInlineMediaType(@Nonnull MediaType mediaType) {
        return INLINE_MEDIA_TYPES.contains(new MediaType(mediaType.getType(), mediaType.getSubtype()));
    }

}
