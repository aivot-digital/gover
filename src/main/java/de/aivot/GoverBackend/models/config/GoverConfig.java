package de.aivot.GoverBackend.models.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@ConfigurationProperties(prefix = "gover")
public class GoverConfig {
    private String fromMail;
    private List<String> reportMail;
    private String sentryServer;
    private String sentryWebApp;
    private String environment;
    private List<String> fileExtensions;
    private List<String> contentTypes;
    private String goverHostname;
    private Integer maxSubmissionCopyRetryCount;

    public String createUrl(String path) {
        var uri = URI.create(goverHostname);
        return uri.resolve(path).toString();
    }

    public String createUrl(String base, String... parts) {
        var uri = URI.create(goverHostname);

        var resolvedParts = Arrays
                .stream(parts)
                .map(part -> URLEncoder.encode(part, StandardCharsets.UTF_8))
                .collect(Collectors.joining("/"));

        if (base.endsWith("/")) {
            return uri.resolve(base + resolvedParts).toString();
        } else {
            return uri.resolve(base + "/" + resolvedParts).toString();
        }
    }

    // region Getters & Setters

    public String getFromMail() {
        return fromMail;
    }

    public void setFromMail(String fromMail) {
        this.fromMail = fromMail;
    }

    public List<String> getReportMail() {
        return reportMail;
    }

    public void setReportMail(List<String> reportMail) {
        this.reportMail = reportMail;
    }

    public String getSentryServer() {
        return sentryServer;
    }

    public void setSentryServer(String sentryServer) {
        this.sentryServer = sentryServer;
    }

    public String getSentryWebApp() {
        return sentryWebApp;
    }

    public void setSentryWebApp(String sentryWebApp) {
        this.sentryWebApp = sentryWebApp;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public List<String> getFileExtensions() {
        return fileExtensions;
    }

    public void setFileExtensions(List<String> fileExtensions) {
        this.fileExtensions = fileExtensions;
    }

    public List<String> getContentTypes() {
        return contentTypes;
    }

    public void setContentTypes(List<String> contentTypes) {
        this.contentTypes = contentTypes;
    }

    public String getGoverHostname() {
        return goverHostname;
    }

    public void setGoverHostname(String goverHostname) {
        this.goverHostname = goverHostname;
    }

    public Integer getMaxSubmissionCopyRetryCount() {
        return maxSubmissionCopyRetryCount;
    }

    public void setMaxSubmissionCopyRetryCount(Integer maxSubmissionCopyRetryCount) {
        this.maxSubmissionCopyRetryCount = maxSubmissionCopyRetryCount;
    }

    // endregion
}
