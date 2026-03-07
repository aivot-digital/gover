package de.aivot.GoverBackend.user.controllers;

import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.core.GenericCrudController;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.permissions.services.PermissionService;
import de.aivot.GoverBackend.user.entities.UserDeputyEntity;
import de.aivot.GoverBackend.user.filters.UserDeputyFilter;
import de.aivot.GoverBackend.user.permissions.UserPermissionProvider;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.services.UserDeputyService;
import de.aivot.GoverBackend.user.services.UserService;
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
@RequestMapping("/api/user-deputies/")
@Tag(
        name = OpenApiConstants.Tags.UserDeputiesName,
        description = OpenApiConstants.Tags.UserDeputiesDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class UserDeputyController extends GenericCrudController<UserDeputyEntity, Integer, UserDeputyFilter> {
    private final PermissionService permissionService;
    private final UserDeputyService userDeputyService;

    public UserDeputyController(AuditService auditService,
                                UserService userService,
                                UserDeputyService service,
                                PermissionService permissionService) {
        super(auditService.createScopedAuditService(UserDeputyController.class),
                userService,
                service);
        this.permissionService = permissionService;
        this.userDeputyService = service;
    }

    @Override
    protected Integer getIdForEntity(UserDeputyEntity entity) {
        return entity.getId();
    }

    @Override
    protected void checkListPermissions(@Nonnull UserEntity user) throws ResponseException {
        // List access is validated in performList to allow owner-based fallback.
    }

    @Override
    protected Page<UserDeputyEntity> performList(@Nonnull UserEntity user,
                                                 @Nonnull Pageable pageable,
                                                 @Nonnull UserDeputyFilter filter) throws ResponseException {
        var specification = filter.build();

        // Without global read permission, users may only list deputy relations they are part of.
        if (!hasDeputyPermission(user.getId(), UserPermissionProvider.DEPUTY_READ)) {
            specification = addRelatedDeputyRestriction(specification, user.getId());
        }

        return userDeputyService.performList(pageable, specification, filter);
    }

    @Override
    protected void checkRetrievePermissions(@Nonnull UserEntity execUser,
                                            @Nonnull Integer itemid) throws ResponseException {
        var entity = userDeputyService
                .retrieve(itemid)
                .orElseThrow(ResponseException::notFound);
        testPermissionOrRelated(execUser, UserPermissionProvider.DEPUTY_READ, entity);
    }

    @Override
    protected void checkCreatePermissions(@Nonnull UserEntity execUser,
                                          @Nonnull UserDeputyEntity newItem) throws ResponseException {
        testPermissionOrRelated(execUser, UserPermissionProvider.DEPUTY_CREATE, newItem);
    }

    @Override
    protected void checkUpdatePermission(@Nonnull UserEntity execUser,
                                         @Nonnull Integer itemid) throws ResponseException {
        var entity = userDeputyService
                .retrieve(itemid)
                .orElseThrow(ResponseException::notFound);
        testPermissionOrRelated(execUser, UserPermissionProvider.DEPUTY_UPDATE, entity);
    }

    @Override
    protected void checkDeletePermission(@Nonnull UserEntity execUser,
                                         @Nonnull Integer itemid) throws ResponseException {
        var entity = userDeputyService
                .retrieve(itemid)
                .orElseThrow(ResponseException::notFound);
        testPermissionOrRelated(execUser, UserPermissionProvider.DEPUTY_DELETE, entity);
    }

    private void testPermissionOrRelated(@Nonnull UserEntity user,
                                         @Nonnull String permission,
                                         @Nonnull UserDeputyEntity deputyEntity) throws ResponseException {
        if (hasDeputyPermission(user.getId(), permission)) {
            return;
        }

        // Fallback rule: users without the global permission may still access their own deputy relations.
        if (isRelatedToUser(deputyEntity, user.getId())) {
            return;
        }

        throw ResponseException.forbidden();
    }

    private boolean hasDeputyPermission(@Nonnull String userId,
                                        @Nonnull String permission) {
        return permissionService.hasSystemPermission(userId, permission);
    }

    private boolean isRelatedToUser(@Nonnull UserDeputyEntity deputyEntity,
                                    @Nonnull String userId) {
        return userId.equals(deputyEntity.getOriginalUserId())
                || userId.equals(deputyEntity.getDeputyUserId());
    }

    @Nonnull
    private Specification<UserDeputyEntity> addRelatedDeputyRestriction(@Nullable Specification<UserDeputyEntity> baseSpecification,
                                                                        @Nonnull String userId) {
        Specification<UserDeputyEntity> relatedSpecification = (root, query, criteriaBuilder) ->
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
