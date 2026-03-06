package de.aivot.GoverBackend.audit.controllers;

import de.aivot.GoverBackend.audit.entities.AuditLogEntity;
import de.aivot.GoverBackend.audit.filters.AuditLogFilter;
import de.aivot.GoverBackend.audit.permissions.AuditPermissionProvider;
import de.aivot.GoverBackend.audit.services.AuditLogService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.permissions.services.PermissionService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs/")
@Tag(
        name = OpenApiConstants.Tags.AuditLogsName,
        description = OpenApiConstants.Tags.AuditLogsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class AuditLogController {
    private final AuditLogService auditLogService;
    private final PermissionService permissionService;

    @Autowired
    public AuditLogController(AuditLogService auditLogService,
                              PermissionService permissionService) {
        this.auditLogService = auditLogService;
        this.permissionService = permissionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Audit Logs",
            description = "Retrieve a paginated list of audit logs with optional filtering. Requires the permission " + AuditPermissionProvider.AUDIT_LOG_READ + "."
    )
    public Page<AuditLogEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid AuditLogFilter filter
    ) throws ResponseException {
        permissionService
                .testSystemPermission(jwt, AuditPermissionProvider.AUDIT_LOG_READ);

        return auditLogService
                .list(pageable, filter);
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Audit Log",
            description = "Retrieve a specific audit log by id. Requires the permission " + AuditPermissionProvider.AUDIT_LOG_READ + "."
    )
    public AuditLogEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Long id
    ) throws ResponseException {
        permissionService
                .testSystemPermission(jwt, AuditPermissionProvider.AUDIT_LOG_READ);

        return auditLogService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }
}
