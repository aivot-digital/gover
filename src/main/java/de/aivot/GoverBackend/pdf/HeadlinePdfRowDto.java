package de.aivot.GoverBackend.pdf;

public class HeadlinePdfRowDto extends BasePdfRowDto {
    public final String text;
    public final int size;

    public HeadlinePdfRowDto(String text, int size) {
        super(PdfRowDtoType.Headline);
        this.text = text;
        this.size = size;
    }
}
