package de.aivot.GoverBackend.models.elements.form.content;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.models.elements.form.BaseFormElement;
import de.aivot.GoverBackend.models.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.models.pdf.HeadlinePdfRowDto;
import de.aivot.GoverBackend.utils.MapUtils;

import javax.script.ScriptEngine;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Headline extends BaseFormElement {
    public final static int HEADLINE_SIZE_DEFAULT = 3;
    public final static int HEADLINE_SIZE_SMALL = 4;

    private String content;
    private Boolean small;
    private int size = 3;

    public Headline(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        super.applyValues(values);
        content = MapUtils.getString(values, "content", "");
        small = MapUtils.getBoolean(values, "small", false);
        size = small ? 4 : 3;
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(RootElement root, Map<String, Object> customerInput, String idPrefix, ScriptEngine scriptEngine) {
        List<BasePdfRowDto> rows = new LinkedList<>();

        int size = getSmall() ? HEADLINE_SIZE_DEFAULT : HEADLINE_SIZE_SMALL;
        rows.add(new HeadlinePdfRowDto(getContent(), size, this));

        return rows;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Headline headline = (Headline) o;

        if (!Objects.equals(content, headline.content)) return false;
        return Objects.equals(small, headline.small);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (content != null ? content.hashCode() : 0);
        result = 31 * result + (small != null ? small.hashCode() : 0);
        return result;
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

    @JsonIgnore
    public int getSize() {
        return size;
    }

    @JsonIgnore
    public void setSize(int size) {
        this.size = size;
    }

    //endregion
}
