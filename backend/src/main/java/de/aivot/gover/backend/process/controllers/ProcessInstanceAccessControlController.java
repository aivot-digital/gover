package de.aivot.gover.backend.process.controllers;

import de.aivot.gover.backend.audit.services.AuditService;
import de.aivot.gover.backend.core.GenericCrudController;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.openApi.OpenApiConfiguration;
import de.aivot.gover.backend.openApi.OpenApiConstants;
import de.aivot.gover.backend.permissions.services.PermissionService;
import de.aivot.gover.backend.process.entities.ProcessInstanceAccessControlEntity;
import de.aivot.gover.backend.process.filters.ProcessInstanceAccessControlFilter;
import de.aivot.gover.backend.process.models.ProcessInstanceAccessSelectableItem;
import de.aivot.gover.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.gover.backend.process.services.PotentialProcessInstanceAccessService;
import de.aivot.gover.backend.process.services.ProcessInstanceAccessControlService;
import de.aivot.gover.backend.user.entities.UserEntity;
import de.aivot.gover.backend.user.services.UserService;
import de.aivot.gover.backend.utils.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/process-instance-access-controls/")
@Tag(
        name = OpenApiConstants.Tags.ProcessAccessControlsName,
        description = OpenApiConstants.Tags.ProcessAccessControlsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessInstanceAccessControlController extends GenericCrudController<ProcessInstanceAccessControlEntity, Integer, ProcessInstanceAccessControlFilter> {
    private final UserService userService;
    private final PotentialProcessInstanceAccessService potentialProcessInstanceAccessService;
    private final ProcessInstanceAccessControlService processInstanceAccessControlService;
    private final PermissionService permissionService;

    public ProcessInstanceAccessControlController(AuditService auditService,
                                                  UserService userService,
                                                  ProcessInstanceAccessControlService processInstanceAccessControlService,
                                                  PotentialProcessInstanceAccessService potentialProcessInstanceAccessService,
                                                  PermissionService permissionService) {
        super(auditService.createScopedAuditService(ProcessInstanceAccessControlController.class, "Prozesse"),
                userService,
                processInstanceAccessControlService);
        this.userService = userService;
        this.processInstanceAccessControlService = processInstanceAccessControlService;
        this.potentialProcessInstanceAccessService = potentialProcessInstanceAccessService;
        this.permissionService = permissionService;
    }

    @GetMapping("potential-options/")
    @Operation(
            summary = "List potential selectable assignees for a process instance",
            description = "Returns users, organisation units and teams that can be selected based on process access and optional required permissions."
    )
    public List<ProcessInstanceAccessSelectableItem> listPotentialOptions(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestParam Integer processId,
            @Nonnull @RequestParam Integer processVersion,
            @RequestParam(required = false) List<String> requiredPermissions
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService.requireProcessPermission(
                user.getId(),
                processId,
                ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE
        );

        return potentialProcessInstanceAccessService.listSelectableItems(
                processId,
                processVersion,
                requiredPermissions
        );
    }

    @Override
    protected Integer getIdForEntity(ProcessInstanceAccessControlEntity entity) {
        return entity.getId();
    }

    @Override
    protected Page<ProcessInstanceAccessControlEntity> performList(@Nonnull UserEntity user,
                                                                   @Nonnull Pageable pageable,
                                                                   @Nonnull ProcessInstanceAccessControlFilter filter) throws ResponseException {
        if (!permissionService.hasSystemPermission(user.getId(), ProcessPermissionProvider.PROCESS_INSTANCE_UPDATE)) {
            if (filter.getTargetProcessInstanceId() != null) {
                permissionService.requireProcessInstancePermission(
                        user.getId(),
                        filter.getTargetProcessInstanceId().longValue(),
                        ProcessPermissionProvider.PROCESS_INSTANCE_UPDATE
                );
            } else {
                var accessibleProcessInstanceIds = permissionService
                        .getProcessInstancesWithPermission(user.getId(), ProcessPermissionProvider.PROCESS_INSTANCE_UPDATE)
                        .stream()
                        .map(Long::intValue)
                        .toList();

                if (filter.getTargetProcessInstanceIds() != null) {
                    // Keep requested targets, but only where the user may administer instance access rules.
                    accessibleProcessInstanceIds = filter.getTargetProcessInstanceIds()
                            .stream()
                            .filter(accessibleProcessInstanceIds::contains)
                            .toList();
                }

                if (accessibleProcessInstanceIds.isEmpty()) {
                    return Page.empty(pageable);
                }

                filter.setTargetProcessInstanceIds(accessibleProcessInstanceIds);
            }
        }

        return super.performList(user, pageable, filter);
    }

    @Override
    protected void checkCreatePermissions(@Nonnull UserEntity execUser,
                                          @Nonnull ProcessInstanceAccessControlEntity newItem) throws ResponseException {
        permissionService.requireProcessInstancePermission(
                execUser.getId(),
                newItem.getTargetProcessInstanceId().longValue(),
                ProcessPermissionProvider.PROCESS_INSTANCE_UPDATE
        );
    }

    @Override
    protected void checkRetrievePermissions(@Nonnull UserEntity execUser,
                                            @Nonnull Integer itemid) throws ResponseException {
        var existing = processInstanceAccessControlService
                .retrieve(itemid)
                .orElseThrow(ResponseException::notFound);

        permissionService.requireProcessInstancePermission(
                execUser.getId(),
                existing.getTargetProcessInstanceId().longValue(),
                ProcessPermissionProvider.PROCESS_INSTANCE_UPDATE
        );
    }

    @Override
    protected void checkUpdatePermission(@Nonnull UserEntity execUser,
                                         @Nonnull Integer itemid) throws ResponseException {
        checkRetrievePermissions(execUser, itemid);
    }

    @Override
    protected void checkDeletePermission(@Nonnull UserEntity execUser,
                                         @Nonnull Integer itemid) throws ResponseException {
        checkRetrievePermissions(execUser, itemid);
    }

    @Override
    @Nonnull
    protected String buildCreateAuditMessage(@Nonnull UserEntity execUser,
                                             @Nonnull ProcessInstanceAccessControlEntity createdItem) {
        return String.format(
                "Die Instanz-Zugriffsregel mit der ID %s für die Prozessinstanz %s wurde von der Mitarbeiter:in %s erstellt.",
                StringUtils.quote(String.valueOf(createdItem.getId())),
                StringUtils.quote(String.valueOf(createdItem.getTargetProcessInstanceId())),
                StringUtils.quote(execUser.getFullName())
        );
    }

    @Override
    @Nonnull
    protected String buildUpdateAuditMessage(@Nonnull UserEntity execUser,
                                             @Nonnull Integer id,
                                             @Nonnull ProcessInstanceAccessControlEntity updatedItem) {
        return String.format(
                "Die Instanz-Zugriffsregel mit der ID %s für die Prozessinstanz %s wurde von der Mitarbeiter:in %s aktualisiert.",
                StringUtils.quote(String.valueOf(id)),
                StringUtils.quote(String.valueOf(updatedItem.getTargetProcessInstanceId())),
                StringUtils.quote(execUser.getFullName())
        );
    }

    @Override
    @Nonnull
    protected String buildDeleteAuditMessage(@Nonnull UserEntity execUser,
                                             @Nonnull Integer id,
                                             @Nonnull ProcessInstanceAccessControlEntity deletedItem) {
        return String.format(
                "Die Instanz-Zugriffsregel mit der ID %s für die Prozessinstanz %s wurde von der Mitarbeiter:in %s gelöscht.",
                StringUtils.quote(String.valueOf(id)),
                StringUtils.quote(String.valueOf(deletedItem.getTargetProcessInstanceId())),
                StringUtils.quote(execUser.getFullName())
        );
    }

}
