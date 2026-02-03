package de.aivot.GoverBackend.storage.services;

import de.aivot.GoverBackend.storage.models.KnownFileExtension;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Service
public class KnownExtensionsService {
    private static final String CVS_PATH = "classpath:data/known-extensions.csv";
    private static final String CVS_SEPARATOR = ";";

    @Nonnull
    private List<KnownFileExtension> knownExtensions = new LinkedList<>();

    @PostConstruct
    public void init() {
        File cvsFile;
        try {
            cvsFile = ResourceUtils
                    .getFile(CVS_PATH);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        knownExtensions = new LinkedList<>();

        try (var fileReader = new FileReader(cvsFile)) {
            try (var bufferedReader = new BufferedReader(fileReader)) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    var parts = line.split(CVS_SEPARATOR);

                    knownExtensions.add(
                            new KnownFileExtension(
                                    cleanQuotes(parts[0]),
                                    cleanQuotes(parts[1]),
                                    Arrays.asList(cleanQuotes(parts[2]).split(","))
                            )
                    );
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String cleanQuotes(@Nonnull String str) {
        if (str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length() - 1);
        } else {
            return str;
        }
    }

    public List<KnownFileExtension> getKnownExtensions() {
        return knownExtensions;
    }

    public Optional<String> determineMimeType(String filename) {
        String lowerFilename = filename.toLowerCase();
        for (KnownFileExtension knownExtension : knownExtensions) {
            for (String extension : knownExtension.getExtensions()) {
                if (lowerFilename.endsWith(extension.toLowerCase())) {
                    return Optional.of(knownExtension.getMime());
                }
            }
        }
        return Optional.empty();
    }
}
