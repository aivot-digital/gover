package de.aivot.GoverBackend.plugins.legacy;

import de.aivot.GoverBackend.plugin.models.Plugin;
import de.aivot.GoverBackend.system.properties.BuildProperties;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

@Component
public class Legacy implements Plugin {
    public static final String PLUGIN_KEY = "de.aivot.legacy";
    private final BuildProperties buildProperties;

    public Legacy(BuildProperties buildProperties) {
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
                Dieses **Plugin** enthält die Kernfunktionalitäten von Gover, einschließlich der standard Prozesselemente,
                Zahlungs- und Speicheranbieter, No-Code-Operatoren und JavaScript-Funktionsbibliotheken.
                Es bildet die Grundlage für die meisten Funktionen von Gover und ist eine Voraussetzung für die Installation anderer Plugins.
                """;
    }

    @Nullable
    @Override
    public String getDeprecationNotice() {
        return """
                Dieses Plugin ist veraltet und wird in Zukunft durch spezialisierte Plugins ersetzt, die die Kernfunktionalitäten von Gover bereitstellen.
                **Bitte installieren Sie stattdessen die folgenden Plugins:**
                  - **Gover Elements**: Enthält alle standard Prozesselemente, einschließlich der Formularelemente, Logik-Operatoren, Trigger und mehr.
                  - **Gover Storage**: Bietet die standard Speicheranbieter, einschließlich der lokalen Speicherung, Datenbankanbindung und mehr.
                  - **Gover Payments**: Bietet die standard Zahlungsanbieter, einschließlich der Anbindung an verschiedene Zahlungsdienstleister und mehr.
                  - **Gover No-Code**: Enthält die standard No-Code-Operatoren, einschließlich der Logik-Operatoren, Trigger und mehr.
                  - **Gover JavaScript**: Enthält die standard JavaScript-Funktionsbibliotheken, einschließlich der Funktionen für die Arbeit mit Daten, Zeit, Strings und mehr.
                """;
    }

    @Override
    public @Nonnull String getBuildDate() {
        return buildProperties.getBuildTimestamp();
    }

    @Nonnull
    @Override
    public String getVersion() {
        return "4.6.0";
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
