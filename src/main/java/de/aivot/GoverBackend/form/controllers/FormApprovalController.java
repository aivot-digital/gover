package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.elements.enums.ElementApprovalStatus;
import de.aivot.GoverBackend.elements.services.ElementApprovalService;
import de.aivot.GoverBackend.form.services.FormService;
import de.aivot.GoverBackend.form.services.FormVersionWithDetailsService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.user.services.UserService;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Nullable;
import java.util.Map;

@RestController
@RequestMapping("/api/forms/{formId}/{formVersion}/approvals/")
public class FormApprovalController {
    private final FormService formService;
    private final FormVersionWithDetailsService formVersionWithDetailsService;

    @Autowired
    public FormApprovalController(
            FormService formService,
            FormVersionWithDetailsService formVersionWithDetailsService) {
        this.formService = formService;
        this.formVersionWithDetailsService = formVersionWithDetailsService;
    }

    @GetMapping("")
    public Map<String, ElementApprovalStatus> retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId,
            @Nonnull @PathVariable Integer formVersion
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var form = formVersionWithDetailsService
                .retrieve(formId)
                .orElseThrow(ResponseException::notFound);

        return ElementApprovalService
                .determineApprovals(form.getRootElement());
    }
}
