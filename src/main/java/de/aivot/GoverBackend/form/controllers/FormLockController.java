package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.enums.EntityLockState;
import de.aivot.GoverBackend.form.filters.FormWithMembershipFilter;
import de.aivot.GoverBackend.form.services.FormLockService;
import de.aivot.GoverBackend.form.services.FormWithMembershipService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.dtos.EntityLockDto;
import de.aivot.GoverBackend.user.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forms/{formId}/lock/")
public class FormLockController {
    private final FormLockService formLockService;
    private final FormWithMembershipService formWithMembershipService;

    @Autowired
    public FormLockController(
            FormLockService formLockService,
            FormWithMembershipService formWithMembershipService
    ) {
        this.formLockService = formLockService;
        this.formWithMembershipService = formWithMembershipService;
    }

    @GetMapping("")
    public EntityLockDto retrieve(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer formId
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var formAccessSpec = FormWithMembershipFilter
                .create()
                .setUserId(user.getId())
                .setId(formId)
                .build();

        if (!formWithMembershipService.exists(formAccessSpec)) {
            throw ResponseException.notFound("Das Formular existiert nicht oder Sie haben keinen Zugriff darauf.");
        }

        return formLockService
                .retrieve(formId)
                .map(
                        lock -> new EntityLockDto(
                                user.hasId(lock.getUserId()) ? EntityLockState.LockedSelf : EntityLockState.LockedOther,
                                lock.getUserId()
                        )
                )
                .orElse(new EntityLockDto(EntityLockState.Free, null));
    }

    @DeleteMapping("")
    public void delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer formId
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var lock = formLockService
                .retrieve(formId);

        if (lock.isPresent()) {
            var lockedByUserId = lock.get().getUserId();

            if (user.hasId(lockedByUserId)) {
                formLockService.delete(lock.get().getFormId());
            } else {
                throw ResponseException.conflict("Das Formular ist von einer anderen Mitarbeiter:in gesperrt.");
            }
        }
    }
}
