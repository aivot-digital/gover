package de.aivot.GoverBackend.models.elements.form.content;

import com.sun.istack.Nullable;
import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.elements.form.FormElement;

import java.util.Map;

public class Image extends FormElement {
    private Integer height;
    private Integer width;
    private String src;
    private String alt;

    public Image(BaseElement parent, Map<String, Object> data) {
        super(data);
        height = (Integer) data.get("height");
        width = (Integer) data.get("width");
        src = (String) data.get("src");
        alt = (String) data.get("alt");
    }

    @Nullable
    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    @Nullable
    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    @Nullable
    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    @Nullable
    public String getAlt() {
        return alt;
    }

    public void setAlt(String alt) {
        this.alt = alt;
    }
}
