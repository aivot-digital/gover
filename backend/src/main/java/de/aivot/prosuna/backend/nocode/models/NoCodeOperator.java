package de.aivot.prosuna.backend.nocode.models;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import de.aivot.prosuna.backend.nocode.exceptions.NoCodeWrongArgumentCountException;
import de.aivot.prosuna.backend.utils.ApplicationTimeZone;
import de.aivot.prosuna.backend.utils.IsoTimestampUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import tools.jackson.core.JacksonException;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Represents an operator in the NoCode language.
 */
public abstract class NoCodeOperator {
    private static final String NOT_A_BOOLEAN = "Der Wert ist weder wahr noch falsch. Verwenden Sie eine Bedingung oder einen Wahrheitswert.";
    private static final String NOT_A_LIST = "Der Wert ist keine Liste.";
    private static final Pattern DECIMAL_COMMA = Pattern.compile("^[+-]?\\d+,\\d+$");
    // Numbers with more integer or decimal digits are rejected before calculating with them.
    private static final int MAX_NUMBER_DIGITS = 100;
    private static final DateTimeFormatter LOCAL_TIME_SECONDS_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Returns the identifier of the operator.
     * This is used to reference the operator in the NoCode language.
     * The reference always consists of the package name where the operator is located and the identifier.
     *
     * @return the identifier of the operator
     */
    public abstract String getIdentifier();

    /**
     * Returns the label of the operator.
     * The label is used to display the operator in the UI.
     *
     * @return the label of the operator
     */
    public abstract String getLabel();

    /**
     * Returns the abstract of the operator.
     *
     * @return the abstract of the operator
     */
    public abstract String getAbstract();

    /**
     * Returns the description of the operator.
     * The description is used to provide additional information about the operator.
     *
     * @return the description of the operator
     */
    public abstract String getDescription();

    /**
     * Returns the tags of the operator.
     * Tags are used to categorize the operator and can be used for filtering in the UI.
     * If no tags are defined, an empty array is returned.
     *
     * @return the tags of the operator
     */
    public String[] getTags() {
        return new String[0];
    }

    /**
     * Returns a human-readable template of the operator.
     * This template will later be filled with the parameters of the operator to provide a better understanding of how the operator works.
     * The template contains placeholders for the parameters of the operator.
     * For example: "Add #0 to #1" where #0 and #1 are placeholders for the parameters.
     *
     * @return a human-readable template of the operator
     */
    @Nullable
    public String getHumanReadableTemplate() {
        return null;
    }

    /**
     * Returns the text that follows the operator in the editor and starts the sentence its parameters continue.
     * For example „ergibt wahr“, followed by the parameters „wenn A“ and „gleich B“.
     *
     * @return the text after the operator, or {@code null} for none
     */
    @Nullable
    public String getSentenceLead() {
        return null;
    }

    /**
     * Returns the list of parameters that the operator expects.
     *
     * @return the array of parameters that the operator expects
     */
    public abstract NoCodeSignatur[] getSignatures();

    @Nonnull
    public NoCodeResult evaluate(DerivedRuntimeElementData data, Object... args) throws NoCodeException {
        if (data == null) {
            throw new NullPointerException("Data is null. Needs to be at least an empty map");
        }

        if (args == null) {
            throw new NullPointerException("Arguments are null. Needs to be at least an empty array");
        }

        var signatures = getSignatures();
        if (signatures == null || signatures.length == 0) {
            throw new NoCodeWrongArgumentCountException(0, args.length);
        }

        var actualParametersLength = args.length;
        var someMatch = false;
        var minimumExpectedParametersLength = Integer.MAX_VALUE;
        for (var signature : signatures) {
            var expectedParametersLength = signature.parameters().length;
            minimumExpectedParametersLength = Math.min(minimumExpectedParametersLength, expectedParametersLength);
            if (acceptsArgumentCount(expectedParametersLength, actualParametersLength)) {
                someMatch = true;
                break;
            }
        }


        if (!someMatch) {
            throw new NoCodeWrongArgumentCountException(minimumExpectedParametersLength, actualParametersLength);
        }

        return performEvaluation(data, args);
    }

