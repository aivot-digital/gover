package de.aivot.GoverBackend.models.elements.form.input;

import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.models.elements.form.BaseInputElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.pdf.ValuePdfRowDto;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.script.ScriptEngine;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.PatternSyntaxException;

public class TextField extends BaseInputElement<String> {
    private String placeholder;
    private Boolean isMultiline;
    private Integer maxCharacters;

    public TextField(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        super.applyValues(values);

        placeholder = MapUtils.getString(values, "placeholder");
        isMultiline = MapUtils.getBoolean(values, "isMultiline");
        maxCharacters = MapUtils.getInteger(values, "maxCharacters");
    }

    @Override
    protected String formatValue(Object value) {
        if (value instanceof String sValue) {
            return sValue;
        }
        return null;
    }

    @Override
    public void validate(String idPrefix, RootElement root, Map<String, Object> customerInput, String value, ScriptEngine scriptEngine) throws ValidationException {
        if (maxCharacters != null && value.length() > maxCharacters) {
            throw new ValidationException(this, "Too many characters");
        }
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(RootElement root, Map<String, Object> customerInput, String value, String idPrefix, ScriptEngine scriptEngine) {
        List<BasePdfRowDto> fields = new LinkedList<>();

        fields.add(new ValuePdfRowDto(
                getLabel(),
                (value != null ? value : "Keine Angaben").replaceAll("\\r?\\n", "<br/>")
        ));

        return fields;
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
                        yield valA.matches(valB);
                    } catch (PatternSyntaxException ex) {
                        yield false;
                    }
                }
                case NotIncludesPattern -> {
                    try {
                        yield !valA.matches(valB);
                    } catch (PatternSyntaxException ex) {
                        yield false;
                    }
                }

                default -> false;
            };
        }

        return false;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
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
}
