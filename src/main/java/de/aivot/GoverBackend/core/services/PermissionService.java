package de.aivot.GoverBackend.core.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.userRoles.repositories.VUserDomainPermissionRepository;
import de.aivot.GoverBackend.userRoles.repositories.VUserSystemPermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {
    private final VUserSystemPermissionRepository vUserSystemPermissionEntityRepository;
    private final VUserDomainPermissionRepository vUserDomainPermissionRepository;

    @Autowired
    public PermissionService(VUserSystemPermissionRepository vUserSystemPermissionEntityRepository,
                             VUserDomainPermissionRepository vUserDomainPermissionRepository) {
        this.vUserSystemPermissionEntityRepository = vUserSystemPermissionEntityRepository;
        this.vUserDomainPermissionRepository = vUserDomainPermissionRepository;
    }

    public boolean hasSystemPermission(String userId, String permission) {
        return vUserSystemPermissionEntityRepository
                .existsByUserIdIsAndPermission(userId, permission);
    }

    public void hasSystemPermissionThrows(String userId, String permission) throws ResponseException  {
        if (hasSystemPermission(userId, permission)) {
            return;
        }
        throw ResponseException.forbidden("Sie benötigen die Berechtigung „" + permission + "“ im System.");
    }

    public boolean hasDepartmentPermission(String userId, Integer departmentId, String permission) {
        return vUserDomainPermissionRepository
                .existsByUserIdIsAndDepartmentIdAndPermission(userId, departmentId, permission);
    }

    public void hasDepartmentPermissionThrows(String userId, Integer departmentId, String permission) throws ResponseException {
        if (hasDepartmentPermission(userId, departmentId, permission)) {
            return;
        }
        throw ResponseException.forbidden("Sie benötigen die Berechtigung „" + permission + "“ für diese Organisationseinheit.");
    }
}
