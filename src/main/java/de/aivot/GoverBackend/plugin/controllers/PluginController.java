package de.aivot.GoverBackend.plugin.controllers;

import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.plugin.dtos.PluginComponentDTO;
import de.aivot.GoverBackend.plugin.dtos.PluginDTO;
import de.aivot.GoverBackend.plugin.models.Plugin;
import de.aivot.GoverBackend.plugin.models.PluginComponent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;


@RestController
@RequestMapping("/api/plugins/")
@Tag(
        name = "Plugins",
        description = "Plugins extend the application platform with additional features and functionalities."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class PluginController {
    private final List<PluginDTO> plugins;

    @Autowired
    public PluginController(List<Plugin> plugins, List<PluginComponent> components) {
        this.plugins = plugins.
                stream()
                .map(plugin -> {
                    var componentDTOs = components
                            .stream()
                            .filter(component -> component.getParentPluginKey().equals(plugin.getKey()))
                            .sorted(Comparator.comparing(PluginComponent::getName))
                            .map(component -> new PluginComponentDTO(
                                    component.getName(),
                                    component.getDescription(),
                                    component.getVersion()
                            ))
                            .toList();

                    return new PluginDTO(
                            plugin.getName(),
                            plugin.getDescription(),
                            plugin.getBuildDate(),
                            plugin.getVersion(),
                            plugin.getVendorName(),
                            componentDTOs
                    );
                }).toList();
    }

    @GetMapping("")
    @Operation(
            summary = "List Plugins",
            description = "Retrieve a list of all installed plugins along with their details."
    )
    public List<PluginDTO> list() {
        return plugins;
    }
}
