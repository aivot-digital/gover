package de.aivot.prosuna.backend.plugins.core.v1.operators.duration;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.enums.DurationLargestUnit;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.*;
import jakarta.annotation.Nonnull;
import de.aivot.prosuna.backend.utils.DurationValueUtils;
import java.util.Locale;

public class NoCodeFormatDurationOperator extends NoCodeOperator {
    @Nonnull
    @Override
    public String getIdentifier() {
        return "format-duration";
    }

    @Nonnull
    @Override
    public String getLabel() {
        return "Zeitdauer formatieren";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Formatiert eine Zeitdauer in Sekunden als Text mit Stunden, Minuten und Sekunden, optional mit Tagen.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return getAbstract() + " Wählen Sie Stunden oder Tage als größte Einheit. Mit Stunden ergeben 99000 Sekunden „27 Stunden 30 Minuten“, mit Tagen „1 Tag 3 Stunden 30 Minuten“. Ein Tag entspricht immer 24 Stunden. Ein leerer Wert für die größte Einheit wird als Stunden interpretiert. Null Sekunden werden als „0 Sekunden“ ausgegeben. Die bestehende Zahlenkonvertierung gilt auch für leere Werte; null und leere Zeichenfolgen ergeben 0.";
    }

    @Nonnull
    @Override
    public String[] getTags() {
        return new String[]{"Zeitdauer", "Sekunden", "Minuten", "Stunden", "Tage"};
    }

    @Nonnull
    @Override
    public String getHumanReadableTemplate() {
        return "formatiere „#0“ Sekunden als Zeitdauer";
    }

    @Nonnull
    @Override
    public NoCodeSignatur[] getSignatures() {
        var duration = new NoCodeParameter(NoCodeDataType.Number, "Zeitdauer in Sekunden", "Die gesamte Zeitdauer als Sekundenwert, zum Beispiel aus einem Zeitdauer-Feld.");
        return NoCodeSignatur.of(
                NoCodeSignatur.of(NoCodeDataType.String, duration,
                        new NoCodeParameter(NoCodeDataType.String, "Größte Einheit", "Stunden oder Tage. Ein Tag entspricht immer 24 Stunden.",
                                new NoCodeParameterOption("Stunden", "hours"),
                                new NoCodeParameterOption("Tage", "days"))));
    }

    @Nonnull
    @Override
    public NoCodeResult performEvaluation(@Nonnull DerivedRuntimeElementData data, @Nonnull Object... args) throws NoCodeException {
        var seconds = DurationOperatorSupport.seconds(castToNumber(args[0]));
        var largestUnit = switch (castToString(args[1]).trim().toLowerCase(Locale.ROOT)) {
            case "", "hours" -> DurationLargestUnit.Hour;
            case "days" -> DurationLargestUnit.Day;
            default -> throw new NoCodeException("Bitte wählen Sie Stunden oder Tage als größte Einheit aus.");
        };
        return new NoCodeResult(DurationValueUtils.formatSeconds(seconds, false, largestUnit));
    }
}
