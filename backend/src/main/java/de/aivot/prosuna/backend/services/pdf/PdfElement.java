package de.aivot.prosuna.backend.services.pdf;

import de.aivot.prosuna.backend.elements.models.elements.BaseElement;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

public record PdfElement(
        @Nonnull
        BaseElement element,
        @Nullable
        Object value,
        @Nullable
        List<PdfElement> children
) {
    public boolean hasValue() {
        return value != null;
    }

    public boolean isEmpty() {
        return !hasValue();
    }

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }
}
