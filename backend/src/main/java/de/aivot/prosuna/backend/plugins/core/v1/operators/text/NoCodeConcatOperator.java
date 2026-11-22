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

public class NoCodeConcatOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "concat";
    }

    @Override
    public String getLabel() {
        return "Werte zu Text zusammenfügen";
    }

    @Override
    public String getAbstract() {
        return "Fügt Werte ohne zusätzliche Trennzeichen zu einem Text zusammen.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Setzen Sie einen Text aus einzelnen Werten zusammen, beispielsweise eine Kennung aus einem festen Präfix und einem vorhandenen Wert.

                # Welche Werte werden benötigt?
                **Erster Wert** und **Zweiter Wert:** Die Werte, die hintereinander stehen sollen. Texte und Zahlen werden zu Text umgewandelt.

                Sie können die Werte frei eingeben oder eine andere Wertquelle beziehungsweise Funktion verwenden. Es gibt keine vorgegebenen Auswahlwerte.

                # Welches Ergebnis erhalten Sie?
                Ein Text, der die Werte in ihrer angegebenen Reihenfolge direkt aneinanderfügt.

                # Beispiel im No-Code-Editor
                Wählen Sie **„Werte zu Text zusammenfügen“**. Geben Sie als **Erster Wert** `AZ-` ein und verwenden Sie als **Zweiter Wert** die Variable „Kennung“, die `2026-17` liefert.

                Zugeklappt und in der Vorschau lesen Sie: `Text aus „AZ-“ gefolgt von „Kennung“`. Das Ergebnis ist `AZ-2026-17`. Für weitere Bausteine können Sie das Ergebnis in eine weitere Funktion **„Werte zu Text zusammenfügen“** einsetzen.

                # Was ist zu beachten?
                Leerzeichen und Trennzeichen werden nicht automatisch ergänzt. Bei selbst festgelegten Texten werden Leerzeichen am Anfang und Ende beim Verlassen des Eingabefeldes entfernt. Benötigen Sie dort Leerzeichen, übernehmen Sie den Text aus einer Wertquelle oder Funktion. Fehlende Werte werden als leerer Text eingefügt.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt den Text";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.String,
                        new NoCodeParameter(
                                NoCodeDataType.Runtime,
                                "Erster Wert",
                                "Der erste Wert des Ergebnistextes, beispielsweise ein festes Präfix wie AZ-."
                        ).withSentence("aus"),
                        new NoCodeParameter(
                                NoCodeDataType.Runtime,
                                "Zweiter Wert",
                                "Der Wert, der direkt auf den ersten Wert folgen soll."
                        ).withSentence("gefolgt von")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "Text aus #0 gefolgt von #1";
    }

    @Override
    protected boolean supportsVariableArgumentCount() {
        return true;
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        if (args.length < 2) {
            throw new NoCodeWrongArgumentCountException(2, args.length);
        }

        var builder = new StringBuilder();
        for (var arg : args) {
            builder.append(castToString(arg));
        }

        return new NoCodeResult(builder.toString());
    }
}
