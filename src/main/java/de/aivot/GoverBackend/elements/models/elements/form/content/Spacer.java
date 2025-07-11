package de.aivot.GoverBackend.elements.models.elements.form.content;

import de.aivot.GoverBackend.elements.models.elements.BaseFormElement;
import de.aivot.GoverBackend.enums.ElementType;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class Spacer extends BaseFormElement {
    @Nullable
    private String height;

    public Spacer() {
        super(ElementType.Spacer);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Spacer spacer = (Spacer) o;
        return Objects.equals(height, spacer.height);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(height);
        return result;
    }

    @Nullable
    public String getHeight() {
        return height;
    }

    public Spacer setHeight(@Nullable String height) {
        this.height = height;
        return this;
    }
}
