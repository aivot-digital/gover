package de.aivot.GoverBackend.pdf;

import de.aivot.GoverBackend.models.elements.BaseElement;

public abstract class BasePdfRowDto {
    public final PdfRowDtoType type;
    public final BaseElement element;

    public BasePdfRowDto(PdfRowDtoType type, BaseElement element) {
        this.type = type;
        this.element = element;
    }
}
