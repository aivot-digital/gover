package de.aivot.GoverBackend.plugin.services;

import de.aivot.GoverBackend.plugin.models.Plugin;
import de.aivot.GoverBackend.plugin.models.PluginComponent;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PluginStartupService implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger logger = LoggerFactory.getLogger(PluginStartupService.class);

    private final List<Plugin> plugins;
    private final List<PluginComponent> pluginComponents;

    @Autowired
    public PluginStartupService(List<Plugin> plugins, List<PluginComponent> pluginComponents) {
        this.plugins = plugins;
        this.pluginComponents = pluginComponents;
    }

    @Override
    public void onApplicationEvent(@Nonnull ApplicationReadyEvent event) {
        var knownPluginKeys = new HashMap<String, List<Plugin>>();
        for (var plugin : plugins) {
            var classList = knownPluginKeys.getOrDefault(plugin.getKey(), new ArrayList<>());
            classList.add(plugin);
            knownPluginKeys.put(plugin.getKey(), classList);
        }

        var hasErrors = false;
        for (var entry : knownPluginKeys.entrySet()) {
            if (entry.getValue().size() > 1) {
                logger.warn(
                        "Für den eindeutigen Plugin-Schlüssel {} werden mehrere Plugins registriert. " +
                                "Bitte stellen Sie sicher, dass für einen Schlüssel nur ein einziges Plugin registriert wird. " +
                                "Die Schlüssel der folgenden Plugins doppeln sich:\n{}",
                        StringUtils.quote(entry.getKey()),
                        entry
                                .getValue()
                                .stream()
                                .map(plugin -> String.format(
                                        "- %s Version %s von %s (Klasse %s)",
                                        plugin.getName(),
                                        plugin.getVersion(),
                                        plugin.getVendorName(),
                                        plugin.getClass().getCanonicalName()
                                ))
                                .collect(Collectors.joining("\n"))
                );
                hasErrors = true;
            }
        }

        if (hasErrors) {
            throw new RuntimeException("Es wurden doppelte Plugin-Schlüssel gefunden. Bitte korrigieren Sie die Fehler in den Log-Ausgaben und starten Sie die Anwendung erneut.");
        }

        var knownComponentKeys = new HashMap<String, Map<Integer, List<PluginComponent>>>();
        for (var component : pluginComponents) {
            var versionMap = knownComponentKeys.getOrDefault(component.getKey(), new HashMap<>());
            var classList = versionMap.getOrDefault(component.getMajorVersion(), new ArrayList<>());
            classList.add(component);
            versionMap.put(component.getMajorVersion(), classList);
            knownComponentKeys.put(component.getKey(), versionMap);
        }

        hasErrors = false;
        for (var keyEntry : knownComponentKeys.entrySet()) {
            for (var entry : keyEntry.getValue().entrySet()) {
                if (entry.getValue().size() > 1) {
                    logger.warn(
                            "Für die Major-Version {} des eindeutigen Plugin-Komponenten-Schlüssels {} werden mehrere Komponenten registriert. " +
                                    "Bitte stellen Sie sicher, dass für einen Schlüssel nur eine einzige Komponente registriert wird. " +
                                    "Die Schlüssel der folgenden Komponenten doppeln sich:\n{}",
                            entry.getKey(),
                            StringUtils.quote(keyEntry.getKey()),
                            entry
                                    .getValue()
                                    .stream()
                                    .map(component -> {
                                        var parentPluginKey = entry
                                                .getValue()
                                                .getFirst()
                                                .getParentPluginKey();

                                        if (knownPluginKeys.containsKey(parentPluginKey)) {
                                            var plugin = knownPluginKeys
                                                    .get(parentPluginKey)
                                                    .getFirst();

                                            return String.format(
                                                    "- %s Version %s des Plugins %s Version %s von %s (Klasse %s)",
                                                    component.getName(),
                                                    component.getComponentVersion(),
                                                    plugin.getName(),
                                                    plugin.getVersion(),
                                                    plugin.getVendorName(),
                                                    component.getClass().getCanonicalName()
                                            );
                                        } else {
                                            return String.format(
                                                    "- %s Version %s (Klasse %s)",
                                                    component.getName(),
                                                    component.getComponentVersion(),
                                                    component.getClass().getCanonicalName()
                                            );
                                        }
                                    })
                                    .collect(Collectors.joining("\n"))
                    );
                    hasErrors = true;
                }
            }
        }

        if (hasErrors) {
            throw new RuntimeException("Es wurden doppelte Plugin-Komponenten-Schlüssel gefunden. Bitte korrigieren Sie die Fehler in den Log-Ausgaben und starten Sie die Anwendung erneut.");
        }
    }
}
