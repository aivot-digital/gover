package de.aivot.GoverBackend.plugins.form;

import de.aivot.GoverBackend.plugin.models.Plugin;
import de.aivot.GoverBackend.system.properties.BuildProperties;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

@Component
public class Form implements Plugin {
    public static final String PLUGIN_KEY = "form";
    private final BuildProperties buildProperties;

    public Form(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @Nonnull
    @Override
    public String getKey() {
        return PLUGIN_KEY;
    }

    @Override
    public @Nonnull String getName() {
        return "Formulare";
    }

    @Override
    public @Nonnull String getDescription() {
        return "Die Formularerweiterung von Gover.";
    }

    @Override
    public @Nonnull String getBuildDate() {
        return buildProperties.getBuildTimestamp();
    }

    @Nonnull
    @Override
    public String getVersion() {
        return buildProperties.getBuildVersion();
    }

    @Override
    public @Nonnull String getVendorName() {
        return "Aivot";
    }

    @Nonnull
    @Override
    public String getVendorWebsite() {
        return "https://aivot.de";
    }

    @Nonnull
    @Override
    public String getChangelog() {
        return "";
    }
}
