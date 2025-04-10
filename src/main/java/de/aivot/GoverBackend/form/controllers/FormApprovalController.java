package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.elements.enums.ElementApprovalStatus;
import de.aivot.GoverBackend.elements.services.ElementApprovalService;
import de.aivot.GoverBackend.form.services.FormService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.user.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/forms/{formId}/approvals/")
public class FormApprovalController {
    private final FormService formService;

    @Autowired
    public FormApprovalController(
            FormService formService
    ) {
        this.formService = formService;
    }

    @GetMapping("")
    public Map<String, ElementApprovalStatus> retrieve(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer formId
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var form = formService
                .retrieve(formId)
                .orElseThrow(ResponseException::notFound);

        return ElementApprovalService
                .determineApprovals(form.getRoot());
    }
}
