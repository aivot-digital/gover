package de.aivot.GoverBackend.av.services;

import de.aivot.GoverBackend.av.exceptions.AVCheckFailedException;
import de.aivot.GoverBackend.av.exceptions.AVVirusFoundException;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.ClamConfig;
import de.aivot.GoverBackend.models.config.GoverConfig;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public class AVService {
    private static final byte[] PING_COMMAND = "zPING\0".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] INSTREAM_COMMAND = "zINSTREAM\0".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] STREAM_END = new byte[]{0, 0, 0, 0};
    private static final int CHUNK_SIZE = 8192;
    private static final int MAX_SCANNER_RESPONSE_BYTES = 2048;

    private final GoverConfig goverConfig;
    private final ClamConfig clamConfig;

    public AVService(GoverConfig goverConfig, ClamConfig clamConfig) {
        this.goverConfig = goverConfig;
        this.clamConfig = clamConfig;
    }

    public boolean testServiceStatus() throws IOException {
        try (var clamAvSocket = openScannerSocket();
             var uploadStream = new BufferedOutputStream(clamAvSocket.getOutputStream());
             var responseStream = clamAvSocket.getInputStream()) {
            uploadStream.write(PING_COMMAND);
            uploadStream.flush();

            return "PONG".equalsIgnoreCase(readScannerResponse(responseStream));
        }
    }

    public void testMultipartFiles(MultipartFile[] files) throws ResponseException {
        if (files != null) {
            for (var file : files) {
                if (!file.isEmpty()) {
                    var validationError = getContentTypeAndExtensionValidationError(file);
                    if (validationError != null) {
                        throw new ResponseException(HttpStatus.NOT_ACCEPTABLE, validationError);
                    }

                    testFile(file);
                }
            }
        }
    }

    public boolean testContentTypeAndExtension(MultipartFile file) {
        return getContentTypeAndExtensionValidationError(file) == null;
    }

    private String getContentTypeAndExtensionValidationError(MultipartFile file) {
        var filename = file.getOriginalFilename();
        if (filename == null) {
            return "Der Name des Anhangs fehlt.";
        }

        var splitFilename = filename.split("\\.");
        if (splitFilename.length < 2) {
            return String.format("Der Anhang \"%s\" hat keine Dateiendung.", filename);
        }

        var extension = splitFilename[splitFilename.length - 1];
        if (goverConfig.getFileExtensions().stream().noneMatch(ext -> ext.equalsIgnoreCase(extension))) {
            return String.format("Die Dateiendung des Anhangs \"%s\" ist nicht erlaubt.", filename);
        }

        var contentType = file.getContentType();
        if (contentType == null) {
            return String.format("Der Inhaltstyp des Anhangs \"%s\" fehlt.", filename);
        }

        if (goverConfig.getContentTypes().stream().noneMatch(ct -> ct.equalsIgnoreCase(contentType))) {
            return String.format("Der Inhaltstyp des Anhangs \"%s\" ist nicht erlaubt.", filename);
        }

        return null;
    }

    public void testFile(MultipartFile file) throws AVCheckFailedException, AVVirusFoundException {
        try (InputStream fileInputStream = file.getInputStream()) {
            testFile(fileInputStream, file.getOriginalFilename());
        } catch (IOException ex) {
            var filename = resolveFilename(file.getOriginalFilename());
            throw new AVCheckFailedException(
                    String.format("Die Datei \"%s\" konnte nicht gelesen werden: %s", filename, ex.getMessage()),
                    ex
            );
        }
    }

    public void testFile(InputStream fileInputStream) throws AVCheckFailedException, AVVirusFoundException {
        testFile(fileInputStream, "unbekannt");
    }

    public void testFile(InputStream fileInputStream, String filename) throws AVCheckFailedException, AVVirusFoundException {
        try (var clamAvSocket = openScannerSocket();
             var uploadStream = new BufferedOutputStream(clamAvSocket.getOutputStream());
             var responseStream = clamAvSocket.getInputStream()) {
            uploadStream.write(INSTREAM_COMMAND);

            var buffer = new byte[CHUNK_SIZE];
            int readBytes;
            while ((readBytes = fileInputStream.read(buffer)) != -1) {
                writeChunkLength(uploadStream, readBytes);
                uploadStream.write(buffer, 0, readBytes);
            }

            uploadStream.write(STREAM_END);
            uploadStream.flush();

            var response = readScannerResponse(responseStream);
            var resolvedFilename = resolveFilename(filename);
            validateScannerResponse(response, resolvedFilename);
        } catch (IOException ex) {
            throw new AVCheckFailedException(
                    String.format("Die Verbindung zum Virenscanner ist fehlgeschlagen: %s", ex.getMessage()),
                    ex
            );
        }
    }

    private static void writeChunkLength(BufferedOutputStream uploadStream, int length) throws IOException {
        uploadStream.write((length >>> 24) & 0xFF);
        uploadStream.write((length >>> 16) & 0xFF);
        uploadStream.write((length >>> 8) & 0xFF);
        uploadStream.write(length & 0xFF);
    }

    static int parseScannerPort(String rawPort) throws IOException {
        if (rawPort == null || rawPort.isBlank()) {
            throw new IOException("Der ClamAV-Port ist nicht konfiguriert.");
        }

        try {
            var port = Integer.parseInt(rawPort);
            if (port < 1 || port > 65535) {
                throw new IOException("Ungültiger ClamAV-Port: " + rawPort);
            }
            return port;
        } catch (NumberFormatException ex) {
            throw new IOException("Ungültiger ClamAV-Port: " + rawPort, ex);
        }
    }

    static int resolveScannerTimeout(Integer rawTimeout) throws IOException {
        if (rawTimeout == null) {
            return 0;
        }

        if (rawTimeout < 0) {
            throw new IOException("Ungültiges ClamAV-Timeout: " + rawTimeout);
        }

        return rawTimeout;
    }

    static void validateScannerResponse(String response, String filename) throws AVCheckFailedException, AVVirusFoundException {
        if ("stream: OK".equalsIgnoreCase(response)) {
            return;
        }

        if (response == null || response.isBlank()) {
            throw new AVCheckFailedException(
                    String.format("Der Virenscanner hat für die Datei \"%s\" keine verwertbare Antwort geliefert.", filename)
            );
        }

        if (response.toUpperCase(Locale.ROOT).contains("ERROR")) {
            throw new AVCheckFailedException(
                    String.format("Der Virenscanner konnte die Datei \"%s\" nicht prüfen: %s", filename, response)
            );
        }

        throw new AVVirusFoundException(filename, response);
    }

    private static String readScannerResponse(InputStream responseStream) throws IOException {
        var response = new ByteArrayOutputStream();
        int currentByte;

        while ((currentByte = responseStream.read()) != -1) {
            if (currentByte == 0 || currentByte == '\n') {
                break;
            }

            response.write(currentByte);

            if (response.size() >= MAX_SCANNER_RESPONSE_BYTES) {
                break;
            }
        }

        return response.toString(StandardCharsets.US_ASCII).trim();
    }

    private static String resolveFilename(String filename) {
        return filename == null || filename.isBlank() ? "unbekannt" : filename;
    }

    private Socket openScannerSocket() throws IOException {
        var host = clamConfig.getHost();
        if (host == null || host.isBlank()) {
            throw new IOException("Der ClamAV-Host ist nicht konfiguriert.");
        }

        var timeout = resolveScannerTimeout(clamConfig.getTimeout());
        var socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, parseScannerPort(clamConfig.getPort())), timeout);
            socket.setSoTimeout(timeout);
            return socket;
        } catch (IOException ex) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            throw ex;
        }
    }
}
