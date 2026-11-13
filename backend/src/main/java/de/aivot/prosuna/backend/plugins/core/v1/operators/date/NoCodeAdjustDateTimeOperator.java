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

import java.time.DateTimeException;
import java.util.Locale;

import static de.aivot.prosuna.backend.plugins.core.v1.operators.date.NoCodeAddToDateOperator.DAYS_UNIT;
import static de.aivot.prosuna.backend.plugins.core.v1.operators.date.NoCodeAddToDateOperator.MONTHS_UNIT;
import static de.aivot.prosuna.backend.plugins.core.v1.operators.date.NoCodeAddToDateOperator.WEEKS_UNIT;
import static de.aivot.prosuna.backend.plugins.core.v1.operators.date.NoCodeAddToDateOperator.YEARS_UNIT;

/**
 * Date-time counterpart of add-to-date and subtract-from-date. Calendar units are applied in the application time
 * zone, so "1 day" keeps the local wall-clock time across daylight-saving changes, like the legacy relative
 * date-time conditions.
 */
public class NoCodeAdjustDateTimeOperator extends NoCodeOperator {
    public enum Direction {
        ADD,
        SUBTRACT,
    }

    private final Direction direction;

    public NoCodeAdjustDateTimeOperator(Direction direction) {
        this.direction = direction;
    }

    @Override
    public String getIdentifier() {
        return switch (direction) {
            case ADD -> "add-to-datetime";
            case SUBTRACT -> "subtract-from-datetime";
        };
    }

    @Override
    public String getLabel() {
        return switch (direction) {
            case ADD -> "Zu Zeitpunkt hinzufügen";
            case SUBTRACT -> "Von Zeitpunkt abziehen";
        };
    }

    @Override
    public String getAbstract() {
        return switch (direction) {
            case ADD -> "Fügt einem Zeitpunkt eine Anzahl von Tagen, Wochen, Monaten oder Jahren hinzu.";
            case SUBTRACT -> "Zieht von einem Zeitpunkt eine Anzahl von Tagen, Wochen, Monaten oder Jahren ab.";
        };
    }

    @Override
    public String getDescription() {
        return """
                # Beschreibung:
                %s \s
                Die Uhrzeit bleibt dabei erhalten. Gerechnet wird in der Zeitzone der Anwendung.

                # Anwendungsbeispiel:
                Um zu prüfen, ob ein Zeitpunkt mindestens 18 Jahre zurückliegt, vergleichen Sie ihn mit \s
                **„Von Zeitpunkt abziehen“** (Aktueller Zeitpunkt, 18, Jahre) über **„Zeitpunkte vergleichen“** mit „ist davor oder gleich“.
                """.formatted(getAbstract());
    }

    @Override
    public NoCodeSignatur[] getSignatures() {
        return NoCodeSignatur.of(
                NoCodeSignatur.of(
                        NoCodeDataType.DateTime,
                        new NoCodeParameter(
                                NoCodeDataType.DateTime,
                                "Zeitpunkt",
                                "Der Ausgangszeitpunkt."
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.Number,
                                "Anzahl",
                                "Die Anzahl der Einheiten."
                        ),
                        new NoCodeParameter(
                                NoCodeDataType.String,
                                "Einheit",
                                "Die Einheit der Zeitspanne (Tage, Wochen, Monate, Jahre).",
                                new NoCodeParameterOption("Tage", DAYS_UNIT),
                                new NoCodeParameterOption("Wochen", WEEKS_UNIT),
                                new NoCodeParameterOption("Monate", MONTHS_UNIT),
                                new NoCodeParameterOption("Jahre", YEARS_UNIT)
                        )
                )
        );
    }

    @Nullable
    @Override
    public String getHumanReadableTemplate() {
        return switch (direction) {
            case ADD -> "füge „#1“ „#2“ zu „#0“ hinzu";
            case SUBTRACT -> "ziehe „#1“ „#2“ von „#0“ ab";
        };
    }

    @Override
    public NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        var dateTime = requireDateTime(args[0], "Ungültiger Zeitpunkt: " + castToString(args[0]));
        var amount = requireInteger(args[1], "Die Anzahl muss eine ganze Zahl sein.");
        var unit = castToString(args[2]).trim().toLowerCase(Locale.ROOT);
        // Widen before negation so Integer.MIN_VALUE remains representable.
        var signedAmount = direction == Direction.ADD ? (long) amount : -(long) amount;

        try {
            var adjusted = switch (unit) {
                case DAYS_UNIT -> dateTime.plusDays(signedAmount);
                case WEEKS_UNIT -> dateTime.plusWeeks(signedAmount);
                case MONTHS_UNIT -> dateTime.plusMonths(signedAmount);
                case YEARS_UNIT -> dateTime.plusYears(signedAmount);
                default -> throw new NoCodeException("Ungültige Einheit: " + unit);
            };
            return new NoCodeResult(adjusted.toInstant());
        } catch (DateTimeException | ArithmeticException exception) {
            throw new NoCodeException("Der Zeitpunkt liegt außerhalb des unterstützten Bereichs.");
        }
    }
}
