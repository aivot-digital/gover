package de.aivot.GoverBackend.services.pdf;

import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.models.elements.BaseElement;

public record PdfElement(ElementType type, BaseElement element, int indent, Object value, String... classes) {
    public boolean hasValue() {
        return value != null;
    }

    public boolean isEmpty() {
        return !hasValue();
    }

    public String getClasses() {
        return type.name() + " indent-" + indent + " " + String.join(" ", classes);
    }

    public PdfElement withClasses(String... classes) {
        return new PdfElement(type, element, indent, value, classes);
    }
}
