package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.openApi.OpenApiConstants;
import de.aivot.prosuna.backend.process.dtos.ProcessDataStructureResponseDTO;
import de.aivot.prosuna.backend.process.services.ProcessNodeService;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/process-versions/")
@Tag(name = OpenApiConstants.Tags.ProcessesDefinitionsName, description = OpenApiConstants.Tags.ProcessesDefinitionsDescription)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessDataStructureController {
    private final ProcessNodeService processNodeService;
    private final UserService userService;

    public ProcessDataStructureController(@Nonnull ProcessNodeService processNodeService, @Nonnull UserService userService) {
        this.processNodeService = processNodeService;
        this.userService = userService;
    }

    @Nonnull
    @GetMapping("{processId}/{processVersion}/data-structure/")
    @Operation(summary = "Retrieve the inferred process data structure",
            description = "List statically known process data, element data and protected process data paths with diagnostics for a saved process version. Requires " + ProcessPermissionProvider.PROCESS_DEFINITION_READ + " for the process or at system level.")
    public ProcessDataStructureResponseDTO retrieve(@Nullable @AuthenticationPrincipal Jwt jwt,
                                                    @Nonnull @PathVariable Integer processId,
                                                    @Nonnull @PathVariable Integer processVersion) throws ResponseException {
        var user = userService.fromJWT(jwt).orElseThrow(ResponseException::unauthorized);
        return processNodeService.getDataStructure(user.getId(), processId, processVersion);
    }
}