    /**
     * Evaluates the operator with the given arguments.
     * The arguments are passed as an array of objects.
     * The amount and types of the arguments must be checked during the evaluation.
     * Arguments are <strong>not</strong> limited to the specified types.
     * The result of the evaluation is returned as a {@link NoCodeResult}.
     * Please make sure, that the evaluate method makes up for type mismatches like receiving a string instead of a number, and it can handle these.
     *
     * @param data the data that is used existent in the context.
     * @param args the arguments that are passed to the operator.
     * @return the result of the evaluation.
     * @throws NoCodeException if an error occurs during the evaluation.
     */
    protected abstract NoCodeResult performEvaluation(DerivedRuntimeElementData data, Object... args) throws NoCodeException;

    /**
     * Returns the message that should be displayed when the operator is deprecated.
     * If the operator is not deprecated, this method should return null.
     *
     * @return the message that should be displayed when the operator is deprecated.
     */
    public String getDeprecatedMessage() {
        return null;
    }

    boolean acceptsArgumentCount(int declaredCount, int actualCount) {
        return actualCount == declaredCount || (supportsVariableArgumentCount() && actualCount > declaredCount);
    }

    /** Exposes the existing plugin hook as operator metadata. */
    public final boolean hasVariableArgumentCount() {
        return supportsVariableArgumentCount();
    }

    protected boolean supportsVariableArgumentCount() {
        return false;
    }

    /**
     * Casts a value to the type of the reference. Returns null if either value is missing or the value cannot be cast,
     * so it never equals the reference.
     */
    @Nullable
    public Object castToTypeOfReference(@Nullable Object referenceObject, @Nullable Object objectToCast) {
        if (referenceObject == null || objectToCast == null) {
            return null;
        }

        try {
            return castToTypeOfReferenceOrThrow(referenceObject, objectToCast);
        } catch (NoCodeException ignored) {
            return null;
        }
    }

    @Nullable
    private Object castToTypeOfReferenceOrThrow(@Nonnull Object referenceObject, @Nonnull Object objectToCast) throws NoCodeException {
        switch (referenceObject) {
            case String sReferenceObject -> {
                return castToString(objectToCast);
            }
            case Integer iReferenceObject -> {
                return castToNumber(objectToCast).intValue();
            }
            case Float fReferenceObject -> {
                return castToNumber(objectToCast).floatValue();
            }
            case Double dReferenceObject -> {
                return castToNumber(objectToCast).doubleValue();
            }
            case Long lReferenceObject -> {
                return castToNumber(objectToCast).longValue();
            }
            case BigDecimal bdReferenceObject -> {
                return castToNumber(objectToCast);
            }
            case Boolean bReferenceObject -> {
                return castToBoolean(objectToCast);
            }
            case List<?> lReferenceObject -> {
                return castToList(objectToCast);
            }
            case Map<?, ?> mReferenceObject -> {
                return castToMap(objectToCast);
            }
            case ZonedDateTime zReferenceObject -> {
                return castToDateTime(objectToCast);
            }
            case OffsetDateTime oReferenceObject -> {
                return castToDateTime(objectToCast).toOffsetDateTime();
            }
            case Instant iReferenceObject -> {
                return castToDateTime(objectToCast).toInstant();
            }
            case LocalDateTime lReferenceObject -> {
                return castToDateTime(objectToCast).toLocalDateTime();
            }
            case LocalDate dReferenceObject -> {
                return castToDate(objectToCast);
            }
            case YearMonth ymReferenceObject -> {
                return YearMonth.from(castToDate(objectToCast));
            }
            case Year yReferenceObject -> {
                return Year.from(castToDate(objectToCast));
            }
            case LocalTime tReferenceObject -> {
                return castToTime(objectToCast);
            }
            default -> {
                return null;
            }
        }
    }

