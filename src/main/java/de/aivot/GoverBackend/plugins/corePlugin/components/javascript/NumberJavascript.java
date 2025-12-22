package de.aivot.GoverBackend.plugins.corePlugin.components.javascript;

import de.aivot.GoverBackend.elements.models.elements.form.input.NumberInputElement;
import de.aivot.GoverBackend.javascript.providers.JavascriptFunctionProvider;
import de.aivot.GoverBackend.plugin.models.PluginComponent;
import de.aivot.GoverBackend.plugins.corePlugin.Core;
import org.graalvm.polyglot.HostAccess;
import org.springframework.stereotype.Service;

@Service
public class NumberJavascript implements JavascriptFunctionProvider, PluginComponent {
    @Override
    public String getKey() {
        return "number";
    }

    @Override
    public String getParentPluginKey() {
        return Core.PLUGIN_KEY;
    }

    @Override
    public String getName() {
        return "Zahlenfunktionen";
    }

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
