package de.aivot.GoverBackend.permissions.controllers;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.permissions.entities.VUserDepartmentPermissionEntity;
import de.aivot.GoverBackend.permissions.entities.VUserDomainPermissionEntity;
import de.aivot.GoverBackend.permissions.entities.VUserSystemPermissionEntity;
import de.aivot.GoverBackend.permissions.entities.VUserTeamPermissionEntity;
import de.aivot.GoverBackend.permissions.models.PermissionProvider;
import de.aivot.GoverBackend.permissions.repositories.VUserDepartmentPermissionRepository;
import de.aivot.GoverBackend.permissions.repositories.VUserDomainPermissionRepository;
import de.aivot.GoverBackend.permissions.repositories.VUserSystemPermissionRepository;
import de.aivot.GoverBackend.permissions.repositories.VUserTeamPermissionRepository;
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
    private final VUserTeamPermissionRepository vUserTeamPermissionRepository;
    private final VUserDomainPermissionRepository vUserDomainPermissionRepository;
    private final VUserSystemPermissionRepository vUserSystemPermissionRepository;

    @Autowired
    public PermissionsController(List<PermissionProvider> permissionProviders,
                                 VUserDepartmentPermissionRepository vUserDepartmentPermissionRepository,
                                 VUserTeamPermissionRepository vUserTeamPermissionRepository,
                                 VUserDomainPermissionRepository vUserDomainPermissionRepository, VUserSystemPermissionRepository vUserSystemPermissionRepository) {
        this.permissionProviders = permissionProviders;
        this.vUserDepartmentPermissionRepository = vUserDepartmentPermissionRepository;
        this.vUserTeamPermissionRepository = vUserTeamPermissionRepository;
        this.vUserDomainPermissionRepository = vUserDomainPermissionRepository;
        this.vUserSystemPermissionRepository = vUserSystemPermissionRepository;
    }

    @GetMapping("")
    @Operation(
            summary = "List Permissions",
            description = "Retrieve a list of all available permissions in the system."
    )
    public List<PermissionProvider> list() throws ResponseException {
        return permissionProviders;
    }

    @GetMapping("/users/{userId}/")
    @Operation(
            summary = "List Permissions for User",
            description = "Retrieve a list of all granted permissions of a user."
    )
    public PermissionSet listForUser(
            @Nonnull @PathVariable String userId
    ) throws ResponseException {
        // TODO: Define access control!

        var teamPermissions = vUserTeamPermissionRepository
                .findAllByUserId(userId);

        var departmentPermissions = vUserDepartmentPermissionRepository
                .findAllByUserId(userId);

        var domainPermissions = vUserDomainPermissionRepository
                .findAllByUserId(userId);

        var systemPermissions = vUserSystemPermissionRepository
                .findAllByUserId(userId);

        return new PermissionSet(
                departmentPermissions,
                teamPermissions,
                domainPermissions,
                systemPermissions
        );
    }

    public record PermissionSet(
            List<VUserDepartmentPermissionEntity> departmentPermissions,
            List<VUserTeamPermissionEntity> teamPermissions,
            List<VUserDomainPermissionEntity> domainPermissions,
            List<VUserSystemPermissionEntity> systemPermissions
    ) { }
}
