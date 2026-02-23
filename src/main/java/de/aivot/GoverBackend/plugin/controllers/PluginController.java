package de.aivot.GoverBackend.plugin.controllers;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.plugin.dtos.PluginComponentDTO;
import de.aivot.GoverBackend.plugin.dtos.PluginDTO;
import de.aivot.GoverBackend.plugin.models.Plugin;
import de.aivot.GoverBackend.plugin.models.PluginComponent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/plugins/")
@Tag(
        name = OpenApiConstants.Tags.PluginsName,
        description = OpenApiConstants.Tags.PluginsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class PluginController {
    private final List<PluginDTO> pluginList;
    private final Map<String, PluginDTO> pluginMap;

    @Autowired
    public PluginController(List<Plugin> plugins, List<PluginComponent> components) {
        var componentList = components
                .stream()
                .map(PluginComponentDTO::from)
                .toList();

        pluginMap = plugins
                .stream()
                .map(plugin -> {
                    var componentDTOs = componentList
                            .stream()
                            .filter(component -> component.parentPluginKey().equals(plugin.getKey()))
                            .toList();

                    return PluginDTO.from(plugin, componentDTOs);
                })
                .collect(Collectors.toMap(
                        PluginDTO::key,
                        plugin -> plugin,
                        (existing, replacement) -> existing
                ));

        pluginList = pluginMap
                .values()
                .stream()
                .sorted(Comparator.comparing(PluginDTO::name))
                .toList();
    }

    @GetMapping("")
    @Operation(
            summary = "List Plugins",
            description = "Retrieve a list of all installed plugins along with their details."
    )
    public List<PluginDTO> list() {
        return pluginList;
    }

    @GetMapping("{pluginKey}/")
    @Operation(
            summary = "Retrieve a Plugin",
            description = "Retrieve detailed information about a specific plugin by its unique key."
    )
    public PluginDTO retrievePlugin(
            @Nonnull @PathVariable String pluginKey
    ) throws ResponseException {
        var plugin = pluginMap.get(pluginKey);

        if (plugin == null) {
            throw ResponseException.notFound();
        }

        return plugin;
    }
}
