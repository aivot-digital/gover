package de.aivot.GoverBackend.dtos;

public class ValueFieldDto extends FieldDto {
    public final String label;
    public final String value;

    public ValueFieldDto(String label, String value) {
        super("value");
        this.label = label;
        this.value = value;
    }
}
