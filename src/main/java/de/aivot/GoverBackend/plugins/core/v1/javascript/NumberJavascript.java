package de.aivot.GoverBackend.plugins.core.v1.javascript;

import de.aivot.GoverBackend.elements.models.elements.form.input.NumberInputElement;
import de.aivot.GoverBackend.javascript.providers.JavascriptFunctionProvider;
import de.aivot.GoverBackend.plugin.models.PluginComponent;
import de.aivot.GoverBackend.plugins.core.Core;
import jakarta.annotation.Nonnull;
import org.graalvm.polyglot.HostAccess;
import org.springframework.stereotype.Service;

@Service
public class NumberJavascript implements JavascriptFunctionProvider, PluginComponent {
    @Override
    public @Nonnull String getKey() {
        return "number";
    }

    @Nonnull
    @Override
    public Integer getVersion() {
        return 1;
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return Core.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public String getName() {
        return "Zahlenfunktionen";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Dieses Modul stellt Funktionen zur Verarbeitung von Zahlenwerten bereit.";
    }

    @Override
    public String getObjectName() {
        return "_" + getKey();
    }

    @Override
    public String[] getMethodTypeDefinitions() {
        return new String[]{
                "formatGermanNumber(number: number): string;",
                "formatGermanNumber(number: number, decimalPlaces: number): string;",
                "parseGermanNumber(numberStr: string): number;"
        };
    }

    @HostAccess.Export
    public String formatGermanNumber(Number number) {
        if (number == null) {
            throw new IllegalArgumentException("Number cannot be null");
        }
        return formatGermanNumber(number.doubleValue(), 2);
    }

    @HostAccess.Export
    public String formatGermanNumber(Number number, int decimalPlaces) {
        if (number == null) {
            throw new IllegalArgumentException("Number cannot be null");
        }
        return NumberInputElement.formatGermanNumber(number.doubleValue(), decimalPlaces < 0 ? 2 : decimalPlaces);
    }

    @HostAccess.Export
    public Double parseGermanNumber(String numberStr) {
        if (numberStr == null || numberStr.isEmpty()) {
            throw new IllegalArgumentException("Input string cannot be null or empty");
        }

        var cleanedStr = numberStr
                .replace(".", "")
                .replace(",", ".");

        try {
            return Double.parseDouble(cleanedStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format: " + numberStr, e);
        }
    }
}
