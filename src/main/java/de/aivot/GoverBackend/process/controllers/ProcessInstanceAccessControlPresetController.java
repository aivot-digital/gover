package de.aivot.GoverBackend.process.controllers;

import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.core.GenericCrudController;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.process.entities.ProcessInstanceAccessControlPresetEntity;
import de.aivot.GoverBackend.process.filters.ProcessInstanceAccessControlPresetFilter;
import de.aivot.GoverBackend.process.services.ProcessInstanceAccessControlPresetService;
import de.aivot.GoverBackend.user.services.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    public ProcessInstanceAccessControlPresetController(AuditService auditService,
                                                        UserService userService,
                                                        ProcessInstanceAccessControlPresetService processInstanceAccessControlPresetService) {
        super(auditService.createScopedAuditService(ProcessInstanceAccessControlPresetController.class),
                userService,
                processInstanceAccessControlPresetService);
    }

    // TODO: Implement Permission Checks
}
