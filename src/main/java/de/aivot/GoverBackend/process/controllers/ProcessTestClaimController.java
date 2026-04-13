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
import de.aivot.GoverBackend.process.repositories.ProcessTestClaimRepository;
import de.aivot.GoverBackend.process.services.ProcessService;
import de.aivot.GoverBackend.process.services.ProcessTestClaimService;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.utils.StringUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
    private final ProcessTestClaimService processTestClaimService;
    private final ProcessTestClaimRepository processTestClaimRepository;

    public ProcessTestClaimController(AuditService auditService,
                                      UserService userService,
                                      ProcessTestClaimService service,
                                      PermissionService permissionService,
                                      ProcessTestClaimRepository processTestClaimRepository) {
        super(
                auditService.createScopedAuditService(ProcessTestClaimController.class, "Prozesse"),
                userService,
                service
        );
        this.permissionService = permissionService;
        this.processTestClaimService = service;
        this.processTestClaimRepository = processTestClaimRepository;
    }

    // region Create

    @Override
    protected void checkCreatePermissions(@Nonnull UserEntity execUser,
                                          @Nonnull ProcessTestClaimEntity newItem) throws ResponseException {
        var canPublishTestSystemwide = permissionService
                .hasSystemPermission(
                        execUser,
                        ProcessPermissionProvider.PROCESS_DEFINITION_PUBLISH_TEST
                );

        if (canPublishTestSystemwide) {
            return;
        }

        var canPublishTestAsDomainMember = processTestClaimRepository
                .hasProcessPermission(
                        execUser.getId(),
                        newItem.getProcessId(),
                        ProcessPermissionProvider.PROCESS_DEFINITION_PUBLISH_TEST
                );

        if (canPublishTestAsDomainMember) {
            return;
        }

        throw ResponseException.forbidden();
    }

    @Override
    protected ProcessTestClaimEntity performCreate(@Nonnull UserEntity execUser,
                                                   @Nonnull ProcessTestClaimEntity newItem) throws ResponseException {
        newItem.setOwningUserId(execUser.getId());
        return super.performCreate(execUser, newItem);
    }

    @Override
    @Nonnull
    protected String buildCreateAuditMessage(@Nonnull UserEntity execUser,
                                             @Nonnull ProcessTestClaimEntity createdItem) {
        return String.format(
                "Der Test-Claim mit der ID %s für den Prozess %s (Version %s) wurde von der Mitarbeiter:in %s erstellt.",
                StringUtils.quote(String.valueOf(createdItem.getId())),
                StringUtils.quote(String.valueOf(createdItem.getProcessId())),
                StringUtils.quote(String.valueOf(createdItem.getProcessVersion())),
                StringUtils.quote(execUser.getFullName())
        );
    }

    // endregion

    // region List

    @Override
    protected Page<ProcessTestClaimEntity> performList(@Nonnull UserEntity user,
                                                       @Nonnull Pageable pageable,
                                                       @Nonnull ProcessTestClaimFilter filter) throws ResponseException {
        var canReadProcessSystemwide = permissionService
                .hasSystemPermission(
                        user,
                        ProcessPermissionProvider.PROCESS_DEFINITION_READ
                );

        if (canReadProcessSystemwide) {
            return super.performList(user, pageable, filter);
        }

        var accessibleProcessIds = processTestClaimRepository
                .getProcessIdsWithPermission(
                        user.getId(),
                        ProcessPermissionProvider.PROCESS_DEFINITION_READ
                );
        if (accessibleProcessIds.isEmpty()) {
            return Page.empty(pageable);
        }

        filter.setProcessIds(accessibleProcessIds);

        return processTestClaimService
                .list(pageable, filter);
    }

    // endregion

    // region retrieve

    @Override
    protected void checkRetrievePermissions(@Nonnull UserEntity execUser,
                                            @Nonnull Integer itemid) throws ResponseException {
        var canReadProcessSystemwide = permissionService
                .hasSystemPermission(
                        execUser,
                        ProcessPermissionProvider.PROCESS_DEFINITION_READ
                );

        if (canReadProcessSystemwide) {
            return;
        }

        var entity = processTestClaimService
                .retrieve(itemid)
                .orElseThrow(ResponseException::notFound);

        var canReadProcessAsDomainMember = processTestClaimRepository
                .hasProcessPermission(
                        execUser.getId(),
                        entity.getProcessId(),
                        ProcessPermissionProvider.PROCESS_DEFINITION_READ
                );

        if (canReadProcessAsDomainMember) {
            return;
        }

        throw ResponseException.forbidden();
    }

    // endregion

    // region update

    @Override
    protected ProcessTestClaimEntity performUpdate(@Nonnull UserEntity execUser,
                                                   @Nonnull Integer itemId,
                                                   @Nonnull ProcessTestClaimEntity patchItem) throws ResponseException {
        // Do nothing to update
        return patchItem;
    }

    // endregion

    // region delete

    @Override
    protected void checkDeletePermission(@Nonnull UserEntity execUser,
                                         @Nonnull Integer itemid) throws ResponseException {
        var canPublishTestAsSystemAdmin = permissionService
                .hasSystemPermission(
                        execUser,
                        ProcessPermissionProvider.PROCESS_DEFINITION_PUBLISH_TEST
                );

        if (canPublishTestAsSystemAdmin) {
            return;
        }

        var entity = processTestClaimService
                .retrieve(itemid)
                .orElseThrow(ResponseException::notFound);

        var canPublishTestAsDomainMember = processTestClaimRepository.hasProcessPermission(
                execUser.getId(),
                entity.getProcessId(),
                ProcessPermissionProvider.PROCESS_DEFINITION_PUBLISH_TEST
        );

        if (canPublishTestAsDomainMember) {
            return;
        }

        throw ResponseException.forbidden();
    }

    @Override
    @Nonnull
    protected String buildDeleteAuditMessage(@Nonnull UserEntity execUser,
                                             @Nonnull Integer id,
                                             @Nonnull ProcessTestClaimEntity deletedItem) {
        return String.format(
                "Der Test-Claim mit der ID %s für den Prozess %s (Version %s) wurde von der Mitarbeiter:in %s gelöscht.",
                StringUtils.quote(String.valueOf(id)),
                StringUtils.quote(String.valueOf(deletedItem.getProcessId())),
                StringUtils.quote(String.valueOf(deletedItem.getProcessVersion())),
                StringUtils.quote(execUser.getFullName())
        );
    }

    // endregion

    @Override
    protected Integer getIdForEntity(ProcessTestClaimEntity entity) {
        return entity.getId();
    }
}