    /**
     * Checks whether a value equals the reference after casting it to the type of the reference.
     * <p>
     * A number only equals another number or text that is a number, in either order, and is compared by its value.
     * {@link BigDecimal#equals} also compares the scale, number fields keep their configured decimal places, and the
     * no-code editor stores constants as text, which may also precede a number field. Other values keep the cast to
     * the type of the reference. Values that cannot be cast are never equal.
     */
    public boolean isEqualToReference(@Nullable Object referenceObject, @Nullable Object objectToCompare) {
        if (referenceObject instanceof Number || objectToCompare instanceof Number) {
            var referenceNumber = parseNumber(referenceObject);
            var comparedNumber = parseNumber(objectToCompare);
            return referenceNumber != null && comparedNumber != null && referenceNumber.compareTo(comparedNumber) == 0;
        }

        return Objects.equals(referenceObject, castToTypeOfReference(referenceObject, objectToCompare));
    }

    /**
     * The exact value of a number or of text that is a number, otherwise null. Text may use a decimal comma, as entered
     * in German text fields. The value is not rescaled, because text such as "1e999999999" would then expand to an
     * enormous number.
     */
    @Nullable
    public static BigDecimal parseNumber(@Nullable Object value) {
        return switch (value) {
            case Integer integerValue -> BigDecimal.valueOf(integerValue);
            case Long longValue -> BigDecimal.valueOf(longValue);
            case BigDecimal decimalValue -> decimalValue;
            case Double doubleValue when Double.isFinite(doubleValue) -> BigDecimal.valueOf(doubleValue);
            case Float floatValue when Float.isFinite(floatValue) -> new BigDecimal(floatValue.toString());
            case String text -> {
                var trimmed = text.trim();
                try {
                    yield new BigDecimal(DECIMAL_COMMA.matcher(trimmed).matches() ? trimmed.replace(',', '.') : trimmed);
                } catch (NumberFormatException ignored) {
                    yield null;
                }
            }
            case null, default -> null;
        };
    }

    /**
     * Reads a condition. A missing value and empty text are false. Otherwise only true and false are accepted, also as
     * English or German text, ignoring case and surrounding whitespace.
     */
    @Nonnull
    public static Boolean castToBoolean(@Nullable Object value) throws NoCodeException {
        return switch (value) {
            case null -> false;
            case Boolean booleanValue -> booleanValue;
            case String text -> switch (text.trim().toLowerCase(Locale.ROOT)) {
                case "", "false", "falsch" -> false;
                case "true", "wahr" -> true;
                default -> throw new NoCodeException(NOT_A_BOOLEAN);
            };
            default -> throw new NoCodeException(NOT_A_BOOLEAN);
        };
    }

    /**
     * Reads a number. A missing value and empty text are 0, so optional fields can be used in calculations. Otherwise
     * only numbers and text that is a number are accepted.
     */
    @Nonnull
    public BigDecimal castToNumber(@Nullable Object value) throws NoCodeException {
        if (value == null || value instanceof String text && text.isBlank()) {
            return BigDecimal.ZERO;
        }

        var number = parseNumber(value);
        if (number == null) {
            throw new NoCodeException("Der Wert ist keine Zahl. Verwenden Sie eine Zahl wie 1.5 oder 1,5.");
        }
        // Security: calculations with enormous exponents, e.g. from the text "1e999999999", would exhaust the memory.
        if (number.precision() - number.scale() > MAX_NUMBER_DIGITS || number.scale() > MAX_NUMBER_DIGITS) {
            throw new NoCodeException("Die Zahl ist zu groß oder hat zu viele Nachkommastellen.");
        }
        return number;
    }

    /** Dates and timestamps are stored and entered with four-digit years, so results must stay within them. */
    protected static void requireSupportedYear(int year, @Nonnull String invalidValueMessage) throws NoCodeException {
        if (year < 1 || year > 9999) {
            throw new NoCodeException(invalidValueMessage);
        }
    }

