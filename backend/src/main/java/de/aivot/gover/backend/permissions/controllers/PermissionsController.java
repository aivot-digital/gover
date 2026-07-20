package de.aivot.gover.backend.permissions.controllers;

import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.openApi.OpenApiConfiguration;
import de.aivot.gover.backend.openApi.OpenApiConstants;
import de.aivot.gover.backend.permissions.entities.VUserDepartmentPermissionEntity;
import de.aivot.gover.backend.permissions.entities.VUserSystemPermissionEntity;
import de.aivot.gover.backend.permissions.entities.VUserTeamPermissionEntity;
import de.aivot.gover.backend.permissions.models.PermissionProvider;
import de.aivot.gover.backend.permissions.repositories.VUserDepartmentPermissionRepository;
import de.aivot.gover.backend.permissions.repositories.VUserSystemPermissionRepository;
import de.aivot.gover.backend.permissions.repositories.VUserTeamPermissionRepository;
import de.aivot.gover.backend.permissions.permissions.PermissionSetPermissionProvider;
import de.aivot.gover.backend.permissions.services.PermissionService;
import de.aivot.gover.backend.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
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
    private final PermissionService permissionService;
    private final UserService userService;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public PermissionsController(List<PermissionProvider> permissionProviders,
                                 VUserDepartmentPermissionRepository vUserDepartmentPermissionRepository,
                                 VUserTeamPermissionRepository vUserTeamPermissionRepository,
                                 VUserSystemPermissionRepository vUserSystemPermissionRepository,
                                 PermissionService permissionService,
                                 UserService userService,
                                 JdbcTemplate jdbcTemplate) {
        this.permissionProviders = permissionProviders;
        this.vUserDepartmentPermissionRepository = vUserDepartmentPermissionRepository;
        this.vUserTeamPermissionRepository = vUserTeamPermissionRepository;
        this.vUserSystemPermissionRepository = vUserSystemPermissionRepository;
        this.permissionService = permissionService;
        this.userService = userService;
        this.jdbcTemplate = jdbcTemplate;
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
                .findAllByUserId(userId)
                .stream()
                .filter(PermissionsController::hasConcreteTeamPermission)
                .toList();

        var departmentPermissions = vUserDepartmentPermissionRepository
                .findAllByUserId(userId)
                .stream()
                .filter(PermissionsController::hasConcreteDepartmentPermission)
                .toList();

        var domainPermissions = listDomainPermissions(userId);

        var processPermissions = listProcessPermissions(userId);

        var processInstancePermissions = listProcessInstancePermissions(userId);

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

    private static boolean hasConcreteTeamPermission(@Nullable VUserTeamPermissionEntity permission) {
        return permission != null && permission.getTeamId() != null && hasPermissionEntries(permission.getPermissions());
    }

    private static boolean hasConcreteDepartmentPermission(@Nullable VUserDepartmentPermissionEntity permission) {
        return permission != null && permission.getDepartmentId() != null && hasPermissionEntries(permission.getPermissions());
    }

    private static boolean hasPermissionEntries(@Nullable List<String> permissions) {
        return permissions != null && !permissions.isEmpty();
    }

    private List<DomainPermission> listDomainPermissions(@Nonnull String userId) {
        // The view may contain structural rows without effective grants, so the API exposes only non-empty permission arrays.
        return jdbcTemplate.query(
                """
                        SELECT user_id, department_id, team_id, permissions
                        FROM v_user_domain_permissions
                        WHERE user_id = ?
                          AND (department_id IS NOT NULL OR team_id IS NOT NULL)
                          AND array_length(permissions, 1) > 0
                        """,
                (rs, rowNum) -> {
                    var departmentId = (Integer) rs.getObject("department_id");
                    var teamId = (Integer) rs.getObject("team_id");
                    return new DomainPermission(
                            domainPermissionId(departmentId, teamId),
                            rs.getString("user_id"),
                            departmentId,
                            teamId,
                            toStringList(rs.getArray("permissions"))
                    );
                },
                userId
        );
    }

    private List<ProcessPermission> listProcessPermissions(@Nonnull String userId) {
        // Process permissions keep their source context so the frontend can distinguish direct and inherited grants.
        return jdbcTemplate.query(
                """
                        SELECT user_id, via_source_team_id, via_source_department_id, target_process_id, permissions
                        FROM v_user_process_access_permissions
                        WHERE user_id = ?
                          AND target_process_id IS NOT NULL
                          AND array_length(permissions, 1) > 0
                        """,
                (rs, rowNum) -> {
                    var processId = (Integer) rs.getObject("target_process_id");
                    var viaSourceTeamId = (Integer) rs.getObject("via_source_team_id");
                    var viaSourceDepartmentId = (Integer) rs.getObject("via_source_department_id");
                    return new ProcessPermission(
                            "process:%d:team:%s:department:%s".formatted(
                                    processId,
                                    viaSourceTeamId == null ? "" : viaSourceTeamId,
                                    viaSourceDepartmentId == null ? "" : viaSourceDepartmentId
                            ),
                            rs.getString("user_id"),
                            viaSourceTeamId,
                            viaSourceDepartmentId,
                            processId,
                            toStringList(rs.getArray("permissions"))
                    );
                },
                userId
        );
    }

    private List<ProcessInstancePermission> listProcessInstancePermissions(@Nonnull String userId) {
        // Process-instance access can change when instances are created or removed, hence it is part of the runtime permission set.
        return jdbcTemplate.query(
                """
                        SELECT user_id, via_source_team_id, via_source_department_id, target_process_instance_id, permissions
                        FROM v_user_process_instance_access_permissions
                        WHERE user_id = ?
                          AND target_process_instance_id IS NOT NULL
                          AND array_length(permissions, 1) > 0
                        """,
                (rs, rowNum) -> {
                    var processInstanceId = ((Number) rs.getObject("target_process_instance_id")).longValue();
                    var viaSourceTeamId = (Integer) rs.getObject("via_source_team_id");
                    var viaSourceDepartmentId = (Integer) rs.getObject("via_source_department_id");
                    return new ProcessInstancePermission(
                            "process-instance:%d:team:%s:department:%s".formatted(
                                    processInstanceId,
                                    viaSourceTeamId == null ? "" : viaSourceTeamId,
                                    viaSourceDepartmentId == null ? "" : viaSourceDepartmentId
                            ),
                            rs.getString("user_id"),
                            viaSourceTeamId,
                            viaSourceDepartmentId,
                            processInstanceId,
                            toStringList(rs.getArray("permissions"))
                    );
                },
                userId
        );
    }

    private static String domainPermissionId(Integer departmentId, Integer teamId) {
        if (departmentId != null) {
            return "department:" + departmentId;
        }
        return "team:" + teamId;
    }

    private static List<String> toStringList(Array array) throws SQLException {
        if (array == null) {
            return List.of();
        }

        var value = array.getArray();
        if (value instanceof String[] stringArray) {
            return Arrays.asList(stringArray);
        }
        if (value instanceof Object[] objectArray) {
            return Arrays
                    .stream(objectArray)
                    .map(String::valueOf)
                    .toList();
        }

        return List.of();
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
    ) { }

    public record ProcessPermission(
            String id,
            String userId,
            Integer viaSourceTeamId,
            Integer viaSourceDepartmentId,
            Integer processId,
            List<String> permissions
    ) { }

    public record ProcessInstancePermission(
            String id,
            String userId,
            Integer viaSourceTeamId,
            Integer viaSourceDepartmentId,
            Long processInstanceId,
            List<String> permissions
    ) { }
}
