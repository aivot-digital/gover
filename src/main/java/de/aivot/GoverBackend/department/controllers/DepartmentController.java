package de.aivot.GoverBackend.department.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.models.AuditLogPayload;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.department.entities.DepartmentEntity;
import de.aivot.GoverBackend.department.filters.DepartmentFilter;
import de.aivot.GoverBackend.department.filters.VDepartmentMembershipWithPermissionsFilter;
import de.aivot.GoverBackend.department.repositories.DepartmentRepository;
import de.aivot.GoverBackend.department.services.DepartmentService;
import de.aivot.GoverBackend.department.services.VDepartmentMembershipWithPermissionsService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.userRoles.data.PermissionLabels;
import de.aivot.GoverBackend.utils.StringUtils;
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

import java.util.Map;

@RestController
@RequestMapping("/api/departments/")
@Tag(
        name = "Departments",
        description = "Departments are organisational units within the system. " +
                "They can represent different sub-organizations, departments, or divisions within an organisation. " +
                "Departments help in structuring users and managing permissions effectively. " +
                "They also own certain resources and can have specific settings that apply to all users within the department."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class DepartmentController {
    private static final String MODULE_NAME = "Organisationseinheiten";

    private final ScopedAuditService auditService;

    private final DepartmentService departmentService;
    private final DepartmentRepository departmentRepository;
    private final VDepartmentMembershipWithPermissionsService vDepartmentMembershipWithPermissionsService;
    private final UserService userService;

    @Autowired
    public DepartmentController(AuditService auditService,
                                DepartmentService departmentService,
                                DepartmentRepository departmentRepository,
                                VDepartmentMembershipWithPermissionsService vDepartmentMembershipWithPermissionsService,
                                UserService userService) {
        this.auditService = auditService.createScopedAuditService(DepartmentController.class, MODULE_NAME);

        this.departmentService = departmentService;
        this.departmentRepository = departmentRepository;
        this.vDepartmentMembershipWithPermissionsService = vDepartmentMembershipWithPermissionsService;
        this.userService = userService;
    }

    @GetMapping("")
    @Operation(
            summary = "List departments",
            description = "List departments with pagination and filtering."
    )
    public Page<DepartmentEntity> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid DepartmentFilter filter
    ) throws ResponseException {
        return departmentService
                .list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create department",
            description = "Create a new department. This requires system admin permissions."
    )
    public DepartmentEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid DepartmentEntity newDepartment
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSystemAdmin()
                .orElseThrow(ResponseException::noSystemAdminPermission);

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
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        return departmentService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    @Operation(
            summary = "Update department",
            description = "Update a department. Requires super admin permissions or department edit permissions."
    )
    public DepartmentEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @RequestBody @Valid DepartmentEntity updatedDepartment
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!user.getIsSystemAdmin()) {
            var filter = VDepartmentMembershipWithPermissionsFilter
                    .create()
                    .setUserId(user.getId())
                    .setDepartmentId(id)
                    .setDepartmentPermissionEdit(true);

            var hasPermissionToEdit = vDepartmentMembershipWithPermissionsService
                    .exists(filter.build());

            if (!hasPermissionToEdit) {
                throw ResponseException
                        .noPermission(PermissionLabels.DepartmentPermissionEdit);
            }
        }

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
            description = "Delete a department. Requires super admin permissions."
    )
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSuperAdmin()
                .orElseThrow(ResponseException::noSuperAdminPermission);

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
