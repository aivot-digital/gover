package de.aivot.GoverBackend.models.elements.form.content;

import com.sun.istack.Nullable;
import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.elements.form.FormElement;

import java.util.Map;

public class RichText extends FormElement {
    private String content;

    public RichText(BaseElement parent, Map<String, Object> data) {
        super(data);
        content = (String) data.get("content");
    }

    @Nullable
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
