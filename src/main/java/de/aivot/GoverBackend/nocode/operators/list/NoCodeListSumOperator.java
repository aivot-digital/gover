package de.aivot.GoverBackend.nocode.operators.list;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.GoverBackend.nocode.models.NoCodeOperator;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;

import java.math.BigDecimal;

public class NoCodeListSumOperator extends NoCodeOperator {
    @Override
    public String getIdentifier() {
        return "list-sum";
    }

    @Override
    public String getLabel() {
        return "Summe einer Liste";
    }

    @Override
    public String getAbstract() {
        return "Berechnet die Summe aller numerischen Werte innerhalb einer Liste.";
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                Der Operator **„Summe aus Liste“** berechnet die Summe aller numerischen Werte innerhalb einer Liste. \s
                Er wird verwendet, um schnell und effizient die Gesamtmenge oder den Gesamtwert aus einer Sammlung von Zahlen zu ermitteln.
                
                # Anwendungsbeispiel:
                Angenommen, Sie haben eine Liste mit Umsätzen aus mehreren Transaktionen: `[100, 200, 50]`. \s
                Mit dem Operator **„Summe aus Liste“** können Sie die Gesamtumsätze berechnen:
                
                ```text
                Summe aus Liste Umsätze
                ```
                
                **Ergebnis:** \s
                Die Summe der Werte in der Liste beträgt: `100 + 200 + 50 = 350`.
                
                **Weitere Beispiele:**
                - Liste der Werte: `[1, 2, 3, 4]` → **Ergebnis:** `10` \s
                - Leere Liste: `[]` → **Ergebnis:** `0` \s
                
                # Wann verwenden Sie den Operator „Summe aus Liste“?
                Verwenden Sie **„Summe aus Liste“**, wenn Sie:
                - Die Gesamtsumme einer Sammlung von Zahlen berechnen möchten (z. B. Umsätze, Preise, Mengen). \s
                - Daten aus einer Tabelle oder Liste aggregieren, z. B. die Summe aller Werte in einer bestimmten Spalte. \s
                - Statistiken oder Berichte erstellen, die Summen erfordern. \s
                
                Dieser Operator ist besonders nützlich für Analysen, Finanzberechnungen oder Berichte.
                """;
    }

    @Override
    public NoCodeParameter[] getParameters() {
        return new NoCodeParameter[]{
                new NoCodeParameter(
                        NoCodeDataType.List,
                        "Liste"
                ),
        };
    }

    @Override
    public NoCodeDataType getReturnType() {
        return NoCodeDataType.Number;
    }

    @Override
    public NoCodeResult performEvaluation(ElementDerivationData data, Object... args) throws NoCodeException {
        if (args.length != 1) {
            throw new NoCodeWrongArgumentCountException(1, args.length);
        }

        var list = castToList(args[0]);

        if (list.isEmpty()) {
            return new NoCodeResult(NoCodeDataType.Number, BigDecimal.ZERO);
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (var summand : list) {
            sum = sum.add(castToNumber(summand));
        }

        return new NoCodeResult(NoCodeDataType.Any, sum);
    }
}
