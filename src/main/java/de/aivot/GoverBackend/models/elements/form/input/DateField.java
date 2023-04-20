package de.aivot.GoverBackend.models.elements.form.input;

import com.sun.istack.Nullable;
import de.aivot.GoverBackend.enums.DateType;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.elements.form.BaseInputElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.pdf.ValuePdfRowDto;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.script.ScriptEngine;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DateField extends BaseInputElement<String> {
    private String placeholder;
    private DateType mode;
    private Boolean mustBePast;
    private Boolean mustBeFuture;

    public DateField(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        super.applyValues(values);

        placeholder = MapUtils.getString(values, "placeholder");
        mode = MapUtils.getEnum(values, "mode", String.class, DateType.values());
        mustBePast = MapUtils.getBoolean(values, "mustBePast");
        mustBeFuture = MapUtils.getBoolean(values, "mustBeFuture");
    }

    @Override
    public void validate(Map<String, Object> customerInput, String value, String idPrefix, ScriptEngine scriptEngine) throws ValidationException {
        if (value == null && Boolean.TRUE.equals(getRequired())) {
            throw new RequiredValidationException(this);
        }

        if (value != null) {
            LocalDate date;
            try {
                var cleandDate = value;
                if (value.contains("T")) {
                    cleandDate = value.split("T")[0];
                }
                date = LocalDate.parse(cleandDate);
            } catch (DateTimeParseException e) {
                throw new ValidationException(this, "Failed to parse date:" + e.getMessage());
            }
            validateIsFuture(date);
            validateIsPast(date);
        }
    }

    private void validateIsFuture(LocalDate date) throws ValidationException {
        if (Boolean.TRUE.equals(mustBeFuture)) {
            if (!date.isAfter(LocalDate.now())) {
                throw new ValidationException(this, "Must be future");
            }
        }
    }

    private void validateIsPast(LocalDate date) throws ValidationException {
        if (Boolean.TRUE.equals(mustBePast)) {
            if (!date.isBefore(LocalDate.now())) {
                throw new ValidationException(this, "Must be past");
            }
        }
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(Map<String, Object> customerInput, String value, String idPrefix, ScriptEngine scriptEngine) {
        List<BasePdfRowDto> rows = new LinkedList<>();

        String displayValue = "Keine Angaben";

        if (value != null) {
            String displayPattern = "dd.MM.yyyy";

            if (mode != null) {
                switch (mode) {
                    case Year -> displayPattern = "yyyy";
                    case Month -> displayPattern = "mm.yyyy";
                }
            }

            try {
                ZonedDateTime date = ZonedDateTime.parse(value);
                displayValue = date.format(DateTimeFormatter
                        .ofPattern(displayPattern)
                        .withZone(ZoneId.of("Europe/Paris")));
            } catch (DateTimeException e) {
                throw new RuntimeException(e);
            }
        }

        rows.add(new ValuePdfRowDto(
                getLabel(),
                displayValue
        ));

        return rows;
    }

    //region Getter & Setter

    @Nullable
    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    @Nullable
    public DateType getMode() {
        return mode;
    }

    public void setMode(DateType mode) {
        this.mode = mode;
    }

    @Nullable
    public Boolean getMustBePast() {
        return mustBePast;
    }

    public void setMustBePast(Boolean mustBePast) {
        this.mustBePast = mustBePast;
    }

    @Nullable
    public Boolean getMustBeFuture() {
        return mustBeFuture;
    }

    public void setMustBeFuture(Boolean mustBeFuture) {
        this.mustBeFuture = mustBeFuture;
    }

    //endregion

}
