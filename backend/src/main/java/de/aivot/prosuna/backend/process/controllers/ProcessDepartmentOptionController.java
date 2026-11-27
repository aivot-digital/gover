package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.openApi.OpenApiConstants;
import de.aivot.prosuna.backend.process.dtos.ProcessDepartmentOptionDTO;
import de.aivot.prosuna.backend.process.services.ProcessDepartmentOptionService;
import de.aivot.prosuna.backend.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/processes/department-options/")
@Tag(name = OpenApiConstants.Tags.ProcessesDefinitionsName)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessDepartmentOptionController {
    private final ProcessDepartmentOptionService optionService;
    private final UserService userService;

    public ProcessDepartmentOptionController(ProcessDepartmentOptionService optionService, UserService userService) {
        this.optionService = optionService;
        this.userService = userService;
    }

    @Nonnull
    @GetMapping("")
    @Operation(summary = "List managing departments of visible processes",
            description = "Return IDs and names of departments owning at least one process the current user may read, independently of search and pagination.")
    public List<ProcessDepartmentOptionDTO> list(@Nullable @AuthenticationPrincipal Jwt jwt) throws ResponseException {
        var user = userService.fromJWT(jwt).orElseThrow(ResponseException::unauthorized);
        return optionService.listForUser(user.getId());
    }
}
