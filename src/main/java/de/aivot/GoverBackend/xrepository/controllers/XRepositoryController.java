package de.aivot.GoverBackend.xrepository.controllers;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.xrepository.models.XRepositoryCodeList;
import de.aivot.GoverBackend.xrepository.services.XRepositoryCodeListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/xrepository/")
public class XRepositoryController {
    private final XRepositoryCodeListService xRepositoryCodeListService;

    @Autowired
    public XRepositoryController(XRepositoryCodeListService xRepositoryCodeListService) {
        this.xRepositoryCodeListService = xRepositoryCodeListService;
    }

    @GetMapping(value = "codelists/{urn}/definition/")
    public XRepositoryCodeList definition(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String urn
    ) throws ResponseException {
        return xRepositoryCodeListService
                .getCodeList(urn);
    }

    @GetMapping(value = "codelists/{urn}/values/")
    public List<Map<String, String>> values(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String urn
    ) throws ResponseException {
        return xRepositoryCodeListService
                .getReducedCodeList(urn);
    }
}
