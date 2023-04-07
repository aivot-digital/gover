package de.aivot.GoverBackend.models.elements.form.input;

import com.sun.istack.Nullable;
import de.aivot.GoverBackend.enums.DateType;
import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.elements.form.InputElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.pdf.ValuePdfRowDto;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DateField extends InputElement<String> {
    private String placeholder;
    private DateType mode;
    private Boolean mustBePast;
    private Boolean mustBeFuture;

    public DateField(BaseElement parent, Map<String, Object> data) {
        super(data);

        placeholder = (String) data.get("placeholder");
        mode = (DateType) data.get("mode");
        mustBePast = (Boolean) data.get("mustBePast");
        mustBeFuture = (Boolean) data.get("mustBeFuture");
    }

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

    @Override
    public boolean isValid(String value, String idPrefix) {
        LocalDate date;
        try {
            date = LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return false;
        }
        return validateIsFuture(date) && validateIsPast(date);
    }

    private boolean validateIsFuture(LocalDate date) {
        if (Boolean.TRUE.equals(mustBeFuture)) {
            return date.isAfter(LocalDate.now());
        }
        return true;
    }

    private boolean validateIsPast(LocalDate date) {
        if (Boolean.TRUE.equals(mustBePast)) {
            return date.isBefore(LocalDate.now());
        }
        return true;
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(String value, String idPrefix) {
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
}
