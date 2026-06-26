package de.aivot.gover.backend.utils;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;

public class MultipartUtils {
    public static class MultipartBodyPublisher {
        private final MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();

        public MultipartBodyPublisher addPart(String name, String value) {
            parts.add(name, value);
            return this;
        }

        public MultipartBodyPublisher addPart(String name, String filename, String content) {
            var bytes = content.getBytes(StandardCharsets.UTF_8);
            return addPart(name, filename, bytes);
        }

        public MultipartBodyPublisher addPart(String name, String filename, byte[] content) {
            var res = new ByteArrayResource(content) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
            parts.add(name, res);
            return this;
        }

        public Object build() {
            return parts;
        }
    }
}
