package de.aivot.prosuna.backend.plugins.core.v1.operators.duration;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.*;
import jakarta.annotation.Nonnull;

import java.math.BigDecimal;

public class NoCodeCreateDurationOperator extends NoCodeOperator {
    @Nonnull
    @Override
    public String getIdentifier() {
        return "create-duration";
    }

    @Nonnull
    @Override
    public String getLabel() {
        return "Zeitdauer bilden";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Bildet aus einer Angabe in Sekunden, Minuten, Stunden oder Tagen eine Zeitdauer in Sekunden.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Bilden Sie eine Zeitdauer aus einer Anzahl von Sekunden, Minuten, Stunden oder Tagen, zum Beispiel für eine Bearbeitungszeit von anderthalb Stunden.

                # Welche Werte werden benötigt?
                - **Anzahl:** Wie viele Einheiten die Zeitdauer umfasst. Sie können die Zahl frei eingeben; es gibt keine festen Auswahlwerte.
                - **Einheit:** Wählen Sie **„Sekunden“**, **„Minuten“**, **„Stunden“** oder **„Tage“**. Zusätzliche eigene Einträge sind in dieser Auswahl nicht möglich.

                Beide Eingaben können auch aus einer anderen Wertquelle oder einer Funktion stammen.

                # Welches Ergebnis erhalten Sie?
                Eine Zahl: die gesamte Zeitdauer in Sekunden. Dieses Ergebnis können Sie in einem Zeitdauer-Feld verwenden oder mit **„Zeitdauer formatieren“** als Text ausgeben.

                # Beispiel im No-Code-Editor
                1. Wählen Sie für einen berechneten Zahlenwert **„Zeitdauer bilden“**.
                2. Legen Sie als **„Anzahl“** **1.5** fest und wählen Sie als **„Einheit“** **„Stunden“**.
                3. Verwenden Sie das Ergebnis beispielsweise als **„Zeitdauer in Sekunden“** von **„Zeitdauer formatieren“**.

                Das Ergebnis sind **5400 Sekunden**. Zugeklappt lesen Sie: **„1.5 Stunden als Zeitdauer in Sekunden“**.

                # Was ist zu beachten?
                Ein Tag entspricht immer **24 Stunden**. Die Funktion berechnet eine Zeitdauer und berücksichtigt weder Kalenderdaten noch Zeitumstellungen.

                Die Anzahl darf Nachkommastellen haben, sofern daraus ganze Sekunden entstehen. Fehlende oder leere Zahlenwerte werden als **0** behandelt. Für Dezimalzahlen in einer selbst festgelegten Eingabe verwenden Sie einen Punkt, zum Beispiel **1.5**.
                """;
    }

    @Nonnull
    @Override
    public String[] getTags() {
        return new String[]{"Zeitdauer", "Sekunden", "Minuten", "Stunden", "Tage"};
    }

    @Nonnull
    @Override
    public String getHumanReadableTemplate() {
        return "#0 #1 als Zeitdauer in Sekunden";
    }

    @Nonnull
    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(NoCodeSignatur.of(NoCodeDataType.Number,
                DurationOperatorSupport.numberParameter("Anzahl", "Die Anzahl in der gewählten Einheit, zum Beispiel 1.5 für anderthalb Stunden. Verwenden Sie bei Dezimalzahlen einen Punkt.").withSentence("aus"),
                DurationOperatorSupport.unitParameter().withSentence("").withQuantityParameterIndex(0)));
    }

    @Nonnull
    @Override
    public NoCodeResult performEvaluation(@Nonnull DerivedRuntimeElementData data, @Nonnull Object... args) throws NoCodeException {
        var amount = DurationOperatorSupport.amount(args[0], "Anzahl");
        var factor = DurationOperatorSupport.factor(castToString(args[1]));
        return new NoCodeResult(BigDecimal.valueOf(DurationOperatorSupport.seconds(amount.multiply(factor))));
    }

    @Nonnull
    @Override
    public String getSentenceLead() {
        return "ergibt die Zeitdauer in Sekunden";
    }
}
