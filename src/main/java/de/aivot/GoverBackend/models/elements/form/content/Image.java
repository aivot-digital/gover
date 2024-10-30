package de.aivot.GoverBackend.models.elements.form.content;

import de.aivot.GoverBackend.models.elements.form.BaseFormElement;
import de.aivot.GoverBackend.utils.MapUtils;

import java.util.Map;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Image image = (Image) o;

        if (!Objects.equals(height, image.height)) return false;
        if (!Objects.equals(width, image.width)) return false;
        if (!Objects.equals(src, image.src)) return false;
        return Objects.equals(alt, image.alt);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (height != null ? height.hashCode() : 0);
        result = 31 * result + (width != null ? width.hashCode() : 0);
        result = 31 * result + (src != null ? src.hashCode() : 0);
        result = 31 * result + (alt != null ? alt.hashCode() : 0);
        return result;
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
