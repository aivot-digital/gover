package de.aivot.GoverBackend.nocode.operators.text;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;

import java.util.regex.Pattern;

public class NoCodeRegexMatchOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "regex-match";
    }

    @Override
    public String getLabel() {
        return "Regex-Übereinstimmung";
    }

    @Override
    public String getAbstract() {
        return "Prüft, ob ein Text mit einem regulären Ausdruck übereinstimmt";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Regex-Übereinstimmung“** prüft, ob ein Text mit einem regulären Ausdruck übereinstimmt. \s
                Das Ergebnis ist **wahr** (true), wenn der Text mit dem regulären Ausdruck übereinstimmt. \s
                Andernfalls ist das Ergebnis **falsch**.
                
                # Anwendungsbeispiel:
                Stellen Sie sich vor, Sie möchten überprüfen, ob eine Zeichenkette eine bestimmte Struktur hat, z.B. eine E-Mail-Adresse.
                
                Mit dem Operator **„Regex-Übereinstimmung“** wird diese Logik so formuliert: \s
                `Regex-Übereinstimmung Text Regulärer Ausdruck`
                
                Beispielwerte: \s
                - **Text:** "test@example.com" \s
                - **Regulärer Ausdruck:** "^[\\w.-]+@[\\w.-]+\\.\\w+$"
                
                **Ergebnis:**
                - **Rückgabewert:** wahr
                
                # Wahrheitswerte für den „Regex-Übereinstimmung“-Operator
                - **Text "abc123" Regulärer Ausdruck "^[a-z]+\\d+$"** → **wahr** \s
                - **Text "123abc" Regulärer Ausdruck "^[a-z]+\\d+$"** → **falsch** \s
                - **Text "test@example.com" Regulärer Ausdruck "^[\\w.-]+@[\\w.-]+\\.\\w+$"** → **wahr**
                
                # Wann verwenden Sie den Operator „Regex-Übereinstimmung“?
                Verwenden Sie **„Regex-Übereinstimmung“**, um zu prüfen, ob ein Text mit einem bestimmten Muster übereinstimmt. \s
                Dieser Operator ist besonders hilfreich bei der Validierung von Eingaben, wie z.B. E-Mail-Adressen, Telefonnummern oder anderen formatierten Daten.
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
                        "Regulärer Ausdruck"
                ),
        };
    }

    @Override
    public NoCodeDataType getReturnType() {
        return NoCodeDataType.Boolean;
    }

    @Override
    public NoCodeResult performEvaluation(ElementDerivationData data, Object... args) throws NoCodeException {
        if (args.length != 2) {
            throw new NoCodeWrongArgumentCountException(2, args.length);
        }

        String input = castToString(args[0]);
        String regex = castToString(args[1]);

        boolean matches = Pattern.matches(regex, input);
        return new NoCodeResult(NoCodeDataType.Boolean, matches);
    }
}