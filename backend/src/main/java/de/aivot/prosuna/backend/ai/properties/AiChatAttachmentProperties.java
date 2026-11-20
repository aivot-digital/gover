package de.aivot.prosuna.backend.ai.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "prosuna.ai.chat.attachments")
@Validated
public class AiChatAttachmentProperties {
    @Min(1)
    @Max(1)
    private int maxFiles = 1;

    @Min(1)
    private long maxFileSizeBytes = 10 * 1024 * 1024;

    @Min(1)
    private int maxExtractedCharacters = 100_000;

    @NotEmpty
    private List<String> extensions = List.of(
            "pdf", "txt", "csv", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "odt", "fodt", "ods", "fods", "odp", "fodp", "odg", "fodg", "odf", "xml", "bpmn"
    );

    public int getMaxFiles() {
        return maxFiles;
    }

    public void setMaxFiles(int maxFiles) {
        this.maxFiles = maxFiles;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public int getMaxExtractedCharacters() {
        return maxExtractedCharacters;
    }

    public void setMaxExtractedCharacters(int maxExtractedCharacters) {
        this.maxExtractedCharacters = maxExtractedCharacters;
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public void setExtensions(List<String> extensions) {
        this.extensions = extensions;
    }
}
