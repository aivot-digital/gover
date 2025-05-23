package de.aivot.GoverBackend.elements.models.form.input;

import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.form.models.FormState;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.elements.models.form.BaseInputElement;
import de.aivot.GoverBackend.models.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.models.pdf.ValuePdfRowDto;
import de.aivot.GoverBackend.utils.MapUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class TextField extends BaseInputElement<String> {
    private String placeholder;
    private String autocomplete;
    private Boolean isMultiline;
    private Integer maxCharacters;
    private Integer minCharacters;
    private TextPattern pattern;

    public TextField(Map<String, Object> data) {
        super(data);
        if (data.containsKey("pattern") && data.get("pattern") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> patternMap = (Map<String, Object>) data.get("pattern");
            this.pattern = new TextPattern(patternMap);
        }
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        super.applyValues(values);

        placeholder = MapUtils.getString(values, "placeholder");
        autocomplete = MapUtils.getString(values, "autocomplete");
        isMultiline = MapUtils.getBoolean(values, "isMultiline");
        maxCharacters = MapUtils.getInteger(values, "maxCharacters");
        minCharacters = MapUtils.getInteger(values, "minCharacters");

        if (values.containsKey("pattern") && values.get("pattern") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> patternMap = (Map<String, Object>) values.get("pattern");
            this.pattern = new TextPattern(patternMap);
        }
    }

    @Override
    public String formatValue(Object value) {
        if (value instanceof String sValue) {
            return sValue;
        }
        return null;
    }

    @Override
    public void validate(String value) throws ValidationException {
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

    @Override
    public List<BasePdfRowDto> toPdfRows(RootElement root, String value, String idPrefix, FormState formState) {
        List<BasePdfRowDto> fields = new LinkedList<>();

        fields.add(new ValuePdfRowDto(
                getLabel(),
                (value != null ? value : "Keine Angabe"),
                true,
                this
        ));

        return fields;
    }

    public String toDisplayValue(Object rawValue) {
        var value = formatValue(rawValue);
        return value != null ? value : "Keine Angabe";
    }

    @Override
    public boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        TextField textField = (TextField) o;

        if (!Objects.equals(placeholder, textField.placeholder))
            return false;
        if (!Objects.equals(autocomplete, textField.autocomplete))
            return false;
        if (!Objects.equals(isMultiline, textField.isMultiline))
            return false;
        if (!Objects.equals(pattern, textField.pattern))
            return false;
        if (!Objects.equals(maxCharacters, textField.maxCharacters))
            return false;
        return Objects.equals(minCharacters, textField.minCharacters);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (placeholder != null ? placeholder.hashCode() : 0);
        result = 31 * result + (autocomplete != null ? autocomplete.hashCode() : 0);
        result = 31 * result + (isMultiline != null ? isMultiline.hashCode() : 0);
        result = 31 * result + (maxCharacters != null ? maxCharacters.hashCode() : 0);
        result = 31 * result + (minCharacters != null ? minCharacters.hashCode() : 0);
        result = 31 * result + (pattern != null ? pattern.hashCode() : 0);
        return result;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public String getAutocomplete() {
        return autocomplete;
    }

    public void setAutocomplete(String autocomplete) {
        this.autocomplete = autocomplete;
    }

    public Boolean getIsMultiline() {
        return isMultiline;
    }

    public void setIsMultiline(Boolean multiline) {
        isMultiline = multiline;
    }

    public Integer getMaxCharacters() {
        return maxCharacters;
    }

    public void setMaxCharacters(Integer maxCharacters) {
        this.maxCharacters = maxCharacters;
    }

    public Integer getMinCharacters() {
        return minCharacters;
    }

    public void setMinCharacters(Integer minCharacters) {
        this.minCharacters = minCharacters;
    }

    public TextPattern getPattern() {
        return pattern;
    }

    public void setPattern(TextPattern pattern) {
        this.pattern = pattern;
    }
}
