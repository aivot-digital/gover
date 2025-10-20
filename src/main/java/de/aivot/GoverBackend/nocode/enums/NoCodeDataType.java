package de.aivot.GoverBackend.nocode.enums;

/**
 * Enum to represent the data types that can be used in the NoCode system.
 * Each data type has a key that is used to serialize the enum to JSON.
 */
public class NoCodeDataType {
    public static int Runtime = 0b11111111;
    public static int Boolean = 0b00000001;
    public static int Number  = 0b00000010;
    public static int String  = 0b00000100;
    public static int Date    = 0b00001000;
    public static int Time    = 0b00010000;
    public static int List    = 0b01000000;
    public static int Object  = 0b10000000;
}
