package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.core.GenericCrudController;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.openApi.OpenApiConstants;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAccessControlPresetEntity;
import de.aivot.prosuna.backend.process.filters.ProcessInstanceAccessControlPresetFilter;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAccessControlPresetService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import de.aivot.prosuna.backend.utils.StringUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/process-instance-access-control-presets/")
@Tag(
        name = OpenApiConstants.Tags.ProcessAccessControlsName,
        description = OpenApiConstants.Tags.ProcessAccessControlsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessInstanceAccessControlPresetController extends GenericCrudController<ProcessInstanceAccessControlPresetEntity, Integer, ProcessInstanceAccessControlPresetFilter> {
    private final ProcessInstanceAccessControlPresetService processInstanceAccessControlPresetService;
    private final PermissionService permissionService;

    public ProcessInstanceAccessControlPresetController(AuditService auditService,
                                                        UserService userService,
                                                        ProcessInstanceAccessControlPresetService processInstanceAccessControlPresetService,
                                                        PermissionService permissionService) {
        super(auditService.createScopedAuditService(ProcessInstanceAccessControlPresetController.class, "Prozesse"),
                userService,
                processInstanceAccessControlPresetService);
        this.processInstanceAccessControlPresetService = processInstanceAccessControlPresetService;
        this.permissionService = permissionService;
    }

    @Override
    protected Integer getIdForEntity(ProcessInstanceAccessControlPresetEntity entity) {
        return entity.getId();
    }

    @Override
    protected Page<ProcessInstanceAccessControlPresetEntity> performList(@Nonnull UserEntity user,
                                                                         @Nonnull Pageable pageable,
                                                                         @Nonnull ProcessInstanceAccessControlPresetFilter filter) throws ResponseException {
        if (!permissionService.hasSystemPermission(user.getId(), ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE)) {
            if (filter.getTargetProcessId() != null) {
                permissionService.requireProcessPermission(
                        user.getId(),
                        filter.getTargetProcessId(),
                        ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE
                );
            } else {
                var accessibleProcessIds = permissionService
                        .getProcessesWithPermission(user.getId(), ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE);

                if (filter.getTargetProcessIds() != null) {
                    // Presets are attached to process definitions, so their administration follows process update access.
                    accessibleProcessIds = filter.getTargetProcessIds()
                            .stream()
                            .filter(accessibleProcessIds::contains)
                            .toList();
                }

                if (accessibleProcessIds.isEmpty()) {
                    return Page.empty(pageable);
                }

                filter.setTargetProcessIds(accessibleProcessIds);
            }
        }

        return super.performList(user, pageable, filter);
    }

    @Override
    protected void checkCreatePermissions(@Nonnull UserEntity execUser,
                                          @Nonnull ProcessInstanceAccessControlPresetEntity newItem) throws ResponseException {
        permissionService.requireProcessPermission(
                execUser.getId(),
                newItem.getTargetProcessId(),
                ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE
        );
    }

    @Override
    protected void checkRetrievePermissions(@Nonnull UserEntity execUser,
                                            @Nonnull Integer itemid) throws ResponseException {
        var existing = processInstanceAccessControlPresetService
                .retrieve(itemid)
                .orElseThrow(ResponseException::notFound);

        permissionService.requireProcessPermission(
                execUser.getId(),
                existing.getTargetProcessId(),
                ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE
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
                                             @Nonnull ProcessInstanceAccessControlPresetEntity createdItem) {
        return String.format(
                "Die Instanz-Zugriffsvorlage mit der ID %s für den Zielprozess %s wurde von der Mitarbeiter:in %s erstellt.",
                StringUtils.quote(String.valueOf(createdItem.getId())),
                StringUtils.quote(String.valueOf(createdItem.getTargetProcessId())),
                StringUtils.quote(execUser.getFullName())
        );
    }

    @Override
    @Nonnull
    protected String buildUpdateAuditMessage(@Nonnull UserEntity execUser,
                                             @Nonnull Integer id,
                                             @Nonnull ProcessInstanceAccessControlPresetEntity updatedItem) {
        return String.format(
                "Die Instanz-Zugriffsvorlage mit der ID %s für den Zielprozess %s wurde von der Mitarbeiter:in %s aktualisiert.",
                StringUtils.quote(String.valueOf(id)),
                StringUtils.quote(String.valueOf(updatedItem.getTargetProcessId())),
                StringUtils.quote(execUser.getFullName())
        );
    }

    @Override
    @Nonnull
    protected String buildDeleteAuditMessage(@Nonnull UserEntity execUser,
                                             @Nonnull Integer id,
                                             @Nonnull ProcessInstanceAccessControlPresetEntity deletedItem) {
        return String.format(
                "Die Instanz-Zugriffsvorlage mit der ID %s für den Zielprozess %s wurde von der Mitarbeiter:in %s gelöscht.",
                StringUtils.quote(String.valueOf(id)),
                StringUtils.quote(String.valueOf(deletedItem.getTargetProcessId())),
                StringUtils.quote(execUser.getFullName())
        );
    }

}
