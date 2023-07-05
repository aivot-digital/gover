package de.aivot.GoverBackend.services;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Component
public class AssetStorageService {
    private static final String ROOT = "./data/assets/";

    public void init() throws IOException {
        Files.createDirectories(Paths.get(ROOT));
    }

    public String getAssetRoot() {
        return ROOT;
    }

    public Optional<Path> getAssetPath(String key) {
        if (!key.matches("[a-zA-Z0-9 _-]+\\.[a-zA-Z0-9]+")) {
            return Optional.empty();
        }

        return Optional.of(Paths.get(ROOT + key));
    }
}
