package de.aivot.GoverBackend.dtos;

import java.util.List;

public class TableFieldDto extends FieldDto {
    public final String label;
    public final List<String> columnHeaders;
    public final List<List<String>> columnValues;

    public TableFieldDto(String label, List<String> columnHeaders, List<List<String>> columnValues) {
        super("table");
        this.label = label;
        this.columnHeaders = columnHeaders;
        this.columnValues = columnValues;
    }
}
