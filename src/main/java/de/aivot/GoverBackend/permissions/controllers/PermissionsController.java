package de.aivot.GoverBackend.permissions.controllers;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.permissions.entities.VUserDepartmentPermissionEntity;
import de.aivot.GoverBackend.permissions.models.PermissionProvider;
import de.aivot.GoverBackend.permissions.repositories.VUserDepartmentPermissionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// TODO: Define permissions for accessing permissions!

@RestController
@RequestMapping("/api/permissions/")
@Tag(
        name = OpenApiConstants.Tags.PermissionsName,
        description = OpenApiConstants.Tags.PermissionsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class PermissionsController {
    private final List<PermissionProvider> permissionProviders;
    private final VUserDepartmentPermissionRepository vUserDepartmentPermissionRepository;

    @Autowired
    public PermissionsController(List<PermissionProvider> permissionProviders,
                                 VUserDepartmentPermissionRepository vUserDepartmentPermissionRepository) {
        this.permissionProviders = permissionProviders;
        this.vUserDepartmentPermissionRepository = vUserDepartmentPermissionRepository;
    }

    @GetMapping("")
    @Operation(
            summary = "List Permissions",
            description = "Retrieve a list of all available permissions in the system."
    )
    public List<PermissionProvider> list() throws ResponseException {
        return permissionProviders;
    }

    @GetMapping("/user/{userId}/")
    @Operation(
            summary = "List Permissions for User",
            description = "Retrieve a list of all granted permissions of a user."
    )
    public List<VUserDepartmentPermissionEntity> listForUser(
            @Nonnull @PathVariable String userId
    ) throws ResponseException {
        return vUserDepartmentPermissionRepository
                .findAllByUserId(userId);
    }
}
