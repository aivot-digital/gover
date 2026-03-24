package de.aivot.GoverBackend.av.services;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AVServiceTest {
    @Test
    void parseScannerPortShouldRejectInvalidValues() {
        assertThrows(IOException.class, () -> AVService.parseScannerPort("invalid"));
        assertThrows(IOException.class, () -> AVService.parseScannerPort("70000"));
        assertThrows(IOException.class, () -> AVService.resolveScannerTimeout(-1));
    }
}