    protected int requireInteger(
            @Nullable Object value,
            @Nonnull String invalidValueMessage
    ) throws NoCodeException {
        try {
            // General no-code number casts intentionally provide permissive fallbacks.
            // Integer parameters must instead reject text lengths, fractions and overflow.
            var number = switch (value) {
                case BigDecimal decimal -> decimal;
                case Number numericValue -> new BigDecimal(numericValue.toString());
                case String stringValue -> new BigDecimal(stringValue.trim());
                case null, default -> null;
            };

            if (number != null) {
                return number.intValueExact();
            }
        } catch (ArithmeticException | NumberFormatException ignored) {
            // Convert all invalid numeric shapes into the operator's domain error below.
        }

        throw new NoCodeException(invalidValueMessage);
    }

    @Nonnull
    public String castToString(@Nullable Object value) {
        if (value == null) {
            return "";
        }

        return switch (value) {
            case String sValue -> sValue;
            case Integer iValue -> String.valueOf(iValue);
            case Float fValue -> String.valueOf(fValue);
            case Double dValue -> String.valueOf(dValue);
            case Long lValue -> String.valueOf(lValue);
            case BigDecimal bdValue -> bdValue.toString();
            case Boolean bValue -> bValue.toString();
            case List<?> lValue -> {
                try {
                    yield JsonMapperFactory.getInstance().writeValueAsString(lValue);
                } catch (JacksonException e) {
                    yield "";
                }
            }
            case Map<?, ?> mValue -> {
                try {
                    yield JsonMapperFactory.getInstance().writeValueAsString(mValue);
                } catch (JacksonException e) {
                    yield "";
                }
            }
            case Instant iValue -> IsoTimestampUtils.toOffsetString(iValue);
            case OffsetDateTime odtValue -> IsoTimestampUtils.toOffsetString(odtValue.toInstant());
            case LocalDate ldValue -> ldValue.toString();
            case YearMonth ymValue -> ymValue.toString();
            case Year yValue -> yValue.toString();
            case LocalTime ltValue -> formatLocalTime(ltValue);
            case LocalDateTime ldtValue -> {
                var resolved = resolveLocalDateTime(ldtValue);
                yield resolved == null
                        ? ""
                        : IsoTimestampUtils.toOffsetString(resolved.toInstant());
            }
            case ZonedDateTime zdtValue -> IsoTimestampUtils.toOffsetString(zdtValue.toInstant());
            default -> "";
        };
    }

