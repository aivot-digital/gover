package de.aivot.GoverBackend.process.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProcessNodePort (
        @NotNull(message = "Der Schlüssel des Ports darf nicht null sein.")
        @NotBlank(message = "Der Schlüssel des Ports darf nicht leer sein.")
        @Size(max = 96, message = "Der Schlüssel des Ports darf maximal 96 Zeichen lang sein.")
        String key,
        @NotNull(message = "Die Beschriftung des Ports darf nicht null sein.")
        @NotBlank(message = "Die Beschriftung des Ports darf nicht leer sein.")
        @Size(max = 64, message = "Die Beschriftung des Ports darf maximal 64 Zeichen lang sein.")
        String label,
        @NotNull(message = "Die Beschreibung des Ports darf nicht null sein.")
        @NotBlank(message = "Die Beschreibung des Ports darf nicht leer sein.")
        @Size(max = 512, message = "Die Beschreibung des Ports darf maximal 512 Zeichen lang sein.")
        String description
){

}
