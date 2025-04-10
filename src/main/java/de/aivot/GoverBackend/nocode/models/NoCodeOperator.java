package de.aivot.GoverBackend.nocode.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeWrongArgumentCountException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Represents an operator in the NoCode language.
 */
public abstract class NoCodeOperator {
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
     * Returns the list of parameters that the operator expects.
     *
     * @return the list of parameters that the operator expects
     */
    public abstract NoCodeParameter[] getParameters();

    /**
     * Returns the type of the return value of the operator.
     *
     * @return the type of the return value of the operator
     */
    public abstract NoCodeDataType getReturnType();

    @Nonnull
    public NoCodeResult evaluate(ElementDerivationData data, Object... args) throws NoCodeException {
        if (data == null) {
            throw new NullPointerException("Data is null. Needs to be at least an empty map");
        }

        if (args == null) {
            throw new NullPointerException("Arguments are null. Needs to be at least an empty array");
        }

        var expectedParametersLength = getParameters().length;
        var actualParametersLength = args.length;

        if (actualParametersLength != expectedParametersLength) {
            throw new NoCodeWrongArgumentCountException(expectedParametersLength, actualParametersLength);
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
    protected abstract NoCodeResult performEvaluation(ElementDerivationData data, Object... args) throws NoCodeException;

    /**
     * Returns the message that should be displayed when the operator is deprecated.
     * If the operator is not deprecated, this method should return null.
     *
     * @return the message that should be displayed when the operator is deprecated.
     */
    public String getDeprecatedMessage() {
        return null;
    }

    @Nullable
    public Object castToTypeOfReference(@Nullable Object referenceObject, @Nullable Object objectToCast) {
        if (referenceObject == null || objectToCast == null) {
            return null;
        }

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
            default -> {
                return null;
            }
        }
    }

    @Nonnull
    public Boolean castToBoolean(@Nullable Object value) {
        if (value == null) {
            return false;
        }

        return switch (value) {
            case Boolean bValue -> bValue;
            case Integer iValue -> iValue != 0;
            case Float fValue -> fValue != 0;
            case Double dValue -> dValue != 0;
            case Long lValue -> lValue != 0;
            case BigDecimal bdValue -> bdValue.compareTo(BigDecimal.ZERO) != 0;
            case String sValue -> {
                if (sValue.isEmpty()) {
                    yield false;
                } else if (sValue.equalsIgnoreCase("false")) {
                    yield false;
                } else if (sValue.equalsIgnoreCase("falsch")) {
                    yield false;
                } else {
                    yield true;
                }
            }
            case List<?> lValue -> !lValue.isEmpty();
            case Map<?, ?> mValue -> !mValue.isEmpty();
            default -> false;
        };
    }

    @Nonnull
    public BigDecimal castToNumber(@Nullable Object value) {
        var res = switch (value) {
            case null -> BigDecimal.ZERO;
            case Integer iValue -> BigDecimal.valueOf(iValue);
            case Float fValue -> BigDecimal.valueOf(fValue);
            case Double dValue -> BigDecimal.valueOf(dValue);
            case Long lValue -> BigDecimal.valueOf(lValue);
            case BigDecimal bdValue -> bdValue;
            case String sValue -> {
                if (isDateTimeString(sValue)) {
                    var dateTime = castToDateTime(sValue);
                    yield BigDecimal.valueOf(dateTime.toEpochSecond());
                } else {
                    try {
                        yield new BigDecimal(sValue);
                    } catch (NumberFormatException e) {
                        yield BigDecimal.valueOf(sValue.length());
                    }
                }
            }
            case List<?> lValue -> BigDecimal.valueOf(lValue.size());
            case Map<?, ?> mValue -> BigDecimal.valueOf(mValue.size());
            case LocalDateTime ldtValue -> BigDecimal.valueOf(ldtValue.toEpochSecond(ZoneOffset.UTC));
            case ZonedDateTime zdtValue -> BigDecimal.valueOf(zdtValue.toEpochSecond());
            default -> BigDecimal.ZERO;
        };

        return res.setScale(8, RoundingMode.HALF_UP);
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
                    yield new ObjectMapper().writeValueAsString(lValue);
                } catch (JsonProcessingException e) {
                    yield "";
                }
            }
            case Map<?, ?> mValue -> {
                try {
                    yield new ObjectMapper().writeValueAsString(mValue);
                } catch (JsonProcessingException e) {
                    yield "";
                }
            }
            case LocalDateTime ldtValue -> ldtValue.format(DateTimeFormatter.ISO_DATE_TIME);
            case ZonedDateTime zdtValue -> zdtValue.format(DateTimeFormatter.ISO_DATE_TIME);
            default -> "";
        };
    }

    private boolean isDateTimeString(String sValue) {
        try {
            ZonedDateTime.parse(sValue);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Nonnull
    public ZonedDateTime castToDateTime(@Nullable Object value) {
        if (value == null) {
            return ZonedDateTime.now();
        }

        return switch (value) {
            case LocalDateTime ldtValue -> ZonedDateTime.of(ldtValue, ZoneOffset.UTC);
            case ZonedDateTime zdtValue -> zdtValue;
            case String sValue -> {
                try {
                    yield ZonedDateTime.parse(sValue);
                } catch (Exception e) {
                    yield ZonedDateTime.now();
                }
            }
            default -> ZonedDateTime.now();
        };
    }

    @Nonnull
    public Map<String, Object> castToMap(@Nullable Object value) {
        if (value == null) {
            return Map.of();
        }

        return switch (value) {
            case Map<?, ?> mValue -> (Map<String, Object>) mValue;
            case String sValue -> {
                try {
                    yield new ObjectMapper().readValue(sValue, Map.class);
                } catch (JsonProcessingException e) {
                    yield Map.of();
                }
            }
            default -> Map.of();
        };
    }

    @Nonnull
    public List<Object> castToList(@Nullable Object value) {
        if (value == null) {
            return List.of();
        }

        return switch (value) {
            case List<?> lValue -> (List<Object>) lValue;
            case String sValue -> {
                try {
                    yield new ObjectMapper().readValue(sValue, List.class);
                } catch (JsonProcessingException e) {
                    yield List.of();
                }
            }
            default -> List.of();
        };
    }
}
