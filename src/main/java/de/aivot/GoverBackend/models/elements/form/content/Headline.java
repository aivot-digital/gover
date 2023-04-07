package de.aivot.GoverBackend.models.elements.form.content;

import com.sun.istack.Nullable;
import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.elements.form.FormElement;
import de.aivot.GoverBackend.pdf.BasePdfRowDto;
import de.aivot.GoverBackend.pdf.HeadlinePdfRowDto;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Headline extends FormElement {
    public final static int HEADLINE_SIZE_DEFAULT = 2;
    public final static int HEADLINE_SIZE_SMALL = 2;

    private String content;
    private Boolean small;

    public Headline(BaseElement parent, Map<String, Object> data) {
        super(data);
        content = (String) data.get("content");
        small = (Boolean) data.get("small");
    }

    @Nullable
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Nullable
    public Boolean getSmall() {
        return small;
    }

    public void setSmall(Boolean small) {
        this.small = small;
    }

    @Override
    public List<BasePdfRowDto> toPdfRows(Map<String, Object> customerInput, @Nullable String idPrefix) {
        List<BasePdfRowDto> rows = new LinkedList<>();

        int size = getSmall() ? HEADLINE_SIZE_DEFAULT : HEADLINE_SIZE_SMALL;
        rows.add(new HeadlinePdfRowDto(getContent(), size));

        return rows;
    }
}
