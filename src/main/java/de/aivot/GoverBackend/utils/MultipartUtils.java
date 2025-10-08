package de.aivot.GoverBackend.utils;

import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MultipartUtils {
    public static class MultipartBodyPublisher {
        private final String boundary;
        private final List<byte[]> parts = new ArrayList<>();

        public MultipartBodyPublisher(String boundary) {
            this.boundary = boundary;
        }

        public MultipartBodyPublisher addPart(String name, String value) {
            String part = "--" + boundary + "\r\n" +
                          "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" +
                          value + "\r\n";
            parts.add(part.getBytes(StandardCharsets.UTF_8));
            return this;
        }

        public MultipartBodyPublisher addPart(String name, String filename, String content) {
            String part = "--" + boundary + "\r\n" +
                          "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n" +
                          "Content-Type: text/html\r\n\r\n" +
                          content + "\r\n";
            parts.add(part.getBytes(StandardCharsets.UTF_8));
            return this;
        }

        public HttpRequest.BodyPublisher build() {
            var end = ("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
            List<byte[]> allParts = new ArrayList<>(parts);
            allParts.add(end);
            return HttpRequest.BodyPublishers.ofByteArrays(allParts);
        }

        public String getBoundary() {
            return boundary;
        }
    }
}
