package de.aivot.GoverBackend.services;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultPdfTemplateTest {
    private static final Path ALBATROS_TEMPLATE = Path.of(
            "default-assets",
            "Vorlagen",
            "Briefe",
            "Standardbrief - Albatros.html"
    );
    private static final Pattern HTML_DOCUMENT_BLOCK_PATTERN = Pattern.compile(
            "<html\\b.*?</html>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern CSS_URL_PATTERN = Pattern.compile("url\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern NON_DATA_SRC_PATTERN = Pattern.compile(
            "src\\s*=\\s*[\"'](?!data:)[^\"']+",
            Pattern.CASE_INSENSITIVE
    );

    @Test
    void albatrosHeaderAndFooterAvoidExternalAssets() throws IOException {
        var template = Files.readString(ALBATROS_TEMPLATE);
        var matcher = HTML_DOCUMENT_BLOCK_PATTERN.matcher(template);
        var checkedSections = 0;

        while (matcher.find()) {
            var htmlBlock = matcher.group();
            if (!htmlBlock.contains("::header::") && !htmlBlock.contains("::footer::")) {
                continue;
            }

            checkedSections++;
            assertFalse(
                    CSS_URL_PATTERN.matcher(htmlBlock).find(),
                    "Header/Footer HTML must not load external CSS assets, because Gotenberg times them out."
            );
            assertFalse(
                    NON_DATA_SRC_PATTERN.matcher(htmlBlock).find(),
                    "Header/Footer HTML must not reference external src assets, because Gotenberg times them out."
            );
        }

        assertTrue(checkedSections >= 2, "Expected header and footer sections in the Albatros template.");
    }
}
