package de.aivot.GoverBackend.pdf;

import java.util.List;

public class TablePdfRowDto extends BasePdfRowDto {
    public final String label;
    public final List<String> columnHeaders;
    public final List<List<String>> columnValues;

    public TablePdfRowDto(String label, List<String> columnHeaders, List<List<String>> columnValues) {
        super(PdfRowDtoType.Table);
        this.label = label;
        this.columnHeaders = columnHeaders;
        this.columnValues = columnValues;
    }
}
