package de.aivot.GoverBackend.config.controllers;

import de.aivot.GoverBackend.config.dtos.UserConfigDefinitionResponseDTO;
import de.aivot.GoverBackend.config.models.UserConfigDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/**
 * This controller provides the configuration definitions for the frontend.
 * The frontend should render some sort of input mask based on the configuration definitions.
 */
@RestController
@RequestMapping("/api/user-config-definitions/")
public class UserConfigDefinitionController {
    private final List<UserConfigDefinitionResponseDTO> userConfigDefinitions;

    @Autowired
    public UserConfigDefinitionController(
            List<UserConfigDefinition> userConfigDefinitions
    ) {
        // This is a bean so let's transform all definitions to DTOs in instantiation
        this.userConfigDefinitions = userConfigDefinitions
                .stream()
                .sorted(Comparator
                        .comparingInt(UserConfigDefinition::getCategoryOrder)
                        .thenComparingInt(UserConfigDefinition::getSubCategoryOrder)
                        .thenComparingInt(UserConfigDefinition::getDefinitionOrder)
                        .thenComparing(UserConfigDefinition::getKey)
                )
                .map(UserConfigDefinitionResponseDTO::fromDefinition)
                .toList();
    }

    /**
     * Returns all user configuration definitions.
     *
     * @return List of all user configuration definitions.
     */
    @GetMapping("")
    public Page<UserConfigDefinitionResponseDTO> list() {
        return new PageImpl<>(userConfigDefinitions);
    }
}
