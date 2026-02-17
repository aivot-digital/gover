package de.aivot.GoverBackend.permissions.services;

import de.aivot.GoverBackend.department.entities.DepartmentEntity;
import de.aivot.GoverBackend.department.repositories.DepartmentRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.permissions.repositories.VUserDepartmentPermissionRepository;
import de.aivot.GoverBackend.permissions.repositories.VUserSystemPermissionRepository;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {
    private final VUserDepartmentPermissionRepository vUserDepartmentPermissionRepository;
    private final VUserSystemPermissionRepository vUserSystemPermissionRepository;
    private final DepartmentRepository departmentRepository;

    public PermissionService(VUserDepartmentPermissionRepository vUserDepartmentPermissionRepository,
                             VUserSystemPermissionRepository vUserSystemPermissionRepository,
                             DepartmentRepository departmentRepository) {
        this.vUserDepartmentPermissionRepository = vUserDepartmentPermissionRepository;
        this.vUserSystemPermissionRepository = vUserSystemPermissionRepository;
        this.departmentRepository = departmentRepository;
    }

    public boolean hasSystemPermission(@Nullable String userId,
                                       @Nonnull String permission) {
        if (userId == null) {
            return false;
        }
        return vUserSystemPermissionRepository
                .hasPermission(userId, permission);
    }

    public boolean hasSystemPermission(@Nullable Jwt jwt,
                                       @Nonnull String permission) {
        return hasSystemPermission(UserService.getIdFromJWT(jwt), permission);
    }

    public void testSystemPermission(@Nullable String userId,
                                     @Nonnull String permission) throws ResponseException {
        if (userId == null || !hasSystemPermission(userId, permission)) {
            throw ResponseException.forbidden(
                    "Sie benötigen die Berechtigung %s im System.",
                    StringUtils.quote(permission)
            );
        }
    }

    public void testSystemPermission(@Nullable Jwt jwt,
                                     @Nonnull String permission) throws ResponseException {
        testSystemPermission(UserService.getIdFromJWT(jwt), permission);
    }

    /**
     * @deprecated use testSystemPermission instead
     */
    @Deprecated
    public void hasSystemPermissionThrows(@Nullable UserEntity user,
                                          @Nonnull String permission) throws ResponseException {
        testSystemPermission(user == null ? "" : user.getId(), permission);
    }

    public boolean hasDepartmentPermission(@Nonnull String userId,
                                           @Nonnull Integer departmentId,
                                           @Nonnull String permission) {
        return vUserDepartmentPermissionRepository
                .hasPermission(userId, departmentId, permission);
    }

    public void testDepartmentPermission(@Nonnull String userId,
                                         @Nonnull Integer departmentId,
                                         @Nonnull String permission) throws ResponseException {
        if (!hasDepartmentPermission(userId, departmentId, permission)) {
            var departmentName = departmentRepository
                    .findById(departmentId)
                    .map(DepartmentEntity::getName)
                    .map(StringUtils::quote)
                    .orElse("mit der ID " + departmentId);

            throw ResponseException.forbidden(
                    "Sie benötigen die Berechtigung %s für die Organisationseinheit %d.",
                    StringUtils.quote(permission),
                    departmentName
            );
        }
    }

    public boolean hasInAnyDepartmentPermission(@Nonnull String userId,
                                                @Nonnull String permission) {
        return vUserDepartmentPermissionRepository
                .hasPermissionInAnyDepartment(userId, permission);
    }

    public void testInAnyDepartmentPermission(@Nonnull String userId,
                                               @Nonnull String permission) throws ResponseException {
        if (!hasInAnyDepartmentPermission(userId, permission)) {
            throw ResponseException.forbidden(
                    "Sie benötigen die Berechtigung %s in mindestens einer Organisationseinheit.",
                    StringUtils.quote(permission)
            );
        }
    }

    /**
     * @deprecated use testDepartmentPermission instead
     */
    @Deprecated
    public void hasDepartmentPermissionThrows(@Nonnull String userId,
                                              @Nonnull Integer departmentId,
                                              @Nonnull String permission) throws ResponseException {
        testDepartmentPermission(userId, departmentId, permission);
    }
}
