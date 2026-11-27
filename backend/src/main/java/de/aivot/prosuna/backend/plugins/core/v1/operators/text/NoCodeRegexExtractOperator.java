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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NoCodeRegexExtractOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "regex-extract";
    }

    @Override
    public String getLabel() {
        return "Treffer aus Text ermitteln";
    }

    @Override
    public String getAbstract() {
        return "Liest alle Textstellen aus, die zu einem Suchmuster passen.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Lesen Sie passende Textstellen aus einem Text aus, beispielsweise alle darin vorkommenden Zahlen.

                # Welche Werte werden benötigt?
                **Text:** Der Text, den Sie prüfen beziehungsweise durchsuchen möchten. **Suchmuster:** Ein regulärer Ausdruck, also eine Regel für passende Textstellen. Beispielsweise steht `[0-9]+` für eine oder mehrere aufeinanderfolgende Ziffern.

                Sie können die Texte frei eingeben oder eine andere Wertquelle beziehungsweise Funktion verwenden. Es gibt keine vorgegebenen Auswahlwerte.

                # Welches Ergebnis erhalten Sie?
                Eine Liste mit allen vollständigen Treffern in der Reihenfolge ihres Vorkommens. Ohne Treffer ist die Liste leer.

                # Beispiel im No-Code-Editor
                Wählen Sie **„Treffer aus Text ermitteln“**. Geben Sie als **Text** `Position 12 und 34` und als **Suchmuster** `[0-9]+` ein.

                Zugeklappt und in der Vorschau lesen Sie: `Treffer für das Muster „[0-9]+“ in „Position 12 und 34“`. Das Ergebnis ist `["12", "34"]`. Verwenden Sie das Ergebnis als **Liste** in **„Anzahl der Listeneinträge“**, um die zwei Treffer zu zählen.

                # Was ist zu beachten?
                Das Suchmuster ist ein regulärer Ausdruck nach Java-Syntax. Treffer überlappen sich nicht. Es wird jeweils der gesamte Treffer zurückgegeben, nicht nur eine eingeklammerte Gruppe. Ein Muster, das auch ohne Zeichen passt, kann leere Treffer erzeugen.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt alle Treffer";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.List,
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Text",
                                "Der Text, aus dem extrahiert werden soll."
                        ).withSentence("in"),
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Suchmuster",
                                "Regel für passende Textstellen, beispielsweise [0-9]+ für eine oder mehrere Ziffern."
                        ).withSentence("für das Muster")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "Treffer für das Muster #1 in #0";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        if (args.length != 2) {
            throw new NoCodeWrongArgumentCountException(2, args.length);
        }

        String input = castToString(args[0]);
        String regex = castToString(args[1]);

        Pattern pattern;
        try {
            pattern = Pattern.compile(regex);
        } catch (Exception e) {
            throw new NoCodeException("Das Suchmuster ist kein gültiger regulärer Ausdruck: " + regex);
        }
        Matcher matcher = pattern.matcher(input);

        List<String> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(matcher.group());
        }

        return new NoCodeResult(matches);
    }
}
