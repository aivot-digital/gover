package de.aivot.prosuna.backend.av.services;

import de.aivot.prosuna.backend.av.exceptions.AVCheckFailedException;
import de.aivot.prosuna.backend.av.exceptions.AVVirusFoundException;
import de.aivot.prosuna.backend.av.services.AVService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.models.config.ClamConfig;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

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

    @Test
    void testMultipartFilesShouldPreserveValidationReason() {
        var prosunaConfig = new ProsunaConfig();
        prosunaConfig.setFileExtensions(List.of("pdf"));
        prosunaConfig.setContentTypes(List.of("application/pdf"));

        var service = new AVService(prosunaConfig, new ClamConfig()) {
            @Override
            public void testFile(MultipartFile file) {
                throw new AssertionError("Virenscan darf bei ungültigem Dateityp nicht starten.");
            }
        };

        var file = new MockMultipartFile("file", "report.exe", "application/pdf", "content".getBytes());

        var exception = assertThrows(ResponseException.class, () -> service.testMultipartFiles(new MultipartFile[]{file}));

        assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getStatus());
        assertEquals("Die Dateiendung des Anhangs \"report.exe\" ist nicht erlaubt.", exception.getTitle());
    }
}
