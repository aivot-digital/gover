package de.aivot.prosuna.backend.plugins.core.v1.operators.list;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

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
        return "Addiert die Werte einer Liste zu einer Summe.";
    }

    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Berechnen Sie eine Gesamtsumme, beispielsweise aus den Beträgen mehrerer Positionen.

                # Welche Werte werden benötigt?
                **Liste:** Die Zahlen, die addiert werden sollen.

                Die Liste können Sie selbst als JSON-Liste eingeben, beispielsweise `[1, 2, 3]`. Es gibt keine vorgegebenen Auswahlwerte. Alternativ verwenden Sie eine andere Wertquelle oder eine Funktion, die eine Liste liefert.

                # Welches Ergebnis erhalten Sie?
                Eine Zahl mit der Summe der Listeneinträge. Eine leere Liste ergibt `0`.

                # Beispiel im No-Code-Editor
                Wählen Sie **„Summe einer Liste“**. Verwenden Sie für **Liste** die Variable „Beträge“, die `[100, 200, 50]` liefert.

                Zugeklappt und in der Vorschau lesen Sie: `Summe von „Beträge“`. Das Ergebnis ist `350`. Um nur Beträge aus einer Tabelle zu addieren, können Sie zuvor mit **„Feldwerte aus Liste“** die betreffende Spalte ermitteln.

                # Was ist zu beachten?
                Jeder Eintrag geht in die Summe ein; Einträge ohne Wert und leerer Text zählen als `0`. Verwenden Sie Zahlen oder Zahlen als Text wie `1.5` oder `1,5`; andere Einträge führen zu einer Fehlermeldung. Fehlt die Liste oder ist sie leerer Text oder `null`, gilt sie als leer. Werte, die keine Liste sind, etwa Zahlen, Objekte oder Text, der keine JSON-Liste ist, führen zu einer Fehlermeldung.
                """;
    }

    @Override
    public String getSentenceLead() {
        return "ergibt die Summe";
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Number,
                        new NoCodeParameter(
                                NoCodeDataType.List,
                                "Liste",
                                "Die Liste der numerischen Werte, deren Summe berechnet werden soll."
                        ).withSentence("von")
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "Summe von #0";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        if (args.length != 1) {
            throw new NoCodeWrongArgumentCountException(1, args.length);
        }

        var list = listOperandOrEmpty(args[0]);

        if (list.isEmpty()) {
            return new NoCodeResult(BigDecimal.ZERO);
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (var summand : list) {
            sum = sum.add(castToNumber(summand));
        }

        return new NoCodeResult(sum);
    }
}
