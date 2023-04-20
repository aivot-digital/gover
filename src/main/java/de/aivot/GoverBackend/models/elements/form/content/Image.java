package de.aivot.GoverBackend.models.elements.form.content;

import de.aivot.GoverBackend.models.elements.form.BaseFormElement;
import de.aivot.GoverBackend.utils.MapUtils;

import java.util.Map;

public class Image extends BaseFormElement {
    private Integer height;
    private Integer width;
    private String src;
    private String alt;

    public Image(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        super.applyValues(values);
        height = MapUtils.getInteger(values, "height", 100);
        width = MapUtils.getInteger(values, "width", 100);
        src = MapUtils.getString(values, "src", "");
        alt = MapUtils.getString(values, "alt", "");
    }

    //region Getters & Setters

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }


    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }


    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }


    public String getAlt() {
        return alt;
    }

    public void setAlt(String alt) {
        this.alt = alt;
    }

    //endregion
}
