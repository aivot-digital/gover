package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.PrintableElement;
import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RichTextInputElement extends BaseInputElement<String> implements PrintableElement<String> {
    @Nullable
    private String content;
    @Nullable
    private Boolean reducedMode;

    public RichTextInputElement() {
        super(ElementType.RichTextInput);
    }

    @Override
    public String formatValue(Object value) {
        return switch (value) {
            case String sValue -> sValue;
            case null, default -> null;
        };
    }

    @Override
    public void performValidation(String value) throws ValidationException {
        if (value == null) {
            if (Boolean.TRUE.equals(getRequired())) {
                throw new RequiredValidationException(this);
            }
        }
    }

    @Nonnull
    public String toDisplayValue(@Nullable String value) {
        return value != null ? value : "Keine Angabe";
    }

    @Nonnull
    @Override
    public Boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
        if (referencedValue == null) {
            return switch (operator) {
                case Equals -> comparedValue == null;
                case NotEquals -> comparedValue != null;
                case Empty -> true;
                default -> false;
            };
        }

        if (operator == ConditionOperator.NotEmpty) {
            return true;
        }

        if (referencedValue instanceof String valA && comparedValue instanceof String valB) {
            return switch (operator) {
                case Equals -> valA.equals(valB);
                case NotEquals -> !valA.equals(valB);

                case EqualsIgnoreCase -> valA.equalsIgnoreCase(valB);
                case NotEqualsIgnoreCase -> !valA.equalsIgnoreCase(valB);

                case Includes -> valA.contains(valB);
                case NotIncludes -> !valA.contains(valB);

                case StartsWith -> valA.startsWith(valB);
                case NotStartsWith -> !valA.startsWith(valB);

                case EndsWith -> valA.endsWith(valB);
                case NotEndsWith -> !valA.endsWith(valB);

                case MatchesPattern -> {
                    try {
                        yield valA.matches("^" + valB + "$");
                    } catch (PatternSyntaxException ex) {
                        yield false;
                    }
                }
                case NotMatchesPattern -> {
                    try {
                        yield !valA.matches("^" + valB + "$");
                    } catch (PatternSyntaxException ex) {
                        yield false;
                    }
                }

                case IncludesPattern -> {
                    try {
                        var pattern = Pattern.compile(valB);
                        var matcher = pattern.matcher(valA);
                        yield matcher.find();
                    } catch (PatternSyntaxException ex) {
                        yield false;
                    }
                }
                case NotIncludesPattern -> {
                    try {
                        var pattern = Pattern.compile(valB);
                        var matcher = pattern.matcher(valA);
                        yield !matcher.find();
                    } catch (PatternSyntaxException ex) {
                        yield false;
                    }
                }

                default -> false;
            };
        }

        return false;
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RichTextInputElement that = (RichTextInputElement) o;
        return Objects.equals(content, that.content)
                && Objects.equals(reducedMode, that.reducedMode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), content, reducedMode);
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public String getContent() {
        return content;
    }

    public RichTextInputElement setContent(@Nullable String content) {
        this.content = content;
        return this;
    }

    @Nullable
    public Boolean getReducedMode() {
        return reducedMode;
    }

    public RichTextInputElement setReducedMode(@Nullable Boolean reducedMode) {
        this.reducedMode = reducedMode;
        return this;
    }

    // endregion
}
