package de.aivot.GoverBackend.process.controllers;

import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.core.GenericCrudController;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.permissions.services.PermissionService;
import de.aivot.GoverBackend.process.entities.ProcessTestClaimEntity;
import de.aivot.GoverBackend.process.filters.ProcessTestClaimFilter;
import de.aivot.GoverBackend.process.permissions.ProcessPermissionProvider;
import de.aivot.GoverBackend.process.services.ProcessTestClaimService;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.services.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/process-test-claims/")
@Tag(
        name = OpenApiConstants.Tags.ProcessTestClaimsName,
        description = OpenApiConstants.Tags.ProcessTestClaimsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessTestClaimController extends GenericCrudController<ProcessTestClaimEntity, Integer, ProcessTestClaimFilter> {
    private final PermissionService permissionService;

    public ProcessTestClaimController(AuditService auditService,
                                      UserService userService,
                                      ProcessTestClaimService service,
                                      PermissionService permissionService) {
        super(
                auditService.createScopedAuditService(ProcessTestClaimController.class),
                userService,
                service
        );
        this.permissionService = permissionService;
    }

    // TODO: Limit list to accessible process definitions and perform permission check for all other endpoints

    @Override
    protected void checkListPermissions(@Nonnull UserEntity user) throws ResponseException {
        permissionService
                .testInAnyDepartmentPermission(user.getId(), ProcessPermissionProvider.PROCESS_DEFINITION_READ);
    }

    @Override
    protected ProcessTestClaimEntity performCreate(@Nonnull UserEntity execUser,
                                                   @Nonnull ProcessTestClaimEntity newItem) throws ResponseException {
        newItem.setOwningUserId(execUser.getId());
        return super.performCreate(execUser, newItem);
    }

    @Override
    protected Integer getIdForEntity(ProcessTestClaimEntity entity) {
        return entity.getId();
    }
}
