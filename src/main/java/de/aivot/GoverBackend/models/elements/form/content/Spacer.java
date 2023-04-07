package de.aivot.GoverBackend.models.elements.form.content;

import com.sun.istack.Nullable;
import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.elements.form.FormElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Spacer extends FormElement {
    private Integer height;

    public Spacer(BaseElement parent, Map<String, Object> data) {
        super(data);
        height = (Integer) data.get("height");
    }

    @Nullable
    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    @Override
    public boolean isValid(Map<String, Object> customerInput, @Nullable String idPrefix) {
        return true;
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(Map<String, Object> customerInput, @Nullable String idPrefix) {
        return new LinkedList<>();
    }
}
