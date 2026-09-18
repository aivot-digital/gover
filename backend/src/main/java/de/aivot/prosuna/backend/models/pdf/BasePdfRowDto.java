package de.aivot.prosuna.backend.models.pdf;

import de.aivot.prosuna.backend.elements.models.elements.BaseElement;

public abstract class BasePdfRowDto {
    public final PdfRowDtoType type;
    public final BaseElement element;

    public BasePdfRowDto(PdfRowDtoType type, BaseElement element) {
        this.type = type;
        this.element = element;
    }
}