    @Nullable
    private LocalTime parseTime(String value) {
        if (!value.matches("^\\d{2}:\\d{2}(?::\\d{2})?$")) {
            return null;
        }

        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    @Nonnull
    private static String formatLocalTime(@Nonnull LocalTime value) {
        // LocalTime.toString() omits zero seconds. No-code string casts use the same
        // stable HH:mm:ss representation as the HTTP and destination-payload contract.
        return value.withNano(0).format(LOCAL_TIME_SECONDS_FORMATTER);
    }

    @Nonnull
    public LocalDate castToDate(@Nullable Object value) throws NoCodeException {
        return requireDate(value, "Der Wert ist kein gültiges Datum.");
    }

    @Nonnull
    protected LocalDate requireDate(
            @Nullable Object value,
            @Nonnull String invalidValueMessage
    ) throws NoCodeException {
        var parsedValue = tryCastToDate(value);
        if (parsedValue == null) {
            throw new NoCodeException(invalidValueMessage);
        }
        return parsedValue;
    }

    @Nonnull
    protected TemporalAccessor requireCalendarValue(
            @Nullable Object value,
            @Nonnull String invalidValueMessage
    ) throws NoCodeException {
        var parsedValue = tryCastToCalendarValue(value);
        if (parsedValue == null) {
            throw new NoCodeException(invalidValueMessage);
        }
        return parsedValue;
    }

    @Nullable
    protected TemporalAccessor tryCastToCalendarValue(@Nullable Object value) {
        return switch (value) {
            case null -> null;
            case LocalDate localDate -> localDate;
            case YearMonth yearMonth -> yearMonth;
            case Year year -> year;
            case String stringValue -> {
                try {
                    yield LocalDate.parse(stringValue);
                } catch (DateTimeParseException ignored) {
                    try {
                        yield YearMonth.parse(stringValue);
                    } catch (DateTimeParseException ignoredMonth) {
                        try {
                            yield Year.parse(stringValue);
                        } catch (DateTimeParseException ignoredYear) {
                            yield null;
                        }
                    }
                }
            }
            default -> null;
        };
    }

    @Nullable
    private LocalDate tryCastToDate(@Nullable Object value) {
        // Generic reference casts require a complete LocalDate. Partial form dates use
        // their first representable day here, then castToReferenceType restores the
        // reference value's YearMonth or Year precision. Temporal operators use
        // requireCalendarValue instead so they never invent missing calendar components.
        return switch (value) {
            case null -> null;
            case LocalDate localDate -> localDate;
            case YearMonth yearMonth -> yearMonth.atDay(1);
            case Year year -> year.atDay(1);
            case LocalDateTime localDateTime -> localDateTime.toLocalDate();
            case Instant instant -> instant.atZone(ApplicationTimeZone.getZoneId()).toLocalDate();
            case OffsetDateTime offsetDateTime ->
                    offsetDateTime.toInstant().atZone(ApplicationTimeZone.getZoneId()).toLocalDate();
            case ZonedDateTime zonedDateTime ->
                    zonedDateTime.toInstant().atZone(ApplicationTimeZone.getZoneId()).toLocalDate();
            case String stringValue -> {
                try {
                    yield LocalDate.parse(stringValue);
                } catch (DateTimeParseException ignored) {
                    try {
                        yield YearMonth.parse(stringValue).atDay(1);
                    } catch (DateTimeParseException ignoredMonth) {
                        try {
                            yield Year.parse(stringValue).atDay(1);
                        } catch (DateTimeParseException ignoredYear) {
                            try {
                                yield IsoTimestampUtils.parseIsoInstant(stringValue)
                                        .atZone(ApplicationTimeZone.getZoneId())
                                        .toLocalDate();
                            } catch (DateTimeParseException ignoredInstant) {
                                yield null;
                            }
                        }
                    }
                }
            }
            default -> null;
        };
    }

    @Nonnull
    public LocalTime castToTime(@Nullable Object value) throws NoCodeException {
        return requireTime(value, "Der Wert ist keine gültige Uhrzeit.");
    }

    @Nonnull
    protected LocalTime requireTime(
            @Nullable Object value,
            @Nonnull String invalidValueMessage
    ) throws NoCodeException {
        var parsedValue = tryCastToTime(value);
        if (parsedValue == null) {
            throw new NoCodeException(invalidValueMessage);
        }
        return parsedValue;
    }

    @Nullable
    private LocalTime tryCastToTime(@Nullable Object value) {
        return switch (value) {
            case null -> null;
            case LocalTime localTime -> localTime.withNano(0);
            case LocalDateTime localDateTime -> localDateTime.toLocalTime().withNano(0);
            case Instant instant ->
                    instant.atZone(ApplicationTimeZone.getZoneId()).toLocalTime().withNano(0);
            case OffsetDateTime offsetDateTime ->
                    offsetDateTime.toInstant().atZone(ApplicationTimeZone.getZoneId()).toLocalTime().withNano(0);
            case ZonedDateTime zonedDateTime ->
                    zonedDateTime.toInstant().atZone(ApplicationTimeZone.getZoneId()).toLocalTime().withNano(0);
            case String stringValue -> {
                var time = parseTime(stringValue);
                if (time != null) {
                    yield time;
                }
                // Stored timestamps are read like timestamp values: as the time in the application time zone.
                try {
                    yield IsoTimestampUtils.parseIsoInstant(stringValue)
                            .atZone(ApplicationTimeZone.getZoneId())
                            .toLocalTime()
                            .withNano(0);
                } catch (DateTimeParseException ignored) {
                    yield null;
                }
            }
            default -> null;
        };
    }

    @Nonnull
    public ZonedDateTime castToDateTime(@Nullable Object value) throws NoCodeException {
        return requireDateTime(value, "Der Wert ist kein gültiger Zeitpunkt.");
    }

    @Nonnull
    protected ZonedDateTime requireDateTime(@Nullable Object value,
                                            @Nonnull String invalidValueMessage) throws NoCodeException {
        var parsedValue = tryCastToDateTime(value);
        if (parsedValue == null) {
            throw new NoCodeException(invalidValueMessage);
        }

        return parsedValue;
    }

    @Nullable
    private ZonedDateTime tryCastToDateTime(@Nullable Object value) {
        return switch (value) {
            case null -> null;
            case Instant iValue -> iValue.atZone(ApplicationTimeZone.getZoneId());
            case OffsetDateTime odtValue -> odtValue.toInstant().atZone(ApplicationTimeZone.getZoneId());
            case LocalDateTime ldtValue -> resolveLocalDateTime(ldtValue);
            case ZonedDateTime zdtValue -> zdtValue.withZoneSameInstant(ApplicationTimeZone.getZoneId());
            case String sValue -> {
                try {
                    yield IsoTimestampUtils.parseIsoInstant(sValue).atZone(ApplicationTimeZone.getZoneId());
                } catch (DateTimeParseException ignored) {
                    yield null;
                }
            }
            default -> null;
        };
    }

    @Nullable
    private ZonedDateTime resolveLocalDateTime(@Nonnull LocalDateTime value) {
        var zoneId = ApplicationTimeZone.getZoneId();
        var validOffsets = zoneId.getRules().getValidOffsets(value);
        // LocalDateTime.atZone would silently move a nonexistent DST-gap value
        // forward. No-code conversions treat that input as invalid instead.
        if (validOffsets.isEmpty()) {
            return null;
        }

        try {
            // ZoneRules lists the earlier offset first during a DST overlap.
            return value.atOffset(validOffsets.getFirst()).toInstant().atZone(zoneId);
        } catch (DateTimeException ignored) {
            return null;
        }
    }

    /**
     * Reads an object. A missing value and empty text are an empty object. A serialized JSON null is null, so it does
     * not equal an empty object.
     */
    @Nullable
    public Map<String, Object> castToMap(@Nullable Object value) throws NoCodeException {
        if (value instanceof String text && text.isBlank()) {
            return Map.of();
        }

        try {
            return readMap(value);
        } catch (JacksonException | ClassCastException | IllegalArgumentException e) {
            throw new NoCodeException("Der Wert ist kein Objekt.");
        }
    }

    @Nullable
    protected Map<String, Object> readMap(@Nullable Object value) {
        return switch (value) {
            case null -> Map.of();
            case Map<?, ?> mValue -> (Map<String, Object>) mValue;
            case String sValue -> JsonMapperFactory.getInstance().readValue(sValue, Map.class);
            default -> Objects.requireNonNullElse(JsonMapperFactory.getInstance().convertValue(value, Map.class), Map.of());
        };
    }

    /** List functions treat a missing list as empty, including a serialized JSON null. */
    @Nonnull
    protected List<Object> listOperandOrEmpty(@Nullable Object value) throws NoCodeException {
        return Objects.requireNonNullElse(castToList(value), List.of());
    }

    /**
     * Reads a list. A missing value and empty text are an empty list. A serialized JSON null is null, so it does not
     * equal an empty list.
     */
    @Nullable
    public List<Object> castToList(@Nullable Object value) throws NoCodeException {
        return switch (value) {
            case null -> List.of();
            case List<?> list -> (List<Object>) list;
            case String text when text.isBlank() -> List.of();
            case String text -> {
                try {
                    yield JsonMapperFactory.getInstance().readValue(text, List.class);
                } catch (JacksonException e) {
                    throw new NoCodeException(NOT_A_LIST);
                }
            }
            default -> throw new NoCodeException(NOT_A_LIST);
        };
    }
}
