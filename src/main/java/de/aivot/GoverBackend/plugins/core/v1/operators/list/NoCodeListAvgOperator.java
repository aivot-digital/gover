package de.aivot.GoverBackend.plugins.core.v1.operators.list;

import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;
import de.aivot.GoverBackend.nocode.models.NoCodeSignatur;
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
        return "Berechnet den Durchschnitt aller numerischen Werte innerhalb einer Liste.";
    }

    @Override
    public String getDescription() {
        return """
                ### **Operator: "Durchschnitt aus Liste"**
                
                # Beschreibung:
                Der Operator **„Durchschnitt aus Liste“** berechnet den Durchschnitt (Mittelwert) aller numerischen Werte innerhalb einer Liste. \s
                Er wird verwendet, um den Durchschnittswert einer Sammlung von Zahlen zu ermitteln.
                
                Die Berechnung erfolgt durch: \s
                **Summe der Werte in der Liste / Anzahl der Werte in der Liste**
                
                # Anwendungsbeispiel:
                Angenommen, Sie haben eine Liste mit Bewertungen: `[4, 5, 3, 4]`. \s
                Mit dem Operator **„Durchschnitt aus Liste“** können Sie den Durchschnitt berechnen:
                
                ```text
                Durchschnitt aus Liste Bewertungen
                ```
                
                **Ergebnis:** \s
                Die Summe der Werte beträgt: `4 + 5 + 3 + 4 = 16` \s
                Anzahl der Werte: `4` \s
                **Durchschnitt:** `16 / 4 = 4`
                
                **Weitere Beispiele:**
                - Liste der Werte: `[10, 20, 30]` → **Ergebnis:** `20` \s
                - Leere Liste: `[]` → **Ergebnis:** `0` (Standardwert, falls Liste leer ist)
                
                # Wann verwenden Sie den Operator „Durchschnitt aus Liste“?
                Verwenden Sie **„Durchschnitt aus Liste“**, wenn Sie:
                - Den Mittelwert von numerischen Werten berechnen möchten (z. B. Durchschnittsnote, Durchschnittsumsatz). \s
                - Analysen oder Statistiken benötigen, die Durchschnittswerte erfordern. \s
                - Daten aus einer Tabelle oder Liste zusammenfassen, um Trends oder Muster zu erkennen.
                
                Dieser Operator ist ideal für Berichte, Bewertungen oder analytische Auswertungen.
                """;
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
                        )
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "berechne den Durchschnitt von „#0“";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var list = castToList(args[0]);

        if (list.isEmpty()) {
            return new NoCodeResult(BigDecimal.ZERO);
        }

        var sum = BigDecimal.ZERO;
        for (var summand : list) {
            sum = sum.add(castToNumber(summand));
        }

        var avg = sum.divide(BigDecimal.valueOf(list.size()), RoundingMode.HALF_UP);

        return new NoCodeResult(avg);
    }
}
