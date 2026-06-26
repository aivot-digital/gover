package de.aivot.GoverBackend.core;

import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.lib.services.ReadEntityService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public abstract class GenericReadController<T, I, F extends Filter<T>> {
    private final UserService userService;
    private final ReadEntityService<T, I> service;

    public GenericReadController(UserService userService,
                                 ReadEntityService<T, I> service) {
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
}
