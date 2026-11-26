package de.aivot.prosuna.backend.lib.exceptions;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResponseExceptionTest {
    @Test
    void internalServerError_UsesInternalServerErrorStatus() {
        var exception = ResponseException.internalServerError();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        assertEquals("Ein unerwarteter Fehler ist aufgetreten.", exception.getMessage());
    }

    @Test
    void notAcceptable_KeepsTheGivenMessage() {
        var exception = ResponseException.notAcceptable("Der Pfad einer Datei darf nicht leer sein.");

        assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getStatus());
        assertEquals("Der Pfad einer Datei darf nicht leer sein.", exception.getMessage());
    }

    @Test
    void notAcceptable_UsesNotAcceptableStatusWithoutMessage() {
        var exception = ResponseException.notAcceptable();

        assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getStatus());
        assertEquals("Die angeforderten Daten sind im gewünschten Format nicht verfügbar.", exception.getMessage());
    }
}
