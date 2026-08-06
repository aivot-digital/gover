package de.aivot.gover.backend.codeLists.controllers;

import de.aivot.gover.backend.codeLists.permissions.CodeListPermissionProvider;
import de.aivot.gover.backend.codeLists.services.CodeListService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.openApi.OpenApiConstants;
import de.aivot.gover.backend.permissions.services.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/code-list-utils/")
@Tag(
        name = OpenApiConstants.Tags.CodeListName,
        description = OpenApiConstants.Tags.CodeListDescription
)
public class CodeListUtilsController {
    private final CodeListService service;
    private final PermissionService permissionService;

    @Autowired
    public CodeListUtilsController(CodeListService service, PermissionService permissionService) {
        this.service = service;
        this.permissionService = permissionService;
    }

    @GetMapping("asset/{assetKey}/columns/")
    @Operation(
            summary = "Read Code List Asset Columns",
            description = "Read the column names from the header row of a CSV asset."
    )
    public List<String> getAssetColumns(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID assetKey
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, CodeListPermissionProvider.CODE_LIST_READ);
        return service.getAssetColumns(assetKey);
    }

    @GetMapping("x-repository/{urn}/columns/")
    @Operation(
            summary = "Read XRepository Code List Columns",
            description = "Read the column names of an XRepository code list."
    )
    public List<String> getXRepositoryColumns(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String urn
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, CodeListPermissionProvider.CODE_LIST_READ);
        return service.getXRepositoryColumns(urn);
    }
}
