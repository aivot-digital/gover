package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.core.GenericReadController;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.openApi.OpenApiConstants;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import de.aivot.prosuna.backend.process.filters.ProcessInstanceAttachmentSetFilter;
import de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentSetService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/process-instance-attachment-sets/")
@Tag(
        name = OpenApiConstants.Tags.ProcessesDefinitionsName,
        description = "Operations for reading process instance attachment sets."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessInstanceAttachmentSetController extends GenericReadController<ProcessInstanceAttachmentSetEntity, Integer, ProcessInstanceAttachmentSetFilter> {
    private final ProcessInstanceAttachmentSetService processInstanceAttachmentSetService;
    private final PermissionService permissionService;

    public ProcessInstanceAttachmentSetController(UserService userService,
                                                  ProcessInstanceAttachmentSetService processInstanceAttachmentSetService,
                                                  PermissionService permissionService) {
        super(userService, processInstanceAttachmentSetService);
        this.processInstanceAttachmentSetService = processInstanceAttachmentSetService;
        this.permissionService = permissionService;
    }

    @Override
    protected Page<ProcessInstanceAttachmentSetEntity> performList(@Nonnull UserEntity user,
                                                                   @Nonnull Pageable pageable,
                                                                   @Nonnull ProcessInstanceAttachmentSetFilter filter) throws ResponseException {
        if (!permissionService.hasSystemPermission(user.getId(), ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ)) {
            if (filter.getProcessInstanceId() != null) {
                permissionService.requireProcessInstancePermission(
                        user.getId(),
                        filter.getProcessInstanceId(),
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ
                );
            } else {
                var accessibleProcessInstanceIds = permissionService
                        .getProcessInstancesWithPermission(user.getId(), ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ);

                if (filter.getProcessInstanceIds() != null) {
                    accessibleProcessInstanceIds = filter.getProcessInstanceIds()
                            .stream()
                            .filter(accessibleProcessInstanceIds::contains)
                            .toList();
                }

                if (accessibleProcessInstanceIds.isEmpty()) {
                    return Page.empty(pageable);
                }

                filter.setProcessInstanceIds(accessibleProcessInstanceIds);
            }
        }

        return processInstanceAttachmentSetService.list(pageable, filter);
    }

    @Override
    protected ProcessInstanceAttachmentSetEntity performRetrieve(@Nonnull UserEntity user,
                                                                  @Nonnull Integer itemId) throws ResponseException {
        var attachmentSet = processInstanceAttachmentSetService
                .retrieve(itemId)
                .orElseThrow(ResponseException::notFound);

        permissionService.requireProcessInstancePermission(
                user.getId(),
                attachmentSet.getProcessInstanceId(),
                ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ
        );

        return attachmentSet;
    }
}
