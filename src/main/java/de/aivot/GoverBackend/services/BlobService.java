package de.aivot.GoverBackend.services;

import de.aivot.GoverBackend.enums.SystemAssetKey;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class BlobService {
    public void init() throws IOException {
        Files.createDirectories(Paths.get("./media/prints/"));
        Files.createDirectories(Paths.get("./media/assets/"));
    }

    public Path getPrintPdfPath(String uuid) {
        return Paths.get("./media/prints/" + uuid + ".pdf");
    }

    public Path getPrintHtmlPath(String uuid) {
        return Paths.get("./media/prints/" + uuid + ".html");
    }

    public Path getAssetPath(SystemAssetKey key) {
        return switch (key) {
            case Favicon -> Paths.get("./media/assets/favicon.ico");
            case Logo -> Paths.get("./media/assets/logo.png");
        };
    }
}
