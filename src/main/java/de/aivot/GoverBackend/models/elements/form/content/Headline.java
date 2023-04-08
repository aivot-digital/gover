package de.aivot.GoverBackend.models.elements.form.content;

import de.aivot.GoverBackend.models.elements.form.FormElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.pdf.HeadlinePdfRowDto;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.script.ScriptEngine;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Headline extends FormElement {
    public final static int HEADLINE_SIZE_DEFAULT = 2;
    public final static int HEADLINE_SIZE_SMALL = 2;

    private String content;
    private Boolean small;

    public Headline(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        super.applyValues(values);
        content = MapUtils.getString(values, "content", "");
        small = MapUtils.getBoolean(values, "small", false);
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(Map<String, Object> customerInput, String idPrefix, ScriptEngine scriptEngine) {
        List<BasePdfRowDto> rows = new LinkedList<>();

        int size = getSmall() ? HEADLINE_SIZE_DEFAULT : HEADLINE_SIZE_SMALL;
        rows.add(new HeadlinePdfRowDto(getContent(), size));

        return rows;
    }

    //region Getters & Setters

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getSmall() {
        return small;
    }

    public void setSmall(Boolean small) {
        this.small = small;
    }

    //endregion
}
