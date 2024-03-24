package de.aivot.GoverBackend.services;

import org.apache.commons.io.FileUtils;
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

    public void deleteSubmissionFolder(String id) throws IOException {
        FileUtils.deleteDirectory(Paths.get(ROOT + id + "/").toFile());
    }

    public Path getSubmissionPdfPath(String id) {
        return Paths.get(ROOT + id + "/Antrag.pdf");
    }

    public Path getSubmissionHtmlPath(String id) {
        return Paths.get(ROOT + id + "/template.html");
    }

    public Path getSubmissionHeaderHtmlPath(String id) {
        return Paths.get(ROOT + id + "/header_template.html");
    }

    public Path getSubmissionFooterHtmlPath(String id) {
        return Paths.get(ROOT + id + "/footer_template.html");
    }

    public Path getSubmissionAttachmentPath(String id, String attachmentName) {
        return Paths.get(ROOT + id + "/attachments/" + attachmentName);
    }
}
