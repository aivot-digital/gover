package de.aivot.prosuna.backend.user.controllers;

import de.aivot.prosuna.backend.core.GenericReadController;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.openApi.OpenApiConstants;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.user.entities.VUserDeputyWithDetailsEntity;
import de.aivot.prosuna.backend.user.filters.VUserDeputyWithDetailsFilter;
import de.aivot.prosuna.backend.user.permissions.UserPermissionProvider;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import de.aivot.prosuna.backend.user.services.VUserDeputyWithDetailsService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/user-deputies-with-details/")
@Tag(
        name = OpenApiConstants.Tags.UserDeputiesName,
        description = OpenApiConstants.Tags.UserDeputiesDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class VUserDeputyWithDetailsController extends GenericReadController<VUserDeputyWithDetailsEntity, Integer, VUserDeputyWithDetailsFilter> {
    private final PermissionService permissionService;
    private final VUserDeputyWithDetailsService service;

    public VUserDeputyWithDetailsController(UserService userService,
                                            VUserDeputyWithDetailsService service,
                                            PermissionService permissionService) {
        super(userService, service);
        this.service = service;
        this.permissionService = permissionService;
    }

    @Override
    protected void checkListPermissions(@Nonnull UserEntity user) throws ResponseException {
        // List access is validated in performList so users can still see deputy relations they are part of.
    }

    @Override
    protected Page<VUserDeputyWithDetailsEntity> performList(@Nonnull UserEntity user,
                                                             @Nonnull Pageable pageable,
                                                             @Nonnull VUserDeputyWithDetailsFilter filter) throws ResponseException {
        var specification = filter.build();

        if (!permissionService.hasSystemPermission(user.getId(), UserPermissionProvider.DEPUTY_READ)) {
            specification = addRelatedDeputyRestriction(specification, user.getId());
        }

        return service.performList(pageable, specification, filter);
    }

    @Override
    protected void checkRetrievePermissions(@Nonnull UserEntity execUser,
                                            @Nonnull Integer itemId) throws ResponseException {
        var entity = service
                .retrieve(itemId)
                .orElseThrow(ResponseException::notFound);

        if (permissionService.hasSystemPermission(execUser.getId(), UserPermissionProvider.DEPUTY_READ)
                || isRelatedToUser(entity, execUser.getId())) {
            return;
        }

        throw ResponseException.forbidden();
    }

    private boolean isRelatedToUser(@Nonnull VUserDeputyWithDetailsEntity entity,
                                    @Nonnull String userId) {
        return userId.equals(entity.getOriginalUserId())
                || userId.equals(entity.getDeputyUserId());
    }

    @Nonnull
    private Specification<VUserDeputyWithDetailsEntity> addRelatedDeputyRestriction(
            @Nullable Specification<VUserDeputyWithDetailsEntity> baseSpecification,
            @Nonnull String userId
    ) {
        Specification<VUserDeputyWithDetailsEntity> relatedSpecification = (root, query, criteriaBuilder) ->
                criteriaBuilder.or(
                        criteriaBuilder.equal(root.get("originalUserId"), userId),
                        criteriaBuilder.equal(root.get("deputyUserId"), userId)
                );

        if (baseSpecification == null) {
            return relatedSpecification;
        }

        return baseSpecification.and(relatedSpecification);
    }
}
