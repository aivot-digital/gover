package de.aivot.gover.backend.codeLists.controllers;

import de.aivot.gover.backend.codeLists.services.CodeListService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.openApi.OpenApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public CodeListUtilsController(CodeListService service) {
        this.service = service;
    }

    @GetMapping("asset/{assetKey}/columns/")
    @Operation(
            summary = "Read Code List Asset Columns",
            description = "Read the column names from the header row of a CSV asset."
    )
    public List<String> getAssetColumns(
            @Nonnull @PathVariable UUID assetKey
    ) throws ResponseException {
        // TODO: Permission Check
        return service.getAssetColumns(assetKey);
    }

    @GetMapping("x-repository/{urn}/columns/")
    @Operation(
            summary = "Read XRepository Code List Columns",
            description = "Read the column names of an XRepository code list."
    )
    public List<String> getXRepositoryColumns(
            @Nonnull @PathVariable String urn
    ) throws ResponseException {
        // TODO: Permission Check
        return service.getXRepositoryColumns(urn);
    }
}
