package de.aivot.GoverBackend.plugins.core.v1.operators.text;

import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;
import de.aivot.GoverBackend.nocode.models.NoCodeSignatur;
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
        return "Aufteilen";
    }

    @Override
    public String getAbstract() {
        return "Teilt einen Text anhand eines Separators auf.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Aufteilen“** teilt einen Text anhand eines angegebenen Separators auf. \s
                Das Ergebnis ist eine Liste von Teilstrings, die durch das Aufteilen des Eingabetextes entstehen.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten einen Satz in einzelne Wörter aufteilen.
                
                Mit dem Operator **„Aufteilen“** wird diese Logik so formuliert: \s
                `Aufteilen Text Separator`
                
                Beispielwerte: \s
                - **Text:** "apfel,banane,orange" \s
                - **Separator:** ","
                
                **Ergebnis:**
                - **Rückgabewert:** ["apfel", "banane", "orange"]
                
                # Wahrheitswerte für den „Aufteilen“-Operator
                - **Text "a,b,c" Separator ","** → **["a", "b", "c"]** \s
                - **Text "hallo welt" Separator " "** → **["hallo", "welt"]** \s
                - **Text "eins|zwei|drei" Separator "|"** → **["eins", "zwei", "drei"]**
                
                # Wann verwenden Sie den Operator „Aufteilen“?
                Verwenden Sie **„Aufteilen“**, um einen Text in Teile zu zerlegen, basierend auf einem bestimmten Separator. \s
                Dieser Operator ist besonders nützlich beim Parsen von CSV-Daten, beim Tokenisieren von Sätzen oder beim Verarbeiten von getrennten Zeichenfolgen.
                """;
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
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Separator",
                                "Das Zeichen oder die Zeichenfolge, anhand derer der Text aufgeteilt werden soll."
                        )
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "teile „#0“ mit dem Separator „#1“ auf";
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
