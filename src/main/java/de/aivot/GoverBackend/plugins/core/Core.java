package de.aivot.GoverBackend.plugins.core;

import de.aivot.GoverBackend.plugin.models.Plugin;
import de.aivot.GoverBackend.system.properties.BuildProperties;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

@Component
public class Core implements Plugin {
    public static final String PLUGIN_KEY = "de.aivot.core";
    private final BuildProperties buildProperties;

    public Core(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @Nonnull
    @Override
    public String getKey() {
        return PLUGIN_KEY;
    }

    @Override
    public @Nonnull String getName() {
        return "Gover Kernfunktionalitäten";
    }

    @Override
    public @Nonnull String getDescription() {
        return """
                Dieses Plugin enthält die Kernfunktionalitäten von Gover, einschließlich der standard Prozesselemente,
                Zahlungs- und Speicheranbieter, No-Code-Operatoren und JavaScript-Funktionsbibliotheken.
                Es bildet die Grundlage für die meisten Funktionen von Gover und ist eine Voraussetzung für die Installation anderer Plugins.
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
