package de.aivot.GoverBackend.models.pdf;

import de.aivot.GoverBackend.elements.models.BaseElement;

public class HeadlinePdfRowDto extends BasePdfRowDto {
    public final String text;
    public final int size;

    public HeadlinePdfRowDto(String text, int size, BaseElement element) {
        super(PdfRowDtoType.Headline, element);
        this.text = text;
        this.size = size;
    }
}
