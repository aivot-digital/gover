package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class TextField extends BaseInputElement<String> {
    @Nullable
    private String placeholder;
    @Nullable
    private String autocomplete;
    @Nullable
    private Boolean isMultiline;
    @Nullable
    private Integer maxCharacters;
    @Nullable
    private Integer minCharacters;
    @Nullable
    private TextPattern pattern;
    @Nullable
    private List<String> suggestions;

    public TextField() {
        super(ElementType.Text);
    }

    @Override
    public String formatValue(Object value) {
        return switch (value) {
            case null -> null;
            case String sValue -> sValue;
            case Number nValue -> NumberField
                    .formatGermanNumber(nValue);
            default -> null;
        };
    }

    @Override
    public void performValidation(String value) throws ValidationException {
        if (maxCharacters != null && maxCharacters > 0 && value.length() > maxCharacters) {
            throw new ValidationException(this, "Zu viele Zeichen. Maximal " + maxCharacters + " Zeichen erlaubt.");
        }

        if (minCharacters != null && minCharacters > 0 && value.length() < minCharacters) {
            throw new ValidationException(this, "Zu wenige Zeichen. Mindestens " + minCharacters + " Zeichen erforderlich.");
        }

        if (pattern != null && pattern.getRegex() != null && !pattern.getRegex().isEmpty()) {
            try {
                if (!value.matches(pattern.getRegex())) {
                    throw new ValidationException(this, pattern.getMessage() != null ? pattern.getMessage() : "Das Format der Eingabe ist ungültig.");
                }
            } catch (PatternSyntaxException ex) {
                throw new ValidationException(this, "Fehlerhaftes Regex-Pattern: " + ex.getMessage());
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
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        if (!super.equals(object)) return false;

        TextField textField = (TextField) object;
        return Objects.equals(placeholder, textField.placeholder) && Objects.equals(autocomplete, textField.autocomplete) && Objects.equals(isMultiline, textField.isMultiline) && Objects.equals(maxCharacters, textField.maxCharacters) && Objects.equals(minCharacters, textField.minCharacters) && Objects.equals(pattern, textField.pattern) && Objects.equals(suggestions, textField.suggestions);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(placeholder);
        result = 31 * result + Objects.hashCode(autocomplete);
        result = 31 * result + Objects.hashCode(isMultiline);
        result = 31 * result + Objects.hashCode(maxCharacters);
        result = 31 * result + Objects.hashCode(minCharacters);
        result = 31 * result + Objects.hashCode(pattern);
        result = 31 * result + Objects.hashCode(suggestions);
        return result;
    }


    // endregion

    // region Getters & Setters

    @Nullable
    public String getPlaceholder() {
        return placeholder;
    }

    public TextField setPlaceholder(@Nullable String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    @Nullable
    public String getAutocomplete() {
        return autocomplete;
    }

    public TextField setAutocomplete(@Nullable String autocomplete) {
        this.autocomplete = autocomplete;
        return this;
    }

    @Nullable
    public Boolean getIsMultiline() {
        return isMultiline;
    }

    public TextField setIsMultiline(@Nullable Boolean multiline) {
        isMultiline = multiline;
        return this;
    }

    @Nullable
    public Integer getMaxCharacters() {
        return maxCharacters;
    }

    public TextField setMaxCharacters(@Nullable Integer maxCharacters) {
        this.maxCharacters = maxCharacters;
        return this;
    }

    @Nullable
    public Integer getMinCharacters() {
        return minCharacters;
    }

    public TextField setMinCharacters(@Nullable Integer minCharacters) {
        this.minCharacters = minCharacters;
        return this;
    }

    @Nullable
    public TextPattern getPattern() {
        return pattern;
    }

    public TextField setPattern(@Nullable TextPattern pattern) {
        this.pattern = pattern;
        return this;
    }

    @Nullable
    public List<String> getSuggestions() {
        return suggestions;
    }

    public TextField setSuggestions(@Nullable List<String> suggestions) {
        this.suggestions = suggestions;
        return this;
    }
    // endregion
}
