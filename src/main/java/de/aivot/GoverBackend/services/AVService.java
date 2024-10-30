package de.aivot.GoverBackend.services;

import de.aivot.GoverBackend.exceptions.NotAcceptableException;
import de.aivot.GoverBackend.models.config.ClamConfig;
import de.aivot.GoverBackend.models.config.GoverConfig;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

@Component
public class AVService {

    private final GoverConfig goverConfig;
    private final ClamConfig clamConfig;

    public AVService(GoverConfig goverConfig, ClamConfig clamConfig) {
        this.goverConfig = goverConfig;
        this.clamConfig = clamConfig;
    }

    public void testMultipartFiles(MultipartFile[] files) {
        if (files != null) {
            for (var file : files) {
                if (!file.isEmpty()) {
                    boolean fileExtensionAndContentTypeClean = testContentTypeAndExtension(file);

                    if (!fileExtensionAndContentTypeClean) {
                        throw new NotAcceptableException("Extension or ContentType not allowed for attachment %s", file.getOriginalFilename());
                    }

                    try {
                        if (!testFile(file)) {
                            throw new NotAcceptableException("Virus detected in attachment %s", file.getOriginalFilename());
                        }
                    } catch (IOException ex) {
                        throw new RuntimeException("Failed to check file integrity", ex);
                    }
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

    public boolean testServiceStatus() throws IOException {
        var clamAvSocket = new Socket(clamConfig.getHost(), Integer.parseInt(clamConfig.getPort()));
        clamAvSocket.setSoTimeout(clamConfig.getTimeout());

        var uploadStream = new BufferedOutputStream(clamAvSocket.getOutputStream());
        var responseStream = clamAvSocket.getInputStream();

        uploadStream.write("zPING\0".getBytes());
        uploadStream.flush();

        String response = new String(responseStream.readAllBytes()).trim();

        clamAvSocket.close();

        return response.equalsIgnoreCase("PONG");
    }

    public boolean testFile(MultipartFile file) throws IOException {
        // Open socket connection to clamav
        var clamAvSocket = new Socket(clamConfig.getHost(), Integer.parseInt(clamConfig.getPort()));
        clamAvSocket.setSoTimeout(clamConfig.getTimeout());

        // Prepare upload and response streams
        var uploadStream = new BufferedOutputStream(clamAvSocket.getOutputStream());
        var responseStream = clamAvSocket.getInputStream();

        // Write start bytes to upload stream
        uploadStream.write("zINSTREAM\0".getBytes());
        uploadStream.flush();

        // Open file input stream
        InputStream fileInputStream = file.getInputStream();

        // Prepare file input stream buffer
        byte[] buffer = new byte[2048];

        // Read first bytes into buffer
        int readBytes = fileInputStream.read(buffer);

        // While bytes have been read, write the to the upload stream
        while (readBytes >= 0) {
            byte[] chunkSize = ByteBuffer.allocate(4).putInt(readBytes).array();

            uploadStream.write(chunkSize);
            uploadStream.write(buffer, 0, readBytes);

            // Check if server has interrupted the transmission
            if (responseStream.available() > 0) {
                throw new IOException("Interrupted by server " + new String(responseStream.readAllBytes()));
            }

            // Read next chunk of bytes into buffer
            readBytes = fileInputStream.read(buffer);
        }

        // Send final bytes to mark end of transmission
        uploadStream.write(new byte[]{0, 0, 0, 0});
        uploadStream.flush();

        // Read response from response stream
        String response = new String(responseStream.readAllBytes()).trim();

        // Close all opened streams
        clamAvSocket.close();
        responseStream.close();
        uploadStream.close();
        fileInputStream.close();

        return response.equalsIgnoreCase("stream: OK");
    }
}
