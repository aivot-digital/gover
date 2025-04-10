package de.aivot.GoverBackend.models.pdf;

import de.aivot.GoverBackend.elements.models.BaseElement;

public abstract class BasePdfRowDto {
    public final PdfRowDtoType type;
    public final BaseElement element;

    public BasePdfRowDto(PdfRowDtoType type, BaseElement element) {
        this.type = type;
        this.element = element;
    }
}
