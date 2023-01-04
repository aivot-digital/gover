package de.aivot.GoverBackend.dtos;

public class HeadlineFieldDto extends FieldDto {
    public final String text;
    public final int size;

    public HeadlineFieldDto(String text, int size) {
        super("headline");
        this.text = text;
        this.size = size;
    }
}
