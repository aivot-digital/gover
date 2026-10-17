package de.aivot.prosuna.backend.permissions.controllers;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.openApi.OpenApiConstants;
import de.aivot.prosuna.backend.permissions.entities.VUserDepartmentPermissionEntity;
import de.aivot.prosuna.backend.permissions.entities.VUserSystemPermissionEntity;
import de.aivot.prosuna.backend.permissions.entities.VUserTeamPermissionEntity;
import de.aivot.prosuna.backend.permissions.models.PermissionProvider;
import de.aivot.prosuna.backend.permissions.projections.DomainPermissionProjection;
import de.aivot.prosuna.backend.permissions.projections.ProcessInstancePermissionProjection;
import de.aivot.prosuna.backend.permissions.projections.ProcessPermissionProjection;
import de.aivot.prosuna.backend.permissions.repositories.VUserDepartmentPermissionRepository;
import de.aivot.prosuna.backend.permissions.repositories.VUserDomainPermissionRepository;
import de.aivot.prosuna.backend.permissions.repositories.VUserSystemPermissionRepository;
import de.aivot.prosuna.backend.permissions.repositories.VUserTeamPermissionRepository;
import de.aivot.prosuna.backend.permissions.permissions.PermissionSetPermissionProvider;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.repositories.VUserProcessAccessPermissionsRepository;
import de.aivot.prosuna.backend.process.repositories.VUserProcessInstanceAccessPermissionsRepository;
import de.aivot.prosuna.backend.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

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
    private final VUserSystemPermissionRepository vUserSystemPermissionRepository;
    private final VUserDomainPermissionRepository vUserDomainPermissionRepository;
    private final VUserProcessAccessPermissionsRepository vUserProcessAccessPermissionsRepository;
    private final VUserProcessInstanceAccessPermissionsRepository vUserProcessInstanceAccessPermissionsRepository;
    private final PermissionService permissionService;
    private final UserService userService;

    @Autowired
    public PermissionsController(List<PermissionProvider> permissionProviders,
                                 VUserDepartmentPermissionRepository vUserDepartmentPermissionRepository,
                                 VUserTeamPermissionRepository vUserTeamPermissionRepository,
                                 VUserSystemPermissionRepository vUserSystemPermissionRepository,
                                 VUserDomainPermissionRepository vUserDomainPermissionRepository,
                                 VUserProcessAccessPermissionsRepository vUserProcessAccessPermissionsRepository,
                                 VUserProcessInstanceAccessPermissionsRepository vUserProcessInstanceAccessPermissionsRepository,
                                 PermissionService permissionService,
                                 UserService userService) {
        this.permissionProviders = permissionProviders;
        this.vUserDepartmentPermissionRepository = vUserDepartmentPermissionRepository;
        this.vUserTeamPermissionRepository = vUserTeamPermissionRepository;
        this.vUserSystemPermissionRepository = vUserSystemPermissionRepository;
        this.vUserDomainPermissionRepository = vUserDomainPermissionRepository;
        this.vUserProcessAccessPermissionsRepository = vUserProcessAccessPermissionsRepository;
        this.vUserProcessInstanceAccessPermissionsRepository = vUserProcessInstanceAccessPermissionsRepository;
        this.permissionService = permissionService;
        this.userService = userService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Permissions",
            description = "Retrieve a list of all available permissions in the system."
    )
    public List<PermissionProvider> list() throws ResponseException {
        return permissionProviders;
    }

    @GetMapping("self/")
    @Operation(
            summary = "List Permissions for Current User",
            description = "Retrieve a list of all granted permissions of the currently authenticated user."
    )
    public PermissionSet listForSelf(
            @Nullable @AuthenticationPrincipal Jwt jwt
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return buildPermissionSet(user.getId());
    }

    @GetMapping("/users/{userId}/")
    @Operation(
            summary = "List Permissions for User",
            description = "Retrieve a list of all granted permissions of a user."
    )
    public PermissionSet listForUser(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String userId
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!Objects.equals(user.getId(), userId)) {
            permissionService.requireSystemPermission(user.getId(), PermissionSetPermissionProvider.PERMISSION_SET_READ);
        }

        return buildPermissionSet(userId);
    }

    private PermissionSet buildPermissionSet(@Nonnull String userId) {
        var teamPermissions = vUserTeamPermissionRepository
                .findAllByUserId(userId);

        var departmentPermissions = vUserDepartmentPermissionRepository
                .findAllByUserId(userId);

        var domainPermissions = vUserDomainPermissionRepository
                .findAllConcreteByUserId(userId)
                .stream()
                .map(DomainPermission::from)
                .toList();

        var processPermissions = vUserProcessAccessPermissionsRepository
                .findAllConcreteByUserId(userId)
                .stream()
                .map(ProcessPermission::from)
                .toList();

        var processInstancePermissions = vUserProcessInstanceAccessPermissionsRepository
                .findAllConcreteByUserId(userId)
                .stream()
                .map(ProcessInstancePermission::from)
                .toList();

        var systemPermissions = vUserSystemPermissionRepository
                .findAllByUserId(userId);

        return new PermissionSet(
                departmentPermissions,
                teamPermissions,
                domainPermissions,
                processPermissions,
                processInstancePermissions,
                systemPermissions
        );
    }

    private static String domainPermissionId(Integer departmentId, Integer teamId) {
        if (departmentId != null) {
            return "department:" + departmentId;
        }
        return "team:" + teamId;
    }

    public record PermissionSet(
            List<VUserDepartmentPermissionEntity> departmentPermissions,
            List<VUserTeamPermissionEntity> teamPermissions,
            List<DomainPermission> domainPermissions,
            List<ProcessPermission> processPermissions,
            List<ProcessInstancePermission> processInstancePermissions,
            List<VUserSystemPermissionEntity> systemPermissions
    ) { }

    public record DomainPermission(
            String id,
            String userId,
            Integer departmentId,
            Integer teamId,
            List<String> permissions
    ) {
        private static DomainPermission from(DomainPermissionProjection projection) {
            return new DomainPermission(
                    domainPermissionId(projection.getDepartmentId(), projection.getTeamId()),
                    projection.getUserId(),
                    projection.getDepartmentId(),
                    projection.getTeamId(),
                    projection.getPermissions()
            );
        }
    }

    public record ProcessPermission(
            String id,
            String userId,
            Integer viaSourceTeamId,
            Integer viaSourceDepartmentId,
            Integer processId,
            List<String> permissions
    ) {
        private static ProcessPermission from(ProcessPermissionProjection projection) {
            return new ProcessPermission(
                    "process:%d:team:%s:department:%s".formatted(
                            projection.getProcessId(),
                            projection.getViaSourceTeamId() == null ? "" : projection.getViaSourceTeamId(),
                            projection.getViaSourceDepartmentId() == null ? "" : projection.getViaSourceDepartmentId()
                    ),
                    projection.getUserId(),
                    projection.getViaSourceTeamId(),
                    projection.getViaSourceDepartmentId(),
                    projection.getProcessId(),
                    projection.getPermissions()
            );
        }
    }

    public record ProcessInstancePermission(
            String id,
            String userId,
            Integer viaSourceTeamId,
            Integer viaSourceDepartmentId,
            Long processInstanceId,
            List<String> permissions
    ) {
        private static ProcessInstancePermission from(ProcessInstancePermissionProjection projection) {
            return new ProcessInstancePermission(
                    "process-instance:%d:team:%s:department:%s".formatted(
                            projection.getProcessInstanceId(),
                            projection.getViaSourceTeamId() == null ? "" : projection.getViaSourceTeamId(),
                            projection.getViaSourceDepartmentId() == null ? "" : projection.getViaSourceDepartmentId()
                    ),
                    projection.getUserId(),
                    projection.getViaSourceTeamId(),
                    projection.getViaSourceDepartmentId(),
                    projection.getProcessInstanceId(),
                    projection.getPermissions()
            );
        }
    }
}
