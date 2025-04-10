package de.aivot.GoverBackend.config.controllers;

import de.aivot.GoverBackend.config.dtos.SystemConfigDefinitionResponseDTO;
import de.aivot.GoverBackend.config.models.SystemConfigDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * This controller provides the configuration definitions for the frontend.
 * The frontend should render some sort of input mask based on the configuration definitions.
 */
@RestController
@RequestMapping("/api/system-config-definitions/")
public class SystemConfigDefinitionController {
    private final List<SystemConfigDefinitionResponseDTO> systemConfigDefinitions;

    @Autowired
    public SystemConfigDefinitionController(
            List<SystemConfigDefinition> systemConfigDefinitions
    ) {
        // This is a bean so let's transform all definitions to DTOs in instantiation
        this.systemConfigDefinitions = systemConfigDefinitions
                .stream()
                .map(SystemConfigDefinitionResponseDTO::fromDefinition)
                .toList();
    }

    /**
     * Returns all system configuration definitions.
     *
     * @return List of all system configuration definitions.
     */
    @GetMapping("")
    public Page<SystemConfigDefinitionResponseDTO> list() {
        return new PageImpl<>(systemConfigDefinitions);
    }
}
