package de.aivot.GoverBackend.av.services;

import de.aivot.GoverBackend.av.exceptions.AVCheckFailedException;
import de.aivot.GoverBackend.av.exceptions.AVVirusFoundException;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.ClamConfig;
import de.aivot.GoverBackend.models.config.GoverConfig;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

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
        try (var clamAvSocket = new Socket(clamConfig.getHost(), Integer.parseInt(clamConfig.getPort()));
             var uploadStream = new BufferedOutputStream(clamAvSocket.getOutputStream());
             var responseStream = clamAvSocket.getInputStream()) {
            clamAvSocket.setSoTimeout(clamConfig.getTimeout());

            uploadStream.write(PING_COMMAND);
            uploadStream.flush();

            return "PONG".equalsIgnoreCase(readScannerResponse(responseStream));
        }
    }

    public void testMultipartFiles(MultipartFile[] files) throws ResponseException {
        if (files != null) {
            for (var file : files) {
                if (!file.isEmpty()) {
                    boolean fileExtensionAndContentTypeClean = testContentTypeAndExtension(file);

                    if (!fileExtensionAndContentTypeClean) {
                        throw ResponseException.notAcceptable("Extension or ContentType not allowed for attachment %s", file.getOriginalFilename());
                    }

                    testFile(file);
                }
            }
        }
    }

    public boolean testContentTypeAndExtension(MultipartFile file) {
        var filename = file.getOriginalFilename();
        if (filename == null) {
            return false;
        }

        var splitFilename = filename.split("\\.");
        if (splitFilename.length < 2) {
            return false;
        }

        var extension = splitFilename[splitFilename.length - 1];
        if (goverConfig.getFileExtensions().stream().noneMatch(ext -> ext.equalsIgnoreCase(extension))) {
            return false;
        }

        var contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }

        if (goverConfig.getContentTypes().stream().noneMatch(ct -> ct.equalsIgnoreCase(contentType))) {
            return false;
        }

        return true;
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
        try (var clamAvSocket = new Socket(clamConfig.getHost(), Integer.parseInt(clamConfig.getPort()));
             var uploadStream = new BufferedOutputStream(clamAvSocket.getOutputStream());
             var responseStream = clamAvSocket.getInputStream()) {
            clamAvSocket.setSoTimeout(clamConfig.getTimeout());

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
            if ("stream: OK".equalsIgnoreCase(response)) {
                return;
            }

            var resolvedFilename = resolveFilename(filename);
            if (response.toUpperCase().contains("ERROR")) {
                throw new AVCheckFailedException(
                        String.format("Der Virenscanner konnte die Datei \"%s\" nicht prüfen: %s", resolvedFilename, response)
                );
            }

            throw new AVVirusFoundException(resolvedFilename, response);
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
}
