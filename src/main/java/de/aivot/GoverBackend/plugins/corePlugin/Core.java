package de.aivot.GoverBackend.plugins.corePlugin;

import de.aivot.GoverBackend.plugin.models.Plugin;
import de.aivot.GoverBackend.system.properties.BuildProperties;
import org.springframework.stereotype.Component;

@Component
public class Core implements Plugin {
    public static final String PLUGIN_KEY = "core";
    private final BuildProperties buildProperties;

    public Core(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @Override
    public String getKey() {
        return PLUGIN_KEY;
    }

    @Override
    public String getName() {
        return "Kern";
    }

    @Override
    public String getDescription() {
        return "Der Kern von Gover.";
    }

    @Override
    public String getBuildDate() {
        return buildProperties.getBuildTimestamp();
    }

    @Override
    public String getVersion() {
        return buildProperties.getBuildVersion();
    }

    @Override
    public String getVendorName() {
        return "Aivot";
    }
}
