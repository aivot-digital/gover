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
        return "Fügt zwei oder mehr Werte in der angegebenen Reihenfolge zu einem Text zusammen.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Setzen Sie einen Text aus einzelnen Werten zusammen, beispielsweise eine Kennung mit einem festen Präfix und einem Zusatz.

                # Welche Werte werden benötigt?
                Sie benötigen mindestens zwei Werte in der gewünschten Reihenfolge. Sie können jeden Wert frei eingeben oder über „Andere Wertquelle verwenden …“ aus einer Wertquelle oder Funktion übernehmen. Es gibt keine vorgegebenen Auswahlwerte.

                # Welches Ergebnis erhalten Sie?
                Ein Text, der die Werte in ihrer angegebenen Reihenfolge direkt aneinanderfügt.

                # Beispiel im No-Code-Editor
                1. Wählen Sie **„Werte zu Text zusammenfügen“**.
                2. Geben Sie bei „Wert 1“ `AZ-` ein. Verwenden Sie bei „Wert 2“ die Variable „Kennung“, die `2026-17` liefert.
                3. Wählen Sie „Weiteren Wert hinzufügen“ und geben Sie bei „Wert 3“ `-A` ein.

                Zugeklappt und in der Vorschau lesen Sie `Text aus „AZ-“, „Kennung“ und „-A“`. Das Ergebnis ist `AZ-2026-17-A`.

                # Was ist zu beachten?
                Leerzeichen am Anfang und Ende jedes Textbausteins bleiben erhalten. Soll zwischen zwei Werten ein Leerzeichen stehen, können Sie es am Ende des ersten Wertes oder als eigenen Textbaustein eingeben. Fehlende Werte werden als leerer Text eingefügt.
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
                                "Wert 1",
                                "Der erste Wert des Ergebnistextes, beispielsweise ein festes Präfix wie AZ-."
                        ).withSentence("aus").withPreservedWhitespace(),
                        new NoCodeParameter(
                                NoCodeDataType.Runtime,
                                "Wert 2",
                                "Der Wert, der direkt an den bisherigen Text angefügt wird."
                        ).withSentence("gefolgt von").withPreservedWhitespace()
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
