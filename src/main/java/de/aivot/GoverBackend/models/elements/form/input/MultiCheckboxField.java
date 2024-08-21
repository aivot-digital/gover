package de.aivot.GoverBackend.models.elements.form.input;

import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.models.elements.form.BaseInputElement;
import de.aivot.GoverBackend.models.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.models.pdf.ValuePdfRowDto;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.script.ScriptEngine;
import java.util.*;

public class MultiCheckboxField extends BaseInputElement<Collection<String>> {
    private Collection<Object> options;
    private Integer minimumRequiredOptions;

    public MultiCheckboxField(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        super.applyValues(values);

        options = MapUtils.getObjectCollection(values, "options");
        minimumRequiredOptions = MapUtils.getInteger(values, "minimumRequiredOptions");
    }

    @Override
    public Collection<String> formatValue(Object value) {
        Collection<String> res = new LinkedList<>();
        if (value instanceof Collection<?> cValue) {
            for (Object val : cValue) {
                if (val instanceof String sVal) {
                    res.add(sVal);
                }
            }
        }
        return res.isEmpty() ? null : res;
    }

    @Override
    public void validate(String idPrefix, RootElement root, Map<String, Object> customerInput, Collection<String> value, ScriptEngine scriptEngine) throws ValidationException {
        testValuesInOptions(value);
        testRequiredOptionsMet(value);
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(RootElement root, Map<String, Object> customerInput, Collection<String> value, String idPrefix, ScriptEngine scriptEngine) {
        List<BasePdfRowDto> fields = new LinkedList<>();

        if (value == null || value.isEmpty()) {
            fields.add(new ValuePdfRowDto(
                    getLabel(),
                    "Keine Angaben",
                    this
            ));
        } else {

            var isFirst = true;
            for (String val : value) {
                for (var opt : options) {
                    if (opt instanceof String sOpt) {
                        if (sOpt.equals(val)) {
                            if (isFirst) {
                                fields.add(new ValuePdfRowDto(
                                        getLabel(),
                                        sOpt,
                                        this
                                ));
                                isFirst = false;
                            } else {
                                fields.add(new ValuePdfRowDto(
                                        "",
                                        sOpt,
                                        this
                                ));
                            }
                            break;
                        }
                    } else if (opt instanceof Map<?,?> mOpt) {
                        var optValue = mOpt.get("value");
                        if (optValue != null && optValue.equals(val)) {
                            if (isFirst) {
                                fields.add(new ValuePdfRowDto(
                                        getLabel(),
                                        (String) mOpt.get("label"),
                                        this
                                ));
                                isFirst = false;
                            } else {
                                fields.add(new ValuePdfRowDto(
                                        "",
                                        (String) mOpt.get("label"),
                                        this
                                ));
                            }
                            break;
                        }
                    }
                }
            }
        }

        return fields;
    }

    private void testValuesInOptions(Collection<String> values) throws ValidationException {
        for (String val : values) {
            boolean valueFound = false;
            for (Object opt : options) {
                if (opt instanceof String sOpt) {
                    if (val.equals(sOpt)) {
                        valueFound = true;
                        break;
                    }
                } else if (opt instanceof Map<?,?> mOpt) {
                    var optValue = mOpt.get("value");
                    if (optValue != null && optValue.equals(val)) {
                        valueFound = true;
                        break;
                    }
                }
            }
            if (!valueFound) {
                throw new ValidationException(this, "Invalid option " + val);
            }
        }
    }

    private void testRequiredOptionsMet(Collection<String> values) throws ValidationException {
        if (minimumRequiredOptions != null && values.size() < minimumRequiredOptions) {
            throw new ValidationException(this, "Not enough options selected");
        }
    }

    @Override
    public boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
        if (referencedValue == null) {
            return operator == ConditionOperator.Empty;
        }

        if (operator == ConditionOperator.NotEmpty) {
            return true;
        }

        if (referencedValue instanceof Collection<?> cValA) {
            if (comparedValue instanceof String sValueB) {
                return switch (operator) {
                    case Includes -> cValA.stream().anyMatch(sValueB::equals);
                    case NotIncludes -> cValA.stream().noneMatch(sValueB::equals);
                    default -> false;
                };
            }
        }

        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        MultiCheckboxField that = (MultiCheckboxField) o;

        if (!Objects.equals(options, that.options)) return false;
        return Objects.equals(minimumRequiredOptions, that.minimumRequiredOptions);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (options != null ? options.hashCode() : 0);
        result = 31 * result + (minimumRequiredOptions != null ? minimumRequiredOptions.hashCode() : 0);
        return result;
    }

    public Collection<Object> getOptions() {
        return options;
    }

    public void setOptions(Collection<Object> options) {
        this.options = options;
    }

    public Integer getMinimumRequiredOptions() {
        return minimumRequiredOptions;
    }

    public void setMinimumRequiredOptions(Integer minimumRequiredOptions) {
        this.minimumRequiredOptions = minimumRequiredOptions;
    }
}
