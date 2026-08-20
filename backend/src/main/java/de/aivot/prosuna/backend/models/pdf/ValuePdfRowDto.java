package de.aivot.prosuna.backend.models.pdf;

import de.aivot.prosuna.backend.elements.models.elements.BaseElement;

public class ValuePdfRowDto extends BasePdfRowDto {
    public final String label;
    public final String value;
    public final Boolean preformatted;

    public ValuePdfRowDto(String label, String value, BaseElement element)  {
        super(PdfRowDtoType.Value, element);
        this.label = label;
        this.value = value;
        this.preformatted = false;
    }

    public ValuePdfRowDto(String label, String value, Boolean preformatted, BaseElement element)  {
        super(PdfRowDtoType.Value, element);
        this.label = label;
        this.value = value;
        this.preformatted = preformatted;
    }
}
