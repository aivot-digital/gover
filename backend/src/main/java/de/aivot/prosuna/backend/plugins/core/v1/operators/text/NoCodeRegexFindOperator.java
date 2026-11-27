package de.aivot.prosuna.backend.plugins.core.v1.operators.text;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class NoCodeRegexFindOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "regex-find";
    }

    @Override
    public String getLabel() {
        return "Text enthält Muster";
    }

    @Override
    public String getAbstract() {
        return "Prüft, ob mindestens eine Textstelle zu einem Suchmuster passt.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Prüfen Sie, ob ein Text eine Textstelle enthält, die zu einem Suchmuster passt, beispielsweise eine vierstellige Jahreszahl.

                # Welche Werte werden benötigt?
                **Text:** Der Text, den Sie prüfen beziehungsweise durchsuchen möchten. **Suchmuster:** Ein regulärer Ausdruck, also eine Regel für passende Textstellen. Beispielsweise steht `[0-9]+` für eine oder mehrere aufeinanderfolgende Ziffern.

                Sie können die Texte frei eingeben oder eine andere Wertquelle beziehungsweise Funktion verwenden. Es gibt keine vorgegebenen Auswahlwerte.

                # Welches Ergebnis erhalten Sie?
                **Wahr**, wenn mindestens eine Textstelle zum Suchmuster passt; andernfalls **Falsch**.

                # Beispiel im No-Code-Editor
                Wählen Sie in einer Bedingung **„Text enthält Muster“**. Verwenden Sie für **Text** die Variable „Aktenzeichen“, die `AZ-2026-17` liefert, und geben Sie als **Suchmuster** `[0-9]{4}` ein.

                Zugeklappt und in der Vorschau lesen Sie: `„Aktenzeichen“ enthält einen Treffer für das Muster „[0-9]{4}“`. Das Ergebnis ist **Wahr**, weil `2026` zum Muster passt.

                # Was ist zu beachten?
                Das Suchmuster ist ein regulärer Ausdruck nach Java-Syntax. Sonderzeichen können eine besondere Bedeutung haben. Ein leeres Suchmuster findet auch in einem leeren Text einen Treffer. Für einen wörtlichen Suchtext verwenden Sie **„Text enthält“**; für die Prüfung des gesamten Textes **„Text passt vollständig zu Muster“**.
                """;
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Boolean,
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Text",
                                "Der zu durchsuchende Text."
                        ).withSentence("wenn der Text"),
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Suchmuster",
                                "Regel für passende Textstellen, beispielsweise [0-9]+ für eine oder mehrere Ziffern."
                        ).withSentence("einen Treffer für das Muster", "enthält")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "#0 enthält einen Treffer für das Muster #1";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var input = castToString(args[0]);
        var regex = castToString(args[1]);

        try {
            return new NoCodeResult(Pattern.compile(regex).matcher(input).find());
        } catch (PatternSyntaxException e) {
            throw new NoCodeException("Das Suchmuster ist kein gültiger regulärer Ausdruck: " + regex);
        }
    }

    @Nonnull
    @Override
    public String getSentenceLead() {
        return "ergibt wahr";
    }
}
