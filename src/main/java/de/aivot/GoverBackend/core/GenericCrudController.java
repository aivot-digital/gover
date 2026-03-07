package de.aivot.GoverBackend.core;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.models.AuditLogPayload;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// TODO: Merge with GenericReadController?
public abstract class GenericCrudController<T, I, F extends Filter<T>> {
    private final ScopedAuditService auditService;
    private final UserService userService;
    private final EntityService<T, I> service;

    public GenericCrudController(ScopedAuditService auditService,
                                 UserService userService,
                                 EntityService<T, I> service) {
        this.auditService = auditService;
        this.userService = userService;
        this.service = service;
    }

    @GetMapping("")
    @Operation(
            summary = "List Items",
            description = "List all items with optional filtering and pagination."
    )
    public Page<T> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid F filter
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        checkListPermissions(execUser);

        return performList(execUser, pageable, filter);
    }

    /**
     * Check if the user has permissions to list the items.
     *
     * @param user The user entity.
     * @throws ResponseException If the user does not have permissions.
     */
    protected void checkListPermissions(@Nonnull UserEntity user) throws ResponseException {
        // Default implementation does nothing. Override in subclasses to enforce permissions.
    }

    /**
     * Perform the listing of items with the given filter and pagination.
     *
     * @param user     The user entity.
     * @param pageable Pagination information.
     * @param filter   The filter to apply.
     * @return A page of items.
     */
    protected Page<T> performList(@Nonnull UserEntity user,
                                  @Nonnull Pageable pageable,
                                  @Nonnull F filter) throws ResponseException {
        return service.list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Item",
            description = "Create a new item."
    )
    public T create(
            @Nonnull @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid T newItem
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        checkCreatePermissions(execUser, newItem);

        var createdItem = performCreate(execUser, newItem);

        auditService.addAuditEntry(
                AuditLogPayload
                        .create()
                        .withUser(execUser)
                        .withAuditAction(
                                AuditAction.Create,
                                createdItem.getClass(),
                                getIdForEntity(createdItem).toString(),
                                Map.of(/* TODO: Create Data Map to Identify */)
                        )
        );

        return createdItem;
    }

    protected void checkCreatePermissions(@Nonnull UserEntity execUser,
                                          @Nonnull T newItem) throws ResponseException {
        // Default implementation does nothing. Override in subclasses to enforce permissions.
    }

    protected T performCreate(@Nonnull UserEntity execUser,
                              @Nonnull T newItem) throws ResponseException {
        return service.create(newItem);
    }

    abstract protected I getIdForEntity(T entity);

    @GetMapping("{itemId}/")
    @Operation(
            summary = "Retrieve Item",
            description = "Retrieve an item by its ID."
    )
    public T retrieve(
            @Nonnull @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable I itemId
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        checkRetrievePermissions(execUser, itemId);

        return performRetrieve(execUser, itemId);
    }

    protected void checkRetrievePermissions(@Nonnull UserEntity execUser,
                                            @Nonnull I itemid) throws ResponseException {
        // Default implementation does nothing. Override in subclasses to enforce permissions.
    }

    protected T performRetrieve(@Nonnull UserEntity execUser,
                                @Nonnull I itemId) throws ResponseException {
        return service
                .retrieve(itemId)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    @Operation(
            summary = "Update Item",
            description = "Update an item by its ID."
    )
    public T update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable I id,
            @Nonnull @RequestBody @Valid T patchItem
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        checkUpdatePermission(execUser, id);

        var before = service
                .retrieve(id)
                .orElse(null);

        var result = performUpdate(execUser, id, patchItem);

        var diff = AuditLogPayload.createDiff(
                AuditLogPayload.toMap(before),
                AuditLogPayload.toMap(result)
        );

        auditService.addAuditEntry(
                AuditLogPayload
                        .create()
                        .withUser(execUser)
                        .withAuditAction(
                                AuditAction.Update,
                                result.getClass(),
                                id,
                                Map.of(/* TODO: Create Data Map */)
                        )
                        .setDiff(diff)
        );

        return result;
    }

    protected void checkUpdatePermission(@Nonnull UserEntity execUser,
                                         @Nonnull I itemid) throws ResponseException {
        // Default implementation does nothing. Override in subclasses to enforce permissions.
    }

    protected T performUpdate(@Nonnull UserEntity execUser,
                              @Nonnull I itemId,
                              @Nonnull T patchItem) throws ResponseException {
        return service
                .update(itemId, patchItem);
    }

    @DeleteMapping("{id}/")
    @Operation(
            summary = "Delete Item",
            description = "Delete an item by its ID."
    )
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable I id
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        checkDeletePermission(execUser, id);

        var deleted = performDelete(execUser, id);

        auditService.addAuditEntry(
                AuditLogPayload
                        .create()
                        .withUser(execUser)
                        .withAuditAction(
                                AuditAction.Delete,
                                deleted.getClass(),
                                id,
                                Map.of(/* TODO: Create Data Map */)
                        )
        );
    }

    protected void checkDeletePermission(@Nonnull UserEntity execUser,
                                         @Nonnull I itemid) throws ResponseException {
        // Default implementation does nothing. Override in subclasses to enforce permissions.
    }

    protected T performDelete(@Nonnull UserEntity execUser,
                              @Nonnull I itemId) throws ResponseException {
        return service
                .delete(itemId);
    }
}
