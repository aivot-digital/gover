package de.aivot.GoverBackend.nocode.operators.text;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;

import java.util.List;

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
    public NoCodeParameter[] getParameters() {
        return new NoCodeParameter[]{
                new NoCodeParameter(
                        NoCodeDataType.String,
                        "Text"
                ),
                new NoCodeParameter(
                        NoCodeDataType.String,
                        "Separator"
                ),
        };
    }

    @Override
    public NoCodeDataType getReturnType() {
        return NoCodeDataType.List;
    }

    @Override
    public NoCodeResult performEvaluation(ElementDerivationData data, Object... args) throws NoCodeException {
        if (args.length != 2) {
            throw new NoCodeWrongArgumentCountException(2, args.length);
        }

        var input = castToString(args[0]);
        var separator = castToString(args[1]);

        return new NoCodeResult(NoCodeDataType.List, List.of(input.split(separator)));
    }
}