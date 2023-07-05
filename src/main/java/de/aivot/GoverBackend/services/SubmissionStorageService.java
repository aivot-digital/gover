package de.aivot.GoverBackend.services;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Stream;

@Component
public class SubmissionStorageService {
    private static final String ROOT = "./data/submissions/";

    public void initRoot() throws IOException {
        Files.createDirectories(Paths.get(ROOT));
    }

    public void initSubmission(String id) throws IOException {
        Files.createDirectories(Paths.get(ROOT + id + "/"));
        Files.createDirectories(Paths.get(ROOT + id + "/attachments/"));
    }

    public Path getSubmissionPdfPath(String id) {
        return Paths.get(ROOT + id + "/print.pdf");
    }

    public Path getSubmissionHtmlPath(String id) {
        return Paths.get(ROOT + id + "/template.html");
    }

    public Path getSubmissionAttachmentPath(String id, String attachmentName) {
        return Paths.get(ROOT + id + "/attachments/" + attachmentName);
    }

    public Collection<Path> getSubmissionAttachmentPaths(String id) throws IOException {
        Stream<Path> stream = Files.list(Paths.get(ROOT + id + "/attachments/"));
        return stream
                .filter(file -> !Files.isDirectory(file))
                .toList();
    }
}
