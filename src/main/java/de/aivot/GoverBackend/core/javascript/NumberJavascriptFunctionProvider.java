package de.aivot.GoverBackend.core.javascript;

import de.aivot.GoverBackend.elements.models.form.input.NumberField;
import de.aivot.GoverBackend.javascript.providers.JavascriptFunctionProvider;
import org.graalvm.polyglot.HostAccess;
import org.springframework.stereotype.Service;

@Service
public class NumberJavascriptFunctionProvider implements JavascriptFunctionProvider {
    @Override
    public String getPackageName() {
        return "_number";
    }

    @Override
    public String getLabel() {
        return "zahlenangaben";
    }

    @Override
    public String getDescription() {
        return "Dieses Paket enthält Funktionen für Zahlenangaben.";
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
        return NumberField.formatGermanNumber(number.doubleValue(), decimalPlaces < 0 ? 2 : decimalPlaces);
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
