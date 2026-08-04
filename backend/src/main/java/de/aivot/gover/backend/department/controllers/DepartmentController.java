package de.aivot.gover.backend.department.controllers;

import de.aivot.gover.backend.audit.enums.AuditAction;
import de.aivot.gover.backend.audit.models.AuditLogPayload;
import de.aivot.gover.backend.audit.services.AuditService;
import de.aivot.gover.backend.audit.services.ScopedAuditService;
import de.aivot.gover.backend.department.entities.DepartmentEntity;
import de.aivot.gover.backend.department.filters.DepartmentFilter;
import de.aivot.gover.backend.department.permissions.DepartmentPermissionProvider;
import de.aivot.gover.backend.department.repositories.DepartmentRepository;
import de.aivot.gover.backend.department.services.DepartmentService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.openApi.OpenApiConfiguration;
import de.aivot.gover.backend.openApi.OpenApiConstants;
import de.aivot.gover.backend.permissions.services.PermissionService;
import de.aivot.gover.backend.user.services.UserService;
import de.aivot.gover.backend.utils.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/departments/")
@Tag(
        name = OpenApiConstants.Tags.DepartmentsName,
        description = OpenApiConstants.Tags.DepartmentsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class DepartmentController {
    private static final String MODULE_NAME = "Organisationseinheiten";

    private final ScopedAuditService auditService;

    private final DepartmentService departmentService;
    private final DepartmentRepository departmentRepository;
    private final PermissionService permissionService;
    private final UserService userService;

    @Autowired
    public DepartmentController(AuditService auditService,
                                DepartmentService departmentService,
                                DepartmentRepository departmentRepository,
                                PermissionService permissionService,
                                UserService userService) {
        this.auditService = auditService.createScopedAuditService(DepartmentController.class, MODULE_NAME);

        this.departmentService = departmentService;
        this.departmentRepository = departmentRepository;
        this.permissionService = permissionService;
        this.userService = userService;
    }

    @GetMapping("")
    @Operation(
            summary = "List departments",
            description = "List departments with pagination and filtering."
    )
    public Page<DepartmentEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid DepartmentFilter filter
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!permissionService.hasSystemPermission(user.getId(), DepartmentPermissionProvider.DEPARTMENT_READ)) {
            if (filter.getId() != null) {
                permissionService.requireDepartmentPermission(
                        user.getId(),
                        filter.getId(),
                        DepartmentPermissionProvider.DEPARTMENT_READ
                );
            } else {
                var accessibleDepartmentIds = permissionService
                        .getDepartmentsWithPermission(user.getId(), DepartmentPermissionProvider.DEPARTMENT_READ);

                if (filter.getIds() != null) {
                    // Preserve explicit client filtering, but intersect it with the departments the user may read.
                    accessibleDepartmentIds = filter.getIds()
                            .stream()
                            .filter(accessibleDepartmentIds::contains)
                            .toList();
                }

                if (accessibleDepartmentIds.isEmpty()) {
                    return Page.empty(pageable);
                }

                filter.setIds(accessibleDepartmentIds);
            }
        }

        return departmentService
                .list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create department",
            description = "Create a new department. Requires the system-level permission `" +
                    DepartmentPermissionProvider.DEPARTMENT_CREATE + "`."
    )
    public DepartmentEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid DepartmentEntity newDepartment
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Creating departments changes the organization structure and is always controlled system-wide.
        permissionService.requireSystemPermission(
                execUser.getId(),
                DepartmentPermissionProvider.DEPARTMENT_CREATE
        );

        var createdDepartment = departmentService
                .create(newDepartment);

        auditService.create()
                .withUser(execUser)
                .withAuditAction(AuditAction.Create, DepartmentEntity.class,
                        createdDepartment.getId(),
                        "id",
                        Map.of(
                                "name", createdDepartment.getName()
                        ))
                .withMessage(
                        "Die Organisationseinheit %s mit der ID %s wurde von der Mitarbeiter:in %s erstellt.",
                        StringUtils.quote(createdDepartment.getName()),
                        StringUtils.quote(String.valueOf(createdDepartment.getId())),
                        StringUtils.quote(execUser.getFullName())
                )
                .log();

        return createdDepartment;
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve department",
            description = "Retrieve a department by its id."
    )
    public DepartmentEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService.requireDepartmentPermission(
                user.getId(),
                id,
                DepartmentPermissionProvider.DEPARTMENT_READ
        );

        return departmentService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    @Operation(
            summary = "Update department",
            description = "Update a department. Requires the permission `" +
                    DepartmentPermissionProvider.DEPARTMENT_UPDATE +
                    "` for the affected organisation unit or at system level."
    )
    public DepartmentEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @RequestBody @Valid DepartmentEntity updatedDepartment
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService.requireDepartmentPermission(
                user.getId(),
                id,
                DepartmentPermissionProvider.DEPARTMENT_UPDATE
        );

        var existingDepartment = departmentService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        var existingMap = AuditLogPayload.toMap(existingDepartment);

        var savedDepartment = departmentService
                .update(id, updatedDepartment);

        var savedMap = AuditLogPayload.toMap(savedDepartment);

        auditService.create()
                .withUser(user)
                .withAuditAction(AuditAction.Update, DepartmentEntity.class,
                        savedDepartment.getId(),
                        "id",
                        Map.of(
                                "name", savedDepartment.getName()
                        ))
                .withDiff(existingMap, savedMap)
                .withMessage(
                        "Die Organisationseinheit %s mit der ID %s wurde von der Mitarbeiter:in %s aktualisiert.",
                        StringUtils.quote(savedDepartment.getName()),
                        StringUtils.quote(String.valueOf(savedDepartment.getId())),
                        StringUtils.quote(user.getFullName())
                )
                .log();

        return savedDepartment;
    }

    @DeleteMapping("{id}/")
    @Operation(
            summary = "Delete department",
            description = "Delete a department. Requires the permission `" +
                    DepartmentPermissionProvider.DEPARTMENT_DELETE +
                    "` for the affected organisation unit or at system level."
    )
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService.requireDepartmentPermission(
                user.getId(),
                id,
                DepartmentPermissionProvider.DEPARTMENT_DELETE
        );

        var dep = departmentRepository
                .findById(id)
                .orElseThrow(ResponseException::notFound);

        departmentService.delete(id);

        auditService.create()
                .withUser(user)
                .withAuditAction(AuditAction.Delete, DepartmentEntity.class,
                        dep.getId(),
                        "id",
                        Map.of(
                                "name", dep.getName()
                        ))
                .withMessage(
                        "Die Organisationseinheit %s mit der ID %s wurde von der Mitarbeiter:in %s gelöscht.",
                        StringUtils.quote(dep.getName()),
                        StringUtils.quote(String.valueOf(dep.getId())),
                        StringUtils.quote(user.getFullName())
                )
                .log();

    }
}
