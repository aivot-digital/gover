package de.aivot.GoverBackend.utils;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.util.MultiValueMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MultipartUtilsTest {
    @Test
    void buildShouldKeepPlainFieldsAsStringsAndFilesAsResources() {
        var body = new MultipartUtils.MultipartBodyPublisher()
                .addPart("marginTop", "2.5cm")
                .addPart("files", "index.html", "<html><body>Test</body></html>")
                .build();

        @SuppressWarnings("unchecked")
        var parts = (MultiValueMap<String, Object>) body;

        assertEquals("2.5cm", parts.getFirst("marginTop"));

        var filePart = assertInstanceOf(Resource.class, parts.getFirst("files"));
        assertNotNull(filePart);
        assertEquals("index.html", filePart.getFilename());
    }
}
