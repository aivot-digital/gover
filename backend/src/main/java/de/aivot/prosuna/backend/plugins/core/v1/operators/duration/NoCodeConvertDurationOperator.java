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
        return getAbstract() + " Beispiel: 9000 Sekunden ergeben 2,5 Stunden oder 150 Minuten. Ein Tag entspricht immer 24 Stunden. Das Ergebnis wird auf acht Nachkommastellen kaufmännisch gerundet. Die bestehende Zahlenkonvertierung gilt auch für leere Werte; null und leere Zeichenfolgen ergeben 0.";
    }

    @Nonnull
    @Override
    public String[] getTags() {
        return new String[]{"Zeitdauer", "Sekunden", "Minuten", "Stunden", "Tage"};
    }

    @Nonnull
    @Override
    public String getHumanReadableTemplate() {
        return "rechne „#0“ Sekunden in „#1“ um";
    }

    @Nonnull
    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(NoCodeSignatur.of(NoCodeDataType.Number,
                new NoCodeParameter(NoCodeDataType.Number, "Zeitdauer in Sekunden", "Die gesamte Zeitdauer als Sekundenwert, zum Beispiel aus einem Zeitdauer-Feld."),
                DurationOperatorSupport.unitParameter()));
    }

    @Nonnull
    @Override
    public NoCodeResult performEvaluation(@Nonnull DerivedRuntimeElementData data, @Nonnull Object... args) throws NoCodeException {
        var seconds = DurationOperatorSupport.seconds(castToNumber(args[0]));
        var factor = DurationOperatorSupport.factor(castToString(args[1]));
        return new NoCodeResult(BigDecimal.valueOf(seconds).divide(factor, 8, RoundingMode.HALF_UP));
    }
}
