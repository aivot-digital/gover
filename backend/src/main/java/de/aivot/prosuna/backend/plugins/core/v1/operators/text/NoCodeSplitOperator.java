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
                **Text:** Der aufzuteilende Text. **Trennzeichen:** Das Zeichen oder die Zeichenfolge zwischen den Textteilen, beispielsweise ein Komma.

                Sie können die Texte frei eingeben oder eine andere Wertquelle beziehungsweise Funktion verwenden. Es gibt keine vorgegebenen Auswahlwerte.

                # Welches Ergebnis erhalten Sie?
                Eine Liste mit den Textteilen in ihrer ursprünglichen Reihenfolge.

                # Beispiel im No-Code-Editor
                Wählen Sie **„Text aufteilen“**. Geben Sie als **Text** `A,B,C` und als **Trennzeichen** ein Komma `,` ein.

                Zugeklappt und in der Vorschau lesen Sie: `„A,B,C“ aufgeteilt an „,“`. Das Ergebnis ist `["A", "B", "C"]`. Verwenden Sie das Ergebnis beispielsweise als **Liste** in **„Anzahl der Listeneinträge“**, um `3` zu erhalten.

                # Was ist zu beachten?
                Das Trennzeichen wird wörtlich verwendet; Sonderzeichen wie Punkte haben keine besondere Bedeutung. Leerzeichen bleiben in den Textteilen erhalten. Leere Teile zwischen Trennzeichen bleiben erhalten, leere Teile am Textende werden entfernt. Ohne Trennzeichen erhalten Sie eine Liste mit dem gesamten Text als einzigem Eintrag.
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
                        ).withSentence(""),
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Trennzeichen",
                                "Zeichen oder Zeichenfolge, an der der Text aufgeteilt wird, beispielsweise ein Komma."
                        ).withSentence("aufgeteilt an")
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
