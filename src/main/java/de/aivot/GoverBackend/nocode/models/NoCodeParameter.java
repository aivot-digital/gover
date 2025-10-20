package de.aivot.GoverBackend.nocode.models;


public record NoCodeParameter(int type, String label, String description, NoCodeParameterOption... options) {
}
