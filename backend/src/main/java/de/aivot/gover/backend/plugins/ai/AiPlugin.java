package de.aivot.gover.backend.plugins.ai;

import de.aivot.gover.backend.plugin.models.Plugin;
import de.aivot.gover.backend.system.properties.BuildProperties;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

@Component
public class AiPlugin implements Plugin {
    public static final String PLUGIN_KEY = "de.aivot.ai";
    private final BuildProperties buildProperties;

    public AiPlugin(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @Nonnull
    @Override
    public String getKey() {
        return PLUGIN_KEY;
    }

    @Override
    public @Nonnull String getName() {
        return "Gover AI-Funktionen";
    }

    @Override
    public @Nonnull String getDescription() {
        return """
                Dieses Plugin enthält AI-Funktionen von Gover.
                """;
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
        return "Aivot UG (haftungsbeschränkt)";
    }

    @Nonnull
    @Override
    public String getVendorWebsite() {
        return "https://aivot.de";
    }

    @Nonnull
    @Override
    public String getChangelog() {
        return """
                # Changelog
                
                All notable changes to this project will be documented in this file.
                
                The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
                and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
                
                ## [5.0.0] - TBD
                ### Added
                ### Fixed
                ### Changed
                ### Deprecated
                ### Removed
                """;
    }
}
