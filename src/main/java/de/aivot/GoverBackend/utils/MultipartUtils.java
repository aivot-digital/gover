package de.aivot.GoverBackend.utils;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;

public class MultipartUtils {
    public static class MultipartBodyPublisher {
        private final MultiValueMap<String, Resource> parts = new LinkedMultiValueMap<>();

        public MultipartBodyPublisher addPart(String name, String value) {
            var bytes = value.getBytes(StandardCharsets.UTF_8);
            var res = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return "file";
                }
            };
            parts.add(name, res);
            return this;
        }

        public MultipartBodyPublisher addPart(String name, String filename, String content) {
            var bytes = content.getBytes(StandardCharsets.UTF_8);
            var res = new ByteArrayResource(bytes) {
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
