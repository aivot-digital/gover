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

import java.util.regex.Pattern;

public class NoCodeRegexMatchOperator extends NoCodeOperator {
    public static final String OPERATOR_ID = "regex-match";

    @Override
    public String getIdentifier() {
        return OPERATOR_ID;
    }

    @Override
    public String getLabel() {
        return "Text passt vollständig zu Muster";
    }

    @Override
    public String getAbstract() {
        return "Prüft, ob der gesamte Text zu einem Suchmuster passt.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Prüfen Sie, ob der gesamte Text einer Regel entspricht, beispielsweise ob eine Postleitzahl genau fünf Ziffern enthält.

                # Welche Werte werden benötigt?
                **Text:** Der Text, den Sie prüfen beziehungsweise durchsuchen möchten. **Suchmuster:** Ein regulärer Ausdruck, also eine Regel für passende Textstellen. Beispielsweise steht `[0-9]+` für eine oder mehrere aufeinanderfolgende Ziffern.

                Sie können die Texte frei eingeben oder eine andere Wertquelle beziehungsweise Funktion verwenden. Es gibt keine vorgegebenen Auswahlwerte.

                # Welches Ergebnis erhalten Sie?
                **Wahr**, wenn der gesamte Text zum Suchmuster passt; andernfalls **Falsch**.

                # Beispiel im No-Code-Editor
                Wählen Sie in einer Bedingung **„Text passt vollständig zu Muster“**. Verwenden Sie für **Text** die Variable „Postleitzahl“, die `10115` liefert, und geben Sie als **Suchmuster** `[0-9]{5}` ein.

                Zugeklappt und in der Vorschau lesen Sie: `„Postleitzahl“ passt vollständig zum Muster „[0-9]{5}“`. Das Ergebnis ist **Wahr**. Der Text `PLZ 10115` ergibt dagegen **Falsch**.

                # Was ist zu beachten?
                Das Suchmuster ist ein regulärer Ausdruck nach Java-Syntax. Es muss den gesamten Text erfassen. Ein leeres Suchmuster passt nur zu einem leeren Text. Möchten Sie nur nach einer passenden Textstelle suchen, verwenden Sie **„Text enthält Muster“**.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt wahr";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Boolean,
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Text",
                                "Der zu überprüfende Text"
                        ).withSentence("wenn"),
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Suchmuster",
                                "Regel, zu der der gesamte Text passen muss, beispielsweise [0-9]{5} für genau fünf Ziffern."
                        ).withSentence("vollständig zum Muster", "passt")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "#0 passt vollständig zum Muster #1";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        if (args.length != 2) {
            throw new NoCodeWrongArgumentCountException(2, args.length);
        }

        String input = castToString(args[0]);
        String regex = castToString(args[1]);

        boolean matches;
        try {
            matches = Pattern.matches(regex, input);
        } catch (Exception e) {
            throw new NoCodeException("Das Suchmuster ist kein gültiger regulärer Ausdruck: " + regex);
        }
        return new NoCodeResult(matches);
    }
}
