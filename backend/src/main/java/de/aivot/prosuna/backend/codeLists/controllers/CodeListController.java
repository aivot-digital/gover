package de.aivot.prosuna.backend.codeLists.controllers;

import de.aivot.prosuna.backend.audit.enums.AuditAction;
import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.audit.services.ScopedAuditService;
import de.aivot.prosuna.backend.codeLists.entities.CodeListEntity;
import de.aivot.prosuna.backend.codeLists.entities.CodeListItemEntity;
import de.aivot.prosuna.backend.codeLists.entities.VCodeListItemEntity;
import de.aivot.prosuna.backend.codeLists.filters.CodeListFilter;
import de.aivot.prosuna.backend.codeLists.permissions.CodeListPermissionProvider;
import de.aivot.prosuna.backend.codeLists.services.CodeListService;
import de.aivot.prosuna.backend.codeLists.services.CodeListWorker;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConstants;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.user.services.UserService;
import de.aivot.prosuna.backend.utils.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/code-lists/")
@Tag(
        name = OpenApiConstants.Tags.CodeListName,
        description = OpenApiConstants.Tags.CodeListDescription
)
public class CodeListController {
    private final ScopedAuditService auditService;
    private final CodeListService service;
    private final UserService userService;
    private final CodeListWorker codeListWorker;
    private final PermissionService permissionService;

