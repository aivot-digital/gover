package de.aivot.GoverBackend.process.controllers;

import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.core.GenericCrudController;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.process.entities.ProcessAccessControlEntity;
import de.aivot.GoverBackend.process.filters.ProcessAccessControlFilter;
import de.aivot.GoverBackend.process.services.ProcessAccessControlService;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.utils.StringUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
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
        super(auditService.createScopedAuditService(ProcessAccessControlController.class, "Prozesse"),
                userService,
                processAccessControlService);
    }

    @Override
    protected Integer getIdForEntity(ProcessAccessControlEntity entity) {
        return entity.getId();
    }

    @Override
    @Nonnull
    protected String buildCreateAuditMessage(@Nonnull UserEntity execUser,
                                             @Nonnull ProcessAccessControlEntity createdItem) {
        return String.format(
                "Die Prozess-Zugriffsregel mit der ID %s für den Zielprozess %s wurde von der Mitarbeiter:in %s erstellt.",
                StringUtils.quote(String.valueOf(createdItem.getId())),
                StringUtils.quote(String.valueOf(createdItem.getTargetProcessId())),
                StringUtils.quote(execUser.getFullName())
        );
    }

    @Override
    @Nonnull
    protected String buildUpdateAuditMessage(@Nonnull UserEntity execUser,
                                             @Nonnull Integer id,
                                             @Nonnull ProcessAccessControlEntity updatedItem) {
        return String.format(
                "Die Prozess-Zugriffsregel mit der ID %s für den Zielprozess %s wurde von der Mitarbeiter:in %s aktualisiert.",
                StringUtils.quote(String.valueOf(id)),
                StringUtils.quote(String.valueOf(updatedItem.getTargetProcessId())),
                StringUtils.quote(execUser.getFullName())
        );
    }

    @Override
    @Nonnull
    protected String buildDeleteAuditMessage(@Nonnull UserEntity execUser,
                                             @Nonnull Integer id,
                                             @Nonnull ProcessAccessControlEntity deletedItem) {
        return String.format(
                "Die Prozess-Zugriffsregel mit der ID %s für den Zielprozess %s wurde von der Mitarbeiter:in %s gelöscht.",
                StringUtils.quote(String.valueOf(id)),
                StringUtils.quote(String.valueOf(deletedItem.getTargetProcessId())),
                StringUtils.quote(execUser.getFullName())
        );
    }

    // TODO: Implement Permission Checks
}
