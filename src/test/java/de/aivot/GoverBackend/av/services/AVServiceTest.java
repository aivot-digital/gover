package de.aivot.GoverBackend.av.services;

import de.aivot.GoverBackend.av.exceptions.AVCheckFailedException;
import de.aivot.GoverBackend.av.exceptions.AVVirusFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AVServiceTest {
    @Test
    void parseScannerPortShouldRejectInvalidValues() {
        assertThrows(IOException.class, () -> AVService.parseScannerPort("invalid"));
        assertThrows(IOException.class, () -> AVService.parseScannerPort("70000"));
        assertThrows(IOException.class, () -> AVService.resolveScannerTimeout(-1));
    }

    @Test
    void validateScannerResponseShouldTreatBlankResponsesAsCheckFailures() {
        var exception = assertThrows(AVCheckFailedException.class, () ->
                AVService.validateScannerResponse(" ", "report.pdf")
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        assertTrue(String.valueOf(exception.getDetails()).contains("keine verwertbare Antwort"));
    }

    @Test
    void validateScannerResponseShouldKeepVirusResponsesAsVirusFindings() {
        var exception = assertThrows(AVVirusFoundException.class, () ->
                AVService.validateScannerResponse("stream: Eicar FOUND", "report.pdf")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
}
