package de.aivot.prosuna.backend.system.controllers;

import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.system.dtos.SystemBuildDTO;
import de.aivot.prosuna.backend.system.properties.BuildProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lets the staff UI report the backend build next to its own, because a cached or separately deployed frontend can run
 * against a different backend build. Like the system health, this is available to every authenticated user.
 */
@RestController
@RequestMapping("/api/system/build/")
@Tag(name = "System", description = "System status of the application")
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class SystemBuildController {
    private final BuildProperties buildProperties;

    public SystemBuildController(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @GetMapping
    @Operation(summary = "Retrieve the backend build", description = "Returns the version, build number and build timestamp of the running backend.")
    @Nonnull
    public SystemBuildDTO getBuild() {
        return SystemBuildDTO.fromProperties(buildProperties);
    }
}
