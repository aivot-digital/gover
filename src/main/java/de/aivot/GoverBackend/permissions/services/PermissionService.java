package de.aivot.GoverBackend.permissions.services;

import de.aivot.GoverBackend.department.entities.DepartmentEntity;
import de.aivot.GoverBackend.department.repositories.DepartmentRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.permissions.repositories.VUserDepartmentPermissionRepository;
import de.aivot.GoverBackend.permissions.repositories.VUserSystemPermissionRepository;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionService {
    private final VUserDepartmentPermissionRepository vUserDepartmentPermissionRepository;
    private final VUserSystemPermissionRepository vUserSystemPermissionRepository;
    private final DepartmentRepository departmentRepository;

    public PermissionService(VUserDepartmentPermissionRepository vUserDepartmentPermissionRepository, VUserSystemPermissionRepository vUserSystemPermissionRepository, DepartmentRepository departmentRepository) {
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

    public boolean hasDomainPermission(@Nonnull String userId,
                                       @Nonnull Integer departmentId,
                                       @Nonnull String permission) {
        return vUserDepartmentPermissionRepository
                .hasPermission(userId, departmentId, permission);
    }

    public void testDomainPermission(@Nonnull String userId,
                                     @Nonnull Integer departmentId,
                                     @Nonnull String permission) throws ResponseException {
        if (!hasDomainPermission(userId, departmentId, permission)) {
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

    public List<Integer> getDepartmentsWithPermission(@Nonnull String userId,
                                                      @Nonnull String permission) {
        return vUserDepartmentPermissionRepository
                .getDepartmentsWithPermission(userId, permission);
    }
}
