package de.aivot.GoverBackend.lib.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResponseExceptionTest {
    @Test
    void internalServerError_UsesInternalServerErrorStatus() {
        var exception = ResponseException.internalServerError();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        assertEquals("Ein unbekannter Fehler ist aufgetreten. Bitte versuchen Sie es später erneut.", exception.getMessage());
    }
}
