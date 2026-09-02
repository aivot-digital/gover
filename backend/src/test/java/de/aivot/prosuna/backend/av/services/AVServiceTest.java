package de.aivot.prosuna.backend.av.services;

import de.aivot.prosuna.backend.av.exceptions.AVCheckFailedException;
import de.aivot.prosuna.backend.av.exceptions.AVVirusFoundException;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.models.config.ClamConfig;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AVServiceTest {
    @Test
    void testServiceStatusShouldUseClamdPingProtocol() throws Exception {
        try (var scanner = new ServerSocket(0)) {
            var scannerTask = startScannerTask(scanner, socket -> {
                var command = socket.getInputStream().readNBytes("zPING\0".length());
                assertArrayEquals("zPING\0".getBytes(StandardCharsets.US_ASCII), command);

                socket.getOutputStream().write("PONG\0".getBytes(StandardCharsets.US_ASCII));
                socket.getOutputStream().flush();
            });

            assertTrue(createService(scanner).testServiceStatus());
            scannerTask.get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void testFileShouldUseClamdInstreamProtocol() throws Exception {
        var content = new byte[9000];
        Arrays.fill(content, (byte) 'a');

        try (var scanner = new ServerSocket(0)) {
            var scannerTask = startScannerTask(scanner, socket -> {
                var input = new DataInputStream(socket.getInputStream());
                var command = input.readNBytes("zINSTREAM\0".length());
                assertArrayEquals("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII), command);

                var receivedContent = new ByteArrayOutputStream();
                int chunkLength;
                while ((chunkLength = input.readInt()) != 0) {
                    receivedContent.write(input.readNBytes(chunkLength));
                }
                assertArrayEquals(content, receivedContent.toByteArray());

                socket.getOutputStream().write("stream: OK\0".getBytes(StandardCharsets.US_ASCII));
                socket.getOutputStream().flush();
            });

            createService(scanner).testFile(new ByteArrayInputStream(content), "clean.txt");
            scannerTask.get(5, TimeUnit.SECONDS);
        }
    }

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

    private static AVService createService(ServerSocket scanner) {
        var clamConfig = new ClamConfig();
        clamConfig.setHost("127.0.0.1");
        clamConfig.setPort(String.valueOf(scanner.getLocalPort()));
        clamConfig.setTimeout(2000);
        return new AVService(new ProsunaConfig(), clamConfig);
    }

    private static FutureTask<Void> startScannerTask(ServerSocket scanner, SocketHandler handler) {
        var task = new FutureTask<Void>(() -> {
            try (var socket = scanner.accept()) {
                handler.handle(socket);
            }
            return null;
        });
        Thread.ofVirtual().start(task);
        return task;
    }

    @FunctionalInterface
    private interface SocketHandler {
        void handle(java.net.Socket socket) throws Exception;
    }
}
