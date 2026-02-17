package de.aivot.GoverBackend.process.controllers;

import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.core.GenericCrudController;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.process.entities.ProcessAccessControlEntity;
import de.aivot.GoverBackend.process.filters.ProcessAccessControlFilter;
import de.aivot.GoverBackend.process.services.ProcessAccessControlService;
import de.aivot.GoverBackend.user.services.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/process-access-controls/")
@Tag(
        name = OpenApiConstants.Tags.ProcessAccessControlsName,
        description = OpenApiConstants.Tags.ProcessAccessControlsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessAccessControlController extends GenericCrudController<ProcessAccessControlEntity, Integer, ProcessAccessControlFilter> {
    public ProcessAccessControlController(AuditService auditService,
                                          UserService userService,
                                          ProcessAccessControlService processAccessControlService) {
        super(auditService.createScopedAuditService(ProcessAccessControlController.class),
                userService,
                processAccessControlService);
    }

    // TODO: Implement Permission Checks
}
