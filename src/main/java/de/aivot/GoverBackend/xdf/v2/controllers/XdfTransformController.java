package de.aivot.GoverBackend.xdf.v2.controllers;

import de.aivot.GoverBackend.form.dtos.FormDetailsResponseDTO;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.xdf.v2.models.XdfStammdatenschema0102;
import de.aivot.GoverBackend.xdf.v2.services.XdfTransformService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@RestController
@RequestMapping("/api/xdf/v2/")
public class XdfTransformController {
    private final XdfTransformService xdfTransformService;

    @Autowired
    public XdfTransformController(XdfTransformService xdfTransformService) {
        this.xdfTransformService = xdfTransformService;
    }

    @PostMapping(value = "transform/", consumes = MediaType.APPLICATION_XML_VALUE, produces =  MediaType.APPLICATION_JSON_VALUE)
    public FormDetailsResponseDTO transform(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid XdfStammdatenschema0102 request
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var formVersion = xdfTransformService
                .transformToGover(request);

        return FormDetailsResponseDTO
                .fromEntity(formVersion);
    }
}
