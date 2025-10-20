package de.aivot.GoverBackend.elements.models.elements.form.content;

import de.aivot.GoverBackend.elements.models.elements.BaseFormElement;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class Image extends BaseFormElement {
    @Nullable
    private Integer height;
    @Nullable
    private Integer width;
    @Nullable
    private String src;
    @Nullable
    private String caption;
    @Nullable
    private String alt;

    public Image() {
        super(ElementType.Image);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Image image = (Image) o;
        return Objects.equals(height, image.height) && Objects.equals(width, image.width) && Objects.equals(src, image.src) && Objects.equals(caption, image.caption) && Objects.equals(alt, image.alt);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(height);
        result = 31 * result + Objects.hashCode(width);
        result = 31 * result + Objects.hashCode(src);
        result = 31 * result + Objects.hashCode(caption);
        result = 31 * result + Objects.hashCode(alt);
        return result;
    }

    @Nullable
    public Integer getHeight() {
        return height;
    }

    public Image setHeight(@Nullable Integer height) {
        this.height = height;
        return this;
    }

    @Nullable
    public Integer getWidth() {
        return width;
    }

    public Image setWidth(@Nullable Integer width) {
        this.width = width;
        return this;
    }

    @Nullable
    public String getSrc() {
        return src;
    }

    public Image setSrc(@Nullable String src) {
        this.src = src;
        return this;
    }

    @Nullable
    public String getCaption() {
        return caption;
    }

    public Image setCaption(@Nullable String caption) {
        this.caption = caption;
        return this;
    }

    @Nullable
    public String getAlt() {
        return alt;
    }

    public Image setAlt(@Nullable String alt) {
        this.alt = alt;
        return this;
    }
}
