package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.enums.EntityLockState;
import de.aivot.GoverBackend.form.entities.VFormWithPermissionsEntity;
import de.aivot.GoverBackend.form.services.FormLockService;
import de.aivot.GoverBackend.form.services.VFormWithPermissionsService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.dtos.EntityLockDto;
import de.aivot.GoverBackend.openApi.OpenAPIConfiguration;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.userRoles.data.PermissionLabels;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forms/{formId}/lock/")
@Tag(
        name = "Forms",
        description = "Forms are built for collecting data from users. " +
                      "They can be designed with various elements and configurations to suit different data collection needs. " +
                      "Forms can be published, managed, and analyzed within the system."
)
@SecurityRequirement(name = OpenAPIConfiguration.Name)
public class FormLockController {
    private final FormLockService formLockService;
    private final VFormWithPermissionsService vFormWithPermissionsService;
    private final UserService userService;

    @Autowired
    public FormLockController(
            FormLockService formLockService,
            VFormWithPermissionsService vFormWithPermissionsService, UserService userService) {
        this.formLockService = formLockService;
        this.vFormWithPermissionsService = vFormWithPermissionsService;
        this.userService = userService;
    }

    @GetMapping("")
    @Operation(
            summary = "Retrieve form lock",
            description = "Retrieve the lock status of a form. " +
                          "Indicates whether the form is locked, and if so, by which user."
    )
    public EntityLockDto retrieve(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer formId
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!execUser.getIsSuperAdmin()) {
            vFormWithPermissionsService.checkUserPermission(
                    formId,
                    execUser.getId(),
                    VFormWithPermissionsEntity::getFormPermissionRead,
                    PermissionLabels.FormPermissionRead);
        }

        return formLockService
                .retrieve(formId)
                .map(
                        lock -> new EntityLockDto(
                                execUser.hasId(lock.getUserId()) ? EntityLockState.LockedSelf : EntityLockState.LockedOther,
                                lock.getUserId()
                        )
                )
                .orElse(new EntityLockDto(EntityLockState.Free, null));
    }

    @DeleteMapping("")
    @Operation(
            summary = "Delete form lock",
            description = "Delete the lock on a form. " +
                          "Only the user who created the lock can delete it."
    )
    public void delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer formId
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var lock = formLockService
                .retrieve(formId);

        if (lock.isPresent()) {
            var lockedByUserId = lock.get().getUserId();

            if (execUser.hasId(lockedByUserId)) {
                formLockService.delete(lock.get().getFormId());
            } else {
                throw ResponseException.conflict("Das Formular ist von einer anderen Mitarbeiter:in gesperrt.");
            }
        }
    }
}
