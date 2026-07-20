package de.aivot.gover.backend.userRoles.services;

import de.aivot.gover.backend.department.permissions.DepartmentPermissionProvider;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.permissions.permissions.PermissionSetPermissionProvider;
import de.aivot.gover.backend.userRoles.entities.UserRoleEntity;
import de.aivot.gover.backend.userRoles.repositories.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class UserRoleServiceTest {
    @Test
    void createShouldRejectPermissionsThatAreNotDomainAssignable() {
        var repository = mock(UserRoleRepository.class);
        var service = new UserRoleService(repository, List.of(
                new DepartmentPermissionProvider(),
                new PermissionSetPermissionProvider()
        ));
        var entity = new UserRoleEntity()
                .setPermissions(List.of(PermissionSetPermissionProvider.PERMISSION_SET_READ));

        var exception = assertThrows(ResponseException.class, () -> service.create(entity));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(repository, never()).save(entity);
    }

    @Test
    void createShouldRejectPermissionsExcludedFromDomainRoleAssignment() {
        var repository = mock(UserRoleRepository.class);
        var service = new UserRoleService(repository, List.of(
                new DepartmentPermissionProvider(),
                new PermissionSetPermissionProvider()
        ));
        var entity = new UserRoleEntity()
                .setPermissions(List.of(DepartmentPermissionProvider.DEPARTMENT_CREATE));

        var exception = assertThrows(ResponseException.class, () -> service.create(entity));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(repository, never()).save(entity);
    }

    @Test
    void updateShouldAllowExistingPermissionsThatAreNoLongerDomainAssignable() throws ResponseException {
        var repository = mock(UserRoleRepository.class);
        var service = new UserRoleService(repository, List.of(
                new DepartmentPermissionProvider(),
                new PermissionSetPermissionProvider()
        ));
        var existingEntity = new UserRoleEntity()
                .setPermissions(List.of(
                        DepartmentPermissionProvider.DEPARTMENT_READ,
                        PermissionSetPermissionProvider.PERMISSION_SET_READ
                ));
        var updatedEntity = new UserRoleEntity()
                .setName("Updated")
                .setDescription("Updated description")
                .setPermissions(List.of(
                        DepartmentPermissionProvider.DEPARTMENT_READ,
                        PermissionSetPermissionProvider.PERMISSION_SET_READ
                ));

        service.performUpdate(1, updatedEntity, existingEntity);

        verify(repository).save(existingEntity);
    }

    @Test
    void updateShouldAllowExistingPermissionsExcludedFromDomainRoleAssignment() throws ResponseException {
        var repository = mock(UserRoleRepository.class);
        var service = new UserRoleService(repository, List.of(
                new DepartmentPermissionProvider(),
                new PermissionSetPermissionProvider()
        ));
        var existingEntity = new UserRoleEntity()
                .setPermissions(List.of(
                        DepartmentPermissionProvider.DEPARTMENT_READ,
                        DepartmentPermissionProvider.DEPARTMENT_CREATE
                ));
        var updatedEntity = new UserRoleEntity()
                .setName("Updated")
                .setDescription("Updated description")
                .setPermissions(List.of(
                        DepartmentPermissionProvider.DEPARTMENT_READ,
                        DepartmentPermissionProvider.DEPARTMENT_CREATE
                ));

        service.performUpdate(1, updatedEntity, existingEntity);

        verify(repository).save(existingEntity);
    }

    @Test
    void updateShouldRejectNewPermissionsThatAreNotDomainAssignable() {
        var repository = mock(UserRoleRepository.class);
        var service = new UserRoleService(repository, List.of(
                new DepartmentPermissionProvider(),
                new PermissionSetPermissionProvider()
        ));
        var existingEntity = new UserRoleEntity()
                .setPermissions(List.of(DepartmentPermissionProvider.DEPARTMENT_READ));
        var updatedEntity = new UserRoleEntity()
                .setPermissions(List.of(
                        DepartmentPermissionProvider.DEPARTMENT_READ,
                        PermissionSetPermissionProvider.PERMISSION_SET_READ
                ));

        var exception = assertThrows(ResponseException.class, () -> service.performUpdate(1, updatedEntity, existingEntity));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(repository, never()).save(existingEntity);
    }

    @Test
    void updateShouldRejectNewPermissionsExcludedFromDomainRoleAssignment() {
        var repository = mock(UserRoleRepository.class);
        var service = new UserRoleService(repository, List.of(
                new DepartmentPermissionProvider(),
                new PermissionSetPermissionProvider()
        ));
        var existingEntity = new UserRoleEntity()
                .setPermissions(List.of(DepartmentPermissionProvider.DEPARTMENT_READ));
        var updatedEntity = new UserRoleEntity()
                .setPermissions(List.of(
                        DepartmentPermissionProvider.DEPARTMENT_READ,
                        DepartmentPermissionProvider.DEPARTMENT_CREATE
                ));

        var exception = assertThrows(ResponseException.class, () -> service.performUpdate(1, updatedEntity, existingEntity));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(repository, never()).save(existingEntity);
    }

    @Test
    void updateShouldAllowRemovingPermissionsThatAreNoLongerDomainAssignable() throws ResponseException {
        var repository = mock(UserRoleRepository.class);
        var service = new UserRoleService(repository, List.of(
                new DepartmentPermissionProvider(),
                new PermissionSetPermissionProvider()
        ));
        var existingEntity = new UserRoleEntity()
                .setPermissions(List.of(
                        DepartmentPermissionProvider.DEPARTMENT_READ,
                        PermissionSetPermissionProvider.PERMISSION_SET_READ
                ));
        var updatedEntity = new UserRoleEntity()
                .setName("Updated")
                .setDescription("Updated description")
                .setPermissions(List.of(DepartmentPermissionProvider.DEPARTMENT_READ));

        service.performUpdate(1, updatedEntity, existingEntity);

        verify(repository).save(existingEntity);
    }
}
