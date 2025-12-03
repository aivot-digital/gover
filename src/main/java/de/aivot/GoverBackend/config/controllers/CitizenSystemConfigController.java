package de.aivot.GoverBackend.config.controllers;

import de.aivot.GoverBackend.config.dtos.SystemConfigResponseDto;
import de.aivot.GoverBackend.config.filters.SystemConfigFilter;
import de.aivot.GoverBackend.config.services.SystemConfigService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Nonnull;

/**
 * This controller provides functionality to list, retrieve and update system configurations.
 */
@RestController
@RequestMapping("/api/public/system-configs/")
@Tag(
        name = "System Configurations",
        description = "System configurations are key-value pairs that define various settings and parameters of the application. " +
                      "These configurations can be used to customize the behavior of the system and should be used if you need to provide configuration values to citizens publicly."
)
public class CitizenSystemConfigController {
    private final SystemConfigService systemConfigService;

    @Autowired
    public CitizenSystemConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Public System Configurations",
            description = "Retrieve a paginated list of public system configurations."
    )
    public Page<SystemConfigResponseDto> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable
    ) {
        var filter = SystemConfigFilter
                .create()
                .setPublicConfig(true);

        return systemConfigService
                .list(pageable, filter)
                .map(entity -> {
                    var def = systemConfigService
                            .getDefinition(entity.getKey())
                            .orElseThrow(RuntimeException::new); // Should never happen
                    try {
                        return SystemConfigResponseDto.fromEntity(entity, def);
                    } catch (ResponseException e) {
                        throw new RuntimeException(e); // Should never happen
                    }
                });
    }
}
