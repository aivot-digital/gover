package de.aivot.GoverBackend.pdf;

public class ValuePdfRowDto extends BasePdfRowDto {
    public final String label;
    public final String value;

    public ValuePdfRowDto(String label, String value) {
        super(PdfRowDtoType.Value);
        this.label = label;
        this.value = value;
    }
}
