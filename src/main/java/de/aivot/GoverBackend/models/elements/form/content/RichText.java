package de.aivot.GoverBackend.models.elements.form.content;

import de.aivot.GoverBackend.models.elements.form.BaseFormElement;
import de.aivot.GoverBackend.utils.MapUtils;

import java.util.Map;

public class RichText extends BaseFormElement {
    private String content;

    public RichText(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        super.applyValues(values);
        content = MapUtils.getString(values, "content", "");
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
