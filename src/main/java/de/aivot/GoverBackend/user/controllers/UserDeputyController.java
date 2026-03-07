package de.aivot.GoverBackend.user.controllers;

import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.core.GenericCrudController;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.process.entities.ProcessAccessControlEntity;
import de.aivot.GoverBackend.process.filters.ProcessAccessControlFilter;
import de.aivot.GoverBackend.process.services.ProcessAccessControlService;
import de.aivot.GoverBackend.user.entities.UserDeputyEntity;
import de.aivot.GoverBackend.user.filters.UserDeputyFilter;
import de.aivot.GoverBackend.user.services.UserDeputyService;
import de.aivot.GoverBackend.user.services.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/user-deputies/")
@Tag(
        name = OpenApiConstants.Tags.UserDeputiesName,
        description = OpenApiConstants.Tags.UserDeputiesDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class UserDeputyController extends GenericCrudController<UserDeputyEntity, Integer, UserDeputyFilter> {
    public UserDeputyController(AuditService auditService,
                                UserService userService,
                                UserDeputyService service) {
        super(auditService.createScopedAuditService(UserDeputyController.class),
                userService,
                service);
    }

    @Override
    protected Integer getIdForEntity(UserDeputyEntity entity) {
        return entity.getId();
    }

    // TODO: Implement Permission Checks
}
