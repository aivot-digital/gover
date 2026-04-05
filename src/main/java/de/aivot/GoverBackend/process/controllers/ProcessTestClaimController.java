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

    @Override
    protected void checkListPermissions(@Nonnull UserEntity user) throws ResponseException {
        // List permissions are checked in performList to support scoped domain filtering.
    }

    @Override
    protected Page<ProcessTestClaimEntity> performList(@Nonnull UserEntity user,
                                                       @Nonnull Pageable pageable,
                                                       @Nonnull ProcessTestClaimFilter filter) throws ResponseException {
        if (hasGlobalReadPermission(user.getId())) {
            return super.performList(user, pageable, filter);
        }

        var accessibleProcessIds = processTestClaimRepository.getProcessIdsWithPermission(
                user.getId(),
                ProcessPermissionProvider.PROCESS_DEFINITION_READ
        );
        if (accessibleProcessIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Specification<ProcessTestClaimEntity> specification = filter.build();
        Specification<ProcessTestClaimEntity> processScopeSpecification = (root, query, criteriaBuilder) ->
                root.get("processId").in(accessibleProcessIds);

        if (specification == null) {
            specification = processScopeSpecification;
        } else {
            specification = specification.and(processScopeSpecification);
        }

        return processTestClaimService.performList(pageable, specification, filter);
    }

    @Override
    protected void checkRetrievePermissions(@Nonnull UserEntity execUser,
                                            @Nonnull Integer itemid) throws ResponseException {
        var entity = processTestClaimService
                .retrieve(itemid)
                .orElseThrow(ResponseException::notFound);
        if (!canReadProcess(execUser.getId(), entity.getProcessId())) {
            throw ResponseException.forbidden();
        }
    }

    @Override
    protected void checkCreatePermissions(@Nonnull UserEntity execUser,
                                          @Nonnull ProcessTestClaimEntity newItem) throws ResponseException {
        if (!canManageProcess(execUser.getId(), newItem.getProcessId())) {
            throw ResponseException.forbidden();
        }
    }

    @Override
    protected void checkUpdatePermission(@Nonnull UserEntity execUser,
                                         @Nonnull Integer itemid) throws ResponseException {
        var entity = processTestClaimService
                .retrieve(itemid)
                .orElseThrow(ResponseException::notFound);
        if (!canManageProcess(execUser.getId(), entity.getProcessId())) {
            throw ResponseException.forbidden();
        }
    }

    @Override
    protected void checkDeletePermission(@Nonnull UserEntity execUser,
                                         @Nonnull Integer itemid) throws ResponseException {
        var entity = processTestClaimService
                .retrieve(itemid)
                .orElseThrow(ResponseException::notFound);
        if (!canManageProcess(execUser.getId(), entity.getProcessId())) {
            throw ResponseException.forbidden();
        }
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

    @Override
    @Nonnull
    protected String buildUpdateAuditMessage(@Nonnull UserEntity execUser,
                                             @Nonnull Integer id,
                                             @Nonnull ProcessTestClaimEntity updatedItem) {
        return String.format(
                "Der Test-Claim mit der ID %s für den Prozess %s (Version %s) wurde von der Mitarbeiter:in %s aktualisiert.",
                StringUtils.quote(String.valueOf(id)),
                StringUtils.quote(String.valueOf(updatedItem.getProcessId())),
                StringUtils.quote(String.valueOf(updatedItem.getProcessVersion())),
                StringUtils.quote(execUser.getFullName())
        );
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

    private boolean hasGlobalReadPermission(@Nonnull String userId) {
        return permissionService.hasSystemPermission(
                userId,
                ProcessPermissionProvider.PROCESS_DEFINITION_READ
        );
    }

    private boolean hasGlobalManagePermission(@Nonnull String userId) {
        return permissionService.hasSystemPermission(
                userId,
                ProcessPermissionProvider.PROCESS_DEFINITION_PUBLISH_TEST
        );
    }

    private boolean canReadProcess(@Nonnull String userId,
                                   @Nonnull Integer processId) {
        return hasGlobalReadPermission(userId)
                || processTestClaimRepository.hasProcessPermission(
                userId,
                processId,
                ProcessPermissionProvider.PROCESS_DEFINITION_READ
        );
    }

    private boolean canManageProcess(@Nonnull String userId,
                                     @Nonnull Integer processId) {
        return hasGlobalManagePermission(userId)
                || processTestClaimRepository.hasProcessPermission(
                userId,
                processId,
                ProcessPermissionProvider.PROCESS_DEFINITION_PUBLISH_LOCAL
        );
    }
}
