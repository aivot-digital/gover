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

import java.util.List;
import java.util.regex.Pattern;

public class NoCodeSplitOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "split";
    }

    @Override
    public String getLabel() {
        return "Text aufteilen";
    }

    @Override
    public String getAbstract() {
        return "Teilt einen Text an einem festgelegten Trennzeichen in eine Liste auf.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?

                Teilen Sie einen Text in einzelne Teile auf, beispielsweise mehrere durch Kommas getrennte Kategorien.

                # Welche Werte werden benötigt?

                - **Text:** Der aufzuteilende Text.
                - **Trennzeichen:** Das Zeichen oder die Zeichenfolge zwischen den Textteilen, beispielsweise ein Komma.

                Sie können die Texte frei eingeben oder eine andere Wertquelle beziehungsweise Funktion verwenden. Es gibt keine vorgegebenen Auswahlwerte.

                # Welches Ergebnis erhalten Sie?

                Eine Liste mit den Textteilen in ihrer ursprünglichen Reihenfolge.

                # Beispiel im No-Code-Editor

                1. Wählen Sie **„Text aufteilen“**.
                2. Geben Sie bei **„Text“** `Max Mustermann` ein.
                3. Drücken Sie im Feld **„Trennzeichen“** einmal die Leertaste.

                Zugeklappt und in der Vorschau lesen Sie `„Max Mustermann“ aufgeteilt an einem Leerzeichen`. Das Ergebnis ist `["Max", "Mustermann"]`. Verwenden Sie diese Liste beispielsweise in **„Anzahl der Listeneinträge“**, um die beiden Wörter zu zählen.

                # Was ist zu beachten?

                Das Trennzeichen wird wörtlich verwendet. Sonderzeichen wie Punkte haben keine besondere Bedeutung. Leerzeichen am Anfang und Ende beider Eingaben zählen mit. Leerzeichen, die zum Trennzeichen gehören, werden beim Aufteilen entfernt. Leere Teile zwischen Trennzeichen bleiben erhalten, leere Teile am Textende werden entfernt. Ohne Trennzeichen erhalten Sie eine Liste mit dem gesamten Text als einzigem Eintrag.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt die Textteile aus";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.List,
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Text",
                                "Der Text, der aufgeteilt werden soll."
                        ).withSentence("").withPreservedWhitespace(),
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Trennzeichen",
                                "Zeichen oder Zeichenfolge, an der der Text aufgeteilt wird, beispielsweise ein Leerzeichen oder ein Komma."
                        ).withSentence("aufgeteilt an").withPreservedWhitespace()
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "#0 aufgeteilt an #1";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        if (args.length != 2) {
            throw new NoCodeWrongArgumentCountException(2, args.length);
        }

        var input = castToString(args[0]);
        var separator = castToString(args[1]);

        if (separator.isEmpty()) {
            return new NoCodeResult(List.of(input));
        }

        return new NoCodeResult(List.of(input.split(Pattern.quote(separator))));
    }
}
