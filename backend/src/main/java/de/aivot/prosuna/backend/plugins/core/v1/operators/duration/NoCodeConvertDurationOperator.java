package de.aivot.prosuna.backend.plugins.core.v1.operators.duration;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.*;
import jakarta.annotation.Nonnull;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NoCodeConvertDurationOperator extends NoCodeOperator {
    @Nonnull
    @Override
    public String getIdentifier() {
        return "convert-duration";
    }

    @Nonnull
    @Override
    public String getLabel() {
        return "Zeitdauer umrechnen";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Rechnet eine Zeitdauer in Sekunden in Gesamtsekunden, Gesamtminuten, Gesamtstunden oder Gesamttage um.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie diese Funktion?
                Rechnen Sie eine Zeitdauer in Sekunden in eine andere Einheit um, zum Beispiel die gesamte Bearbeitungszeit in Stunden.

                # Welche Werte werden benötigt?
                - **Zeitdauer in Sekunden:** Die gesamte Zeitdauer als Sekundenwert. Sie können diesen frei eingeben; es gibt keine festen Auswahlwerte.
                - **Einheit:** Wählen Sie **„Sekunden“**, **„Minuten“**, **„Stunden“** oder **„Tage“** als gewünschte Ergebniseinheit. Zusätzliche eigene Einträge sind in dieser Auswahl nicht möglich.

                Beide Eingaben können auch aus einer anderen Wertquelle oder einer Funktion stammen. Ein Zeitdauer-Feld liefert bereits einen Sekundenwert.

                # Welches Ergebnis erhalten Sie?
                Eine Zahl: die gesamte Zeitdauer in der gewählten Einheit. Bei Bedarf enthält das Ergebnis Nachkommastellen.

                # Beispiel im No-Code-Editor
                1. Wählen Sie für einen berechneten Zahlenwert **„Zeitdauer umrechnen“**.
                2. Legen Sie als **„Zeitdauer in Sekunden“** **9000** fest oder übernehmen Sie einen entsprechenden Wert aus einem Zeitdauer-Feld.
                3. Wählen Sie als **„Einheit“** **„Stunden“**.

                Das Ergebnis ist **2,5**: Die Zeitdauer beträgt insgesamt **2,5 Stunden**. Zugeklappt lesen Sie: **„9000 Sekunden umgerechnet in Stunden“**.

                # Was ist zu beachten?
                Es wird die gesamte Zeitdauer umgerechnet. **9000 Sekunden** ergeben **150 Minuten**, nicht nur die nach zwei vollen Stunden verbleibenden **30 Minuten**.

                Ein Tag entspricht immer **24 Stunden**. Ergebnisse werden auf acht Nachkommastellen gerundet. Fehlende oder leere Sekundenwerte werden als **0** behandelt. Der Sekundenwert muss eine ganze Zahl sein; auch **0.999999999** führt zu einer Fehlermeldung. Für einen bereits in Stunden vorliegenden Wert verwenden Sie zunächst **„Zeitdauer bilden“**.
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
        return "#0 Sekunden umgerechnet in #1";
    }

    @Nonnull
    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(NoCodeSignatur.of(NoCodeDataType.Number,
                DurationOperatorSupport.numberParameter("Zeitdauer in Sekunden", "Die gesamte Zeitdauer als Sekundenwert, zum Beispiel aus einem Zeitdauer-Feld.").withSentence("aus", "Sekunden"),
                DurationOperatorSupport.unitParameter().withSentence("in").withOptionsAsSentencePart()));
    }

    @Nonnull
    @Override
    public NoCodeResult performEvaluation(@Nonnull DerivedRuntimeElementData data, @Nonnull Object... args) throws NoCodeException {
        var seconds = DurationOperatorSupport.seconds(DurationOperatorSupport.amount(args[0], "Zeitdauer in Sekunden"));
        var factor = DurationOperatorSupport.factor(castToString(args[1]));
        return new NoCodeResult(BigDecimal.valueOf(seconds).divide(factor, 8, RoundingMode.HALF_UP));
    }

    @Nonnull
    @Override
    public String getSentenceLead() {
        return "ergibt die umgerechnete Zeitdauer";
    }
}
