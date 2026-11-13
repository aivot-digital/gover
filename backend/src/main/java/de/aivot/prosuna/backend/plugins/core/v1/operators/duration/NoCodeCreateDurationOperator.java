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
        return getAbstract() + " Beispiel: 1,5 Stunden ergeben 5400 Sekunden. Ein Tag entspricht immer 24 Stunden. Das Ergebnis muss ganze, nicht negative Sekunden enthalten. Die bestehende Zahlenkonvertierung gilt auch für leere Werte; null und leere Zeichenfolgen ergeben 0.";
    }

    @Nonnull
    @Override
    public String[] getTags() {
        return new String[]{"Zeitdauer", "Sekunden", "Minuten", "Stunden", "Tage"};
    }

    @Nonnull
    @Override
    public String getHumanReadableTemplate() {
        return "bilde eine Zeitdauer aus „#0“ „#1“";
    }

    @Nonnull
    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(NoCodeSignatur.of(NoCodeDataType.Number,
                new NoCodeParameter(NoCodeDataType.Number, "Anzahl", "Die Anzahl in der gewählten Einheit. Verwenden Sie für Dezimalzahlen einen Punkt, zum Beispiel 1.5. Das Ergebnis muss ganze Sekunden enthalten."),
                DurationOperatorSupport.unitParameter()));
    }

    @Nonnull
    @Override
    public NoCodeResult performEvaluation(@Nonnull DerivedRuntimeElementData data, @Nonnull Object... args) throws NoCodeException {
        var amount = castToNumber(args[0]);
        var factor = DurationOperatorSupport.factor(castToString(args[1]));
        return new NoCodeResult(BigDecimal.valueOf(DurationOperatorSupport.seconds(amount.multiply(factor))));
    }
}
