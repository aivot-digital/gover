package de.aivot.prosuna.backend.plugins.core.v1.operators.text;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class NoCodeBase64Operator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "base64";
    }

    @Override
    public String getLabel() {
        return "Text als Base64 kodieren";
    }

    @Override
    public String getAbstract() {
        return "Kodiert einen Text mit UTF-8 als Base64.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Wandeln Sie einen Text in Base64 um, wenn eine Anbindung dieses Textformat erwartet.

                # Welche Werte werden benötigt?
                **Text:** Der zu kodierende Text.

                Sie können die Texte frei eingeben oder eine andere Wertquelle beziehungsweise Funktion verwenden. Es gibt keine vorgegebenen Auswahlwerte.

                # Welches Ergebnis erhalten Sie?
                Ein Text mit der Base64-Kodierung des Eingabetextes.

                # Beispiel im No-Code-Editor
                Wählen Sie **„Text als Base64 kodieren“** und geben Sie unter **Text** `Hallo` ein.

                Zugeklappt und in der Vorschau lesen Sie: `„Hallo“ als Base64`. Das Ergebnis ist `SGFsbG8=`. Verwenden Sie den Ergebnistext dort, wo die Anbindung einen Base64-kodierten Text benötigt.

                # Was ist zu beachten?
                Der Text wird mit der Zeichenkodierung UTF-8 verarbeitet, auch bei Umlauten. Ein fehlender oder leerer Text ergibt einen leeren Text. Base64 ist eine Kodierung, keine Verschlüsselung. Diese Funktion dekodiert keine Base64-Texte.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.String,
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Text",
                                "Der Text, der in Base64 konvertiert werden soll."
                        ).withSentence("", "als Base64")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "#0 als Base64";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        if (args.length != 1) {
            throw new NoCodeWrongArgumentCountException(1, args.length);
        }

        var input = castToString(args[0]);
        var encoded = Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));

        return new NoCodeResult(encoded);
    }
}
