package de.aivot.prosuna.backend.plugins.core.v1.javascript;

import de.aivot.prosuna.backend.javascript.providers.JavascriptFunctionProvider;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.utils.PhoneNumberUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.graalvm.polyglot.HostAccess;
import org.springframework.stereotype.Service;

@Service
public class PhoneNumberJavascriptV1 implements JavascriptFunctionProvider {
    @Override @Nonnull public String getComponentKey() { return "phoneNumber"; }
    @Override @Nonnull public String getComponentVersion() { return "1.0.0"; }
    @Override @Nonnull public String getParentPluginKey() { return CorePlugin.PLUGIN_KEY; }
    @Override @Nonnull public String getName() { return "Telefonnummernfunktionen"; }
    @Override @Nonnull public String getAbstract() { return "Prüft, normalisiert und formatiert Telefonnummern mit Ländervorwahl."; }
    @Override @Nonnull public String getDescription() {
        return "isPossible prüft die Plausibilität, isValid zusätzlich bekannte Nummernpläne. Beide prüfen keine Erreichbarkeit. "
                + "normalize gibt plausible Nummern im E.164-Format zurück. format stellt sie international (Standard) oder national dar. "
                + "Die Formatierung ändert nicht den gespeicherten Wert. Leere oder nicht plausible Nummern und separate Nebenstellenzusätze ergeben null bei normalize/format und false bei den Prüfungen.";
    }
    @Override @Nonnull public String[] getMethodTypeDefinitions() {
        return new String[]{
                "isPossible(value: string | null): boolean;",
                "isValid(value: string | null): boolean;",
                "normalize(value: string | null): string | null;",
                "format(value: string | null): string | null;",
                "format(value: string | null, presentation: 'international' | 'national'): string | null;"
        };
    }
    @HostAccess.Export public boolean isPossible(@Nullable String value) { return PhoneNumberUtils.isPossiblePhoneNumber(value); }
    @HostAccess.Export public boolean isValid(@Nullable String value) { return PhoneNumberUtils.isValidPhoneNumber(value); }
    @HostAccess.Export @Nullable public String normalize(@Nullable String value) { return PhoneNumberUtils.normalizePossiblePhoneNumberToE164(value); }
    @HostAccess.Export @Nullable public String format(@Nullable String value) { return PhoneNumberUtils.formatPhoneNumber(value, false); }
    @HostAccess.Export @Nullable public String format(@Nullable String value, @Nullable String presentation) {
        var national = switch (presentation == null ? "" : presentation.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "", "international" -> false;
            case "national" -> true;
            default -> throw new IllegalArgumentException("Bitte wählen Sie international oder national als Darstellung aus.");
        };
        return PhoneNumberUtils.formatPhoneNumber(value, national);
    }
}