    @Autowired
    public CodeListController(AuditService auditService,
                              CodeListService service,
                              UserService userService,
                              CodeListWorker codeListWorker, PermissionService permissionService) {
        this.auditService = auditService.createScopedAuditService(CodeListController.class, "Codelisten");
        this.service = service;
        this.userService = userService;
        this.codeListWorker = codeListWorker;
        this.permissionService = permissionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Code Lists",
            description = "Retrieve a paginated list of code lists with optional filtering."
    )
    public Page<CodeListEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid CodeListFilter filter
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, CodeListPermissionProvider.CODE_LIST_READ);

        return service
                .list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Code List",
            description = "Create a new code list."
    )
    public CodeListEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody CodeListEntity create
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, CodeListPermissionProvider.CODE_LIST_CREATE);

        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var created = service
                .create(create);

        auditService.create()
                .withUser(execUser)
                .withAuditAction(
                        AuditAction.Create,
                        CodeListEntity.class,
                        created.getKey(),
                        "key"
                )
                .withMessage(
                        "Eine neue Codeliste mit dem Schlüssel %s wurde von der Mitarbeiter:in %s erstellt.",
                        StringUtils.quote(created.getKey()),
                        StringUtils.quote(execUser.getFullName())
                )
                .log();

        codeListWorker
                .triggerCodeListUpdate(created.getKey(), false);

        return created;
    }

    @GetMapping("{codeListKey}/")
    @Operation(
            summary = "Retrieve Code List",
            description = "Retrieve a specific code list by its key."
    )
    public CodeListEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String codeListKey
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, CodeListPermissionProvider.CODE_LIST_READ);

        return service
                .retrieve(codeListKey)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{codeListKey}/")
    @Operation(
            summary = "Update Code List",
            description = "Update an existing code list."
    )
    public CodeListEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String codeListKey,
            @Nonnull @Valid @RequestBody CodeListEntity update
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, CodeListPermissionProvider.CODE_LIST_UPDATE);

        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var updated = service
                .update(codeListKey, update);

        auditService.create()
                .withUser(execUser)
                .withAuditAction(
                        AuditAction.Update,
                        CodeListEntity.class,
                        updated.getKey(),
                        "key"
                )
                .withMessage(
                        "Die Codeliste mit dem Schlüssel %s wurde von der Mitarbeiter:in %s aktualisiert.",
                        StringUtils.quote(updated.getKey()),
                        StringUtils.quote(execUser.getFullName())
                )
                .log(); // TODO: Add Diff

        codeListWorker
                .triggerCodeListUpdate(updated.getKey(), true);

        return updated;
    }

    @DeleteMapping("{codeListKey}/")
    @Operation(
            summary = "Delete Code List",
            description = "Delete a specific code list by its key."
    )
    public void destroy(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String codeListKey
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, CodeListPermissionProvider.CODE_LIST_DELETE);

        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var deleted = service.delete(codeListKey);

        auditService.create()
                .withUser(execUser)
                .withAuditAction(
                        AuditAction.Delete,
                        CodeListEntity.class,
                        deleted.getKey(),
                        "key"
                )
                .withMessage(
                        "Die Codeliste mit dem Schlüssel %s wurde von der Mitarbeiter:in %s gelöscht.",
                        StringUtils.quote(deleted.getKey()),
                        StringUtils.quote(execUser.getFullName())
                )
                .log();
    }

    @GetMapping("{codeListKey}/export.csv")
    public ResponseEntity<Resource> exportCSV(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String codeListKey
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, CodeListPermissionProvider.CODE_LIST_EXPORT);

        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var bytes = service.exportCSV(codeListKey);

        auditService.create()
                .withUser(execUser)
                .withAuditAction(
                        AuditAction.Export,
                        CodeListEntity.class,
                        codeListKey,
                        "key"
                )
                .withMessage(
                        "Die Codeliste mit dem Schlüssel %s wurde von der Mitarbeiter:in %s exportiert.",
                        StringUtils.quote(codeListKey),
                        StringUtils.quote(execUser.getFullName())
                )
                .log();

        return ResponseEntity
                .ok()
                .contentType(MediaType.valueOf("text/csv"))
                .contentLength(bytes.length)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition
                                .attachment()
                                .filename("code-list-%s.csv".formatted(codeListKey), StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(new ByteArrayResource(bytes));
    }

    @PostMapping(
            value = "{codeListKey}/import.csv",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public CodeListEntity importCSV(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String codeListKey,
            @Nonnull @RequestPart("file") MultipartFile file
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, CodeListPermissionProvider.CODE_LIST_UPDATE);

        try {
            return service.importCSV(codeListKey, file.getInputStream());
        } catch (IOException e) {
            throw ResponseException.badRequest("Die CSV-Datei konnte nicht gelesen werden: " + e.getMessage(), e);
        }
    }

    @GetMapping("{codeListKey}/update/")
    public Object updateItems(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String codeListKey,
            @Nonnull @RequestParam(defaultValue = "true") Boolean keepOutdated
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, CodeListPermissionProvider.CODE_LIST_UPDATE);

        var cl = service
                .retrieve(codeListKey)
                .orElseThrow(ResponseException::notFound);

        codeListWorker
                .triggerCodeListUpdate(cl.getKey(), keepOutdated);

        return Map.of(
                "status", "ok"
        );
    }

    @GetMapping("{codeListKey}/items/")
    public Page<VCodeListItemEntity> listItems(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String codeListKey,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, CodeListPermissionProvider.CODE_LIST_READ);

        return service.listItems(codeListKey, pageable);
    }

    @PostMapping("{codeListKey}/items/")
    public VCodeListItemEntity createItem(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String codeListKey,
            @Nonnull @Valid @RequestBody CodeListItemEntity item
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, CodeListPermissionProvider.CODE_LIST_UPDATE);

        return service.createItem(codeListKey, item);
    }

    @GetMapping("{codeListKey}/items/{itemId}/")
    public VCodeListItemEntity getItem(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String codeListKey,
            @Nonnull @PathVariable Long itemId
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, CodeListPermissionProvider.CODE_LIST_READ);

        return service.getItem(codeListKey, itemId);
    }

    @PutMapping("{codeListKey}/items/{itemId}/")
    public VCodeListItemEntity updateItem(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String codeListKey,
            @Nonnull @PathVariable Long itemId,
            @Nonnull @Valid @RequestBody CodeListItemEntity item
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, CodeListPermissionProvider.CODE_LIST_UPDATE);

        return service.updateItem(codeListKey, itemId, item);
    }

    @DeleteMapping("{codeListKey}/items/{itemId}/")
    public void deleteItem(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String codeListKey,
            @Nonnull @PathVariable Long itemId
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, CodeListPermissionProvider.CODE_LIST_UPDATE);

        service.deleteItem(codeListKey, itemId);
    }
}
