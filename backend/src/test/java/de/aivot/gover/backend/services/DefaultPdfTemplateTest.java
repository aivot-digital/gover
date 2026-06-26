package de.aivot.gover.backend.services;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultPdfTemplateTest {
    private static final String HEADER_HTML_SECTION_SEPARATOR = "<!-- KOPFZEILE -->";
    private static final String FOOTER_HTML_SECTION_SEPARATOR = "<!-- FUSSZEILE -->";
    private static final Path ALBATROS_TEMPLATE = Path.of(
            "default-assets",
            "Vorlagen",
            "Briefe",
            "Standardbrief - Albatros.html"
    );
    private static final Pattern CSS_URL_PATTERN = Pattern.compile("url\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern NON_DATA_SRC_PATTERN = Pattern.compile(
            "src\\s*=\\s*[\"'](?!data:)[^\"']+",
            Pattern.CASE_INSENSITIVE
    );

    @Test
    void albatrosHeaderAndFooterAvoidExternalAssets() throws IOException {
        var template = Files.readString(ALBATROS_TEMPLATE);
        var headerSeparatorIndex = template.indexOf(HEADER_HTML_SECTION_SEPARATOR);
        var footerSeparatorIndex = template.indexOf(FOOTER_HTML_SECTION_SEPARATOR);

        assertTrue(headerSeparatorIndex >= 0, "Expected a header section separator in the Albatros template.");
        assertTrue(footerSeparatorIndex >= 0, "Expected a footer section separator in the Albatros template.");
        assertTrue(
                headerSeparatorIndex < footerSeparatorIndex,
                "Expected the header section separator before the footer section separator."
        );

        assertSectionAvoidsExternalAssets(template.substring(0, headerSeparatorIndex));
        assertSectionAvoidsExternalAssets(template.substring(footerSeparatorIndex + FOOTER_HTML_SECTION_SEPARATOR.length()));
    }

    private static void assertSectionAvoidsExternalAssets(String htmlBlock) {
        assertFalse(
                CSS_URL_PATTERN.matcher(htmlBlock).find(),
                "Header/Footer HTML must not load external CSS assets, because Gotenberg times them out."
        );
        assertFalse(
                NON_DATA_SRC_PATTERN.matcher(htmlBlock).find(),
                "Header/Footer HTML must not reference external src assets, because Gotenberg times them out."
        );
    }
}
