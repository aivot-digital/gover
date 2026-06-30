package de.aivot.gover.backend.process.services;

import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.gover.backend.process.services.CaseNumberGeneratorService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CaseNumberGeneratorServiceTest {
    @Test
    void generateCaseNumber_RendersProcessPlaceholdersAndIncrementsWithinRenderedBucket() throws ResponseException {
        var repository = mock(ProcessInstanceRepository.class);
        when(repository.getMaxCaseNumberIncrement(
                eq("AZ-2026-05-29-14-07-"),
                eq(""),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt()
        )).thenReturn(41);

        var service = new CaseNumberGeneratorService(repository);
        var result = service.generateCaseNumber(
                "AZ-%YYY-%M-%D-%h-%m-%I(4)",
                ZonedDateTime.of(2026, 5, 29, 14, 7, 0, 0, ZoneId.of("Europe/Berlin"))
        );

        assertEquals("AZ-2026-05-29-14-07-0042", result);
    }

    @Test
    void generateCaseNumber_UsesShortYearPlaceholderWhenConfigured() throws ResponseException {
        var repository = mock(ProcessInstanceRepository.class);
        when(repository.getMaxCaseNumberIncrement(
                eq("AZ-26-"),
                eq(""),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt()
        )).thenReturn(6);

        var service = new CaseNumberGeneratorService(repository);
        var result = service.generateCaseNumber(
                "AZ-%Y-%I(4)",
                ZonedDateTime.of(2026, 5, 29, 14, 7, 0, 0, ZoneId.of("Europe/Berlin"))
        );

        assertEquals("AZ-26-0007", result);
    }

    @Test
    void validateCaseNumberTemplate_RejectsUppercaseHourPlaceholder() {
        var service = new CaseNumberGeneratorService(mock(ProcessInstanceRepository.class));

        var exception = assertThrows(
                ResponseException.class,
                () -> service.validateCaseNumberTemplate("AZ-%H-%I(4)")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals(
                "Die Vorgangsschlüssel-Formatvorlage enthält einen unbekannten Platzhalter an Position 4. Unterstützt werden %YYY, %Y, %M, %D, %h, %m und %I(n).",
                exception.getTitle()
        );
    }

    @Test
    void validateCaseNumberTemplate_RejectsMissingIncrementPlaceholder() {
        var service = new CaseNumberGeneratorService(mock(ProcessInstanceRepository.class));

        var exception = assertThrows(
                ResponseException.class,
                () -> service.validateCaseNumberTemplate("AZ-%YYY-%M-%D")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals(
                "Die Vorgangsschlüssel-Formatvorlage muss genau einen Inkrement-Platzhalter im Format %I(n) enthalten.",
                exception.getTitle()
        );
    }

    @Test
    void validateCaseNumberTemplate_RejectsIncrementPaddingOutsideProcessBounds() {
        var service = new CaseNumberGeneratorService(mock(ProcessInstanceRepository.class));

        var exception = assertThrows(
                ResponseException.class,
                () -> service.validateCaseNumberTemplate("AZ-%I(3)")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals(
                "Die Inkrement-Breite in der Vorgangsschlüssel-Formatvorlage muss zwischen 4 und 12 Stellen liegen.",
                exception.getTitle()
        );
    }

    @Test
    void validateCaseNumberTemplate_RejectsTemplatesThatWouldExceedDatabaseLength() {
        var service = new CaseNumberGeneratorService(mock(ProcessInstanceRepository.class));

        var exception = assertThrows(
                ResponseException.class,
                () -> service.validateCaseNumberTemplate("VERFAHREN-2026-LANDKREIS-NORD-%YYY-%I(12)")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals(
                "Der erzeugte Vorgangsschlüssel würde das Limit von 36 Zeichen überschreiten.",
                exception.getTitle()
        );
    }

    @Test
    void generateCaseNumber_ReturnsUuidWhenTemplateIsMissing() {
        var repository = mock(ProcessInstanceRepository.class);
        var service = new CaseNumberGeneratorService(repository);

        var result = assertDoesNotThrow(() -> service.generateCaseNumber(null));

        assertEquals(result, UUID.fromString(result).toString());
        verifyNoInteractions(repository);
    }
}
