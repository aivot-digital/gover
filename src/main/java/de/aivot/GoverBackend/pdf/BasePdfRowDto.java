package de.aivot.GoverBackend.pdf;

public abstract class BasePdfRowDto {
    public final PdfRowDtoType type;

    public BasePdfRowDto(PdfRowDtoType type) {
        this.type = type;
    }
}
