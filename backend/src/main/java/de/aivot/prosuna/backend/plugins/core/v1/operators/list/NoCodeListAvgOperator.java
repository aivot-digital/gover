package de.aivot.prosuna.backend.plugins.core.v1.operators.list;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NoCodeListAvgOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "list-avg";
    }

    @Override
    public String getLabel() {
        return "Durchschnitt einer Liste";
    }

    @Override
    public String getAbstract() {
        return "Berechnet den arithmetischen Mittelwert der Werte einer Liste.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Berechnen Sie den Durchschnitt mehrerer Zahlen, beispielsweise die durchschnittliche Bewertung aus einer Liste.

                # Welche Werte werden benötigt?
                **Liste:** Die Zahlen, deren Durchschnitt Sie benötigen.

                Die Liste können Sie selbst als JSON-Liste eingeben, beispielsweise `[1, 2, 3]`. Es gibt keine vorgegebenen Auswahlwerte. Alternativ verwenden Sie eine andere Wertquelle oder eine Funktion, die eine Liste liefert.

                # Welches Ergebnis erhalten Sie?
                Eine Zahl: die Summe der Werte geteilt durch die Anzahl der Listeneinträge. Eine leere Liste ergibt `0`.

                # Beispiel im No-Code-Editor
                Wählen Sie **„Durchschnitt einer Liste“**. Verwenden Sie für **Liste** die Variable „Bewertungen“, die `[4, 5, 3, 4]` liefert.

                Zugeklappt und in der Vorschau lesen Sie: `Durchschnitt von „Bewertungen“`. Das Ergebnis ist `4`. Sie können die Funktion beispielsweise als ersten Wert von **„Größer als“** verwenden und mit `3` vergleichen.

                # Was ist zu beachten?
                Das Ergebnis wird auf acht Nachkommastellen gerundet. Jeder Eintrag geht in die Berechnung ein; Einträge ohne Wert und leerer Text zählen als `0`. Verwenden Sie Zahlen oder Zahlen als Text wie `1.5` oder `1,5`; andere Einträge führen zu einer Fehlermeldung. Fehlt die Liste oder ist sie leerer Text oder `null`, gilt sie als leer. Werte, die keine Liste sind, etwa Zahlen, Objekte oder Text, der keine JSON-Liste ist, führen zu einer Fehlermeldung.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt den Durchschnitt";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Number,
                        new NoCodeParameter(
                                NoCodeDataType.List,
                                "Liste",
                                "Die Liste der numerischen Werte, deren Durchschnitt berechnet werden soll."
                        ).withSentence("von")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "Durchschnitt von #0";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var list = listOperandOrEmpty(args[0]);

        if (list.isEmpty()) {
            return new NoCodeResult(BigDecimal.ZERO);
        }

        var sum = BigDecimal.ZERO;
        for (var summand : list) {
            sum = sum.add(castToNumber(summand));
        }

        // Results keep the eight decimal places that the inputs were rounded to before.
        var avg = sum.divide(BigDecimal.valueOf(list.size()), 8, RoundingMode.HALF_UP);

        return new NoCodeResult(avg);
    }
}
