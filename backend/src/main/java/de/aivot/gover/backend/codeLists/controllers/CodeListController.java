package de.aivot.gover.backend.codeLists.controllers;

import de.aivot.gover.backend.audit.enums.AuditAction;
import de.aivot.gover.backend.audit.services.AuditService;
import de.aivot.gover.backend.audit.services.ScopedAuditService;
import de.aivot.gover.backend.codeLists.entities.CodeListEntity;
import de.aivot.gover.backend.codeLists.entities.CodeListItemEntity;
import de.aivot.gover.backend.codeLists.entities.VCodeListItemEntity;
import de.aivot.gover.backend.codeLists.filters.CodeListFilter;
import de.aivot.gover.backend.codeLists.services.CodeListService;
import de.aivot.gover.backend.codeLists.services.CodeListWorker;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.openApi.OpenApiConstants;
import de.aivot.gover.backend.user.services.UserService;
import de.aivot.gover.backend.utils.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

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

    @Autowired
    public CodeListController(AuditService auditService,
                              CodeListService service,
                              UserService userService,
                              CodeListWorker codeListWorker) {
        this.auditService = auditService.createScopedAuditService(CodeListController.class, "Code-Listen");
        this.service = service;
        this.userService = userService;
        this.codeListWorker = codeListWorker;
    }

    @GetMapping("")
    @Operation(
            summary = "List Code Lists",
            description = "Retrieve a paginated list of code lists with optional filtering."
    )
    public Page<CodeListEntity> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid CodeListFilter filter
    ) throws ResponseException {
        // TODO: Permission Check

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
        // TODO: Permission Check

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
                        created.getId(),
                        "id"
                )
                .withMessage(
                        "Eine neue Code-Liste wurde von der Mitarbeiter:in %s erstellt.",
                        StringUtils.quote(execUser.getFullName())
                )
                .log();

        codeListWorker
                .triggerCodeListUpdate(created.getId(), false);

        return created;
    }

    @GetMapping("{codeListId}/")
    @Operation(
            summary = "Retrieve Code List",
            description = "Retrieve a specific code list by its ID."
    )
    public CodeListEntity retrieve(
            @Nonnull @PathVariable Integer codeListId
    ) throws ResponseException {
        // TODO: Permission Check
        return service
                .retrieve(codeListId)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{codeListId}/")
    @Operation(
            summary = "Update Code List",
            description = "Update an existing code list."
    )
    public CodeListEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer codeListId,
            @Nonnull @Valid @RequestBody CodeListEntity update
    ) throws ResponseException {
        // TODO: Permission Check

        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var updated = service
                .update(codeListId, update);

        auditService.create()
                .withUser(execUser)
                .withAuditAction(
                        AuditAction.Update,
                        CodeListEntity.class,
                        updated.getId(),
                        "id"
                )
                .withMessage(
                        "Die Code-Liste mit der ID %d wurde von der Mitarbeiter:in %s aktualisiert.",
                        updated.getId(),
                        StringUtils.quote(execUser.getFullName())
                )
                .log(); // TODO: Add Diff

        codeListWorker
                .triggerCodeListUpdate(updated.getId(), true);

        return updated;
    }

    @DeleteMapping("{codeListId}/")
    @Operation(
            summary = "Delete Code List",
            description = "Delete a specific code list by its ID."
    )
    public void destroy(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer codeListId
    ) throws ResponseException {
        // TODO: Permission Check

        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var deleted = service.delete(codeListId);

        auditService.create()
                .withUser(execUser)
                .withAuditAction(
                        AuditAction.Delete,
                        CodeListEntity.class,
                        deleted.getId(),
                        "id"
                )
                .withMessage(
                        "Die Code-Liste mit der ID %d wurde von der Mitarbeiter:in %s gelöscht.",
                        deleted.getId(),
                        StringUtils.quote(execUser.getFullName())
                )
                .log();
    }

    @GetMapping("{codeListId}/export.csv")
    public Resource exportCSV(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer codeListId
    ) throws ResponseException {
        // TODO: Permission Check
        throw ResponseException.methodNotAllowed("CSV-Export ist noch nicht implementiert.");
    }

    @GetMapping("{codeListId}/update/")
    public Object updateItems(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer codeListId,
            @Nonnull @RequestParam(defaultValue = "true") Boolean keepOutdated
    ) throws ResponseException {
        // TODO: Permission Check

        var cl = service
                .retrieve(codeListId)
                .orElseThrow(ResponseException::notFound);

        codeListWorker
                .triggerCodeListUpdate(cl.getId(), keepOutdated);

        return Map.of(
                "status", "ok"
        );
    }

    @GetMapping("{codeListId}/items/")
    public Page<VCodeListItemEntity> listItems(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer codeListId,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable
    ) throws ResponseException {
        // TODO: Permission Check
        return service.listItems(codeListId, pageable);
    }

    @PostMapping("{codeListId}/items/")
    public VCodeListItemEntity createItem(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer codeListId,
            @Nonnull @Valid @RequestBody CodeListItemEntity item
    ) throws ResponseException {
        // TODO: Permission Check
        return service.createItem(codeListId, item);
    }

    @GetMapping("{codeListId}/items/{itemId}/")
    public VCodeListItemEntity getItem(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer codeListId,
            @Nonnull @PathVariable Long itemId
    ) throws ResponseException {
        // TODO: Permission Check
        return service.getItem(codeListId, itemId);
    }

    @PutMapping("{codeListId}/items/{itemId}/")
    public VCodeListItemEntity updateItem(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer codeListId,
            @Nonnull @PathVariable Long itemId,
            @Nonnull @Valid @RequestBody CodeListItemEntity item
    ) throws ResponseException {
        // TODO: Permission Check
        return service.updateItem(codeListId, itemId, item);
    }

    @DeleteMapping("{codeListId}/items/{itemId}/")
    public void deleteItem(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer codeListId,
            @Nonnull @PathVariable Long itemId
    ) throws ResponseException {
        // TODO: Permission Check
        service.deleteItem(codeListId, itemId);
    }
}
