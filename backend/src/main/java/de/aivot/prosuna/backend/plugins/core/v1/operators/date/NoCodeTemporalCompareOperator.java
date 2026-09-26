package de.aivot.prosuna.backend.plugins.core.v1.operators.date;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.enums.NoCodeDataType;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperator;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameter;
import de.aivot.prosuna.backend.nocode.models.NoCodeParameterOption;
import de.aivot.prosuna.backend.nocode.models.NoCodeResult;
import de.aivot.prosuna.backend.nocode.models.NoCodeSignatur;
import jakarta.annotation.Nullable;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

public class NoCodeTemporalCompareOperator extends NoCodeOperator {
    public enum TemporalType {
        DATE(NoCodeDataType.Date, "date", "Datumswerte"),
        TIME(NoCodeDataType.Time, "time", "Uhrzeiten"),
        DATETIME(NoCodeDataType.DateTime, "datetime", "Zeitpunkte");

        private final NoCodeDataType dataType;
        private final String identifierPart;
        private final String label;

        TemporalType(NoCodeDataType dataType, String identifierPart, String label) {
            this.dataType = dataType;
            this.identifierPart = identifierPart;
            this.label = label;
        }
    }

    private static final String EQUAL = "gleich";
    private static final String NOT_EQUAL = "ungleich";
    private static final String BEFORE = "vor";
    private static final String BEFORE_OR_EQUAL = "vor-oder-gleich";
    private static final String AFTER = "nach";
    private static final String AFTER_OR_EQUAL = "nach-oder-gleich";

    private final TemporalType temporalType;

    public NoCodeTemporalCompareOperator(TemporalType temporalType) {
        this.temporalType = temporalType;
    }

    @Override
    public String getIdentifier() {
        return "compare-" + temporalType.identifierPart;
    }

    @Override
    public String getLabel() {
        return temporalType.label + " vergleichen";
    }

    @Override
    public String getAbstract() {
        return "Vergleicht zwei " + temporalType.label + " anhand ihrer fachlichen zeitlichen Bedeutung.";
    }

    @Override
    public String getDescription() {
        return switch (temporalType) {
            case DATE -> """
                    Vergleicht zwei Kalenderdaten ohne Uhrzeit oder Zeitzone.
                    Beide Werte müssen dieselbe Präzision (Tag, Monat oder Jahr) besitzen.
                    """;
            case TIME -> "Vergleicht zwei lokale Uhrzeiten ohne Datum oder Zeitzone.";
            case DATETIME -> "Vergleicht zwei absolute Zeitpunkte unabhängig von ihrer Offset-Darstellung.";
        };
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.Boolean,
                        new NoCodeParameter(
                                temporalType.dataType,
                                "Wert 1",
                                "Der linke Vergleichswert."
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Vergleich",
                                "Die auszuführende Vergleichsoperation.",
                                new NoCodeParameterOption("ist gleich", EQUAL),
                                new NoCodeParameterOption("ist ungleich", NOT_EQUAL),
                                new NoCodeParameterOption("ist davor", BEFORE),
                                new NoCodeParameterOption("ist davor oder gleich", BEFORE_OR_EQUAL),
                                new NoCodeParameterOption("ist danach", AFTER),
                                new NoCodeParameterOption("ist danach oder gleich", AFTER_OR_EQUAL)
                        ),
                        new NoCodeParameter(
                                temporalType.dataType,
                                "Wert 2",
                                "Der rechte Vergleichswert."
                        )
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return "vergleiche „#0“: „#1“ „#2“";
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var operation = castToString(args[1]).trim().toLowerCase(Locale.ROOT);
        if (!isSupportedOperation(operation)) {
            throw new NoCodeException("Ungültiger zeitlicher Vergleich: " + operation);
        }

        var comparison = switch (temporalType) {
            case DATE -> compareCalendarValues(
                    requireCalendarValue(args[0], invalidValue(args[0])),
                    requireCalendarValue(args[2], invalidValue(args[2]))
            );
            case TIME -> requireTime(args[0], invalidValue(args[0]))
                    .compareTo(requireTime(args[2], invalidValue(args[2])));
            case DATETIME -> requireDateTime(args[0], invalidValue(args[0])).toInstant()
                    .compareTo(requireDateTime(args[2], invalidValue(args[2])).toInstant());
        };

        var result = switch (operation) {
            case EQUAL -> comparison == 0;
            case NOT_EQUAL -> comparison != 0;
            case BEFORE -> comparison < 0;
            case BEFORE_OR_EQUAL -> comparison <= 0;
            case AFTER -> comparison > 0;
            case AFTER_OR_EQUAL -> comparison >= 0;
            default -> throw new NoCodeException("Ungültiger zeitlicher Vergleich: " + operation);
        };

        return new NoCodeResult(result);
    }

    private boolean isSupportedOperation(String operation) {
        return switch (operation) {
            case EQUAL, NOT_EQUAL, BEFORE, BEFORE_OR_EQUAL, AFTER, AFTER_OR_EQUAL -> true;
            default -> false;
        };
    }

    private int compareCalendarValues(
            TemporalAccessor left,
            TemporalAccessor right
    ) throws NoCodeException {
        // Cross-precision ordering is ambiguous: treating YearMonth or Year as their
        // first day would add calendar information that is absent from the source value.
        if (!left.getClass().equals(right.getClass())) {
            throw new NoCodeException(
                    "Datumswerte können nur mit derselben Präzision verglichen werden."
            );
        }

        return switch (left) {
            case LocalDate localDate -> localDate.compareTo((LocalDate) right);
            case YearMonth yearMonth -> yearMonth.compareTo((YearMonth) right);
            case Year year -> year.compareTo((Year) right);
            default -> throw new NoCodeException("Nicht unterstützter Datumswert.");
        };
    }

    private String invalidValue(Object value) {
        return "Ungültiger Wert für den Vergleich von " + temporalType.label + ": " + castToString(value);
    }
}
