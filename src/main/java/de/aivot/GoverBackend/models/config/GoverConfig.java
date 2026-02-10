package de.aivot.GoverBackend.models.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
    private List<String> bootstrapAdminMail;

    public String getDefaultLogoUrl() {
        return createUrl("/assets/default-logo.png");
    }

    public String getDefaultFaviconUrl() {
        return createUrl("/assets/default-favicon.ico");
    }

    public String createUrl(String path) {
        var uri = URI.create(goverHostname);
        return uri.resolve(path).toString();
    }

    public String createUrl(String base, Object... parts) {
        var uri = URI.create(goverHostname);

        var resolvedParts = Arrays
                .stream(parts)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .map(part -> URLEncoder.encode(part, StandardCharsets.UTF_8))
                .collect(Collectors.joining("/"));

        if (base.endsWith("/")) {
            return uri.resolve(base + resolvedParts).toString();
        } else {
            return uri.resolve(base + "/" + resolvedParts).toString();
        }
    }

    public String createUrlWithTrailingSlash(String base, Object... parts) {
        var url = createUrl(base, parts);
        if (!url.endsWith("/")) {
            url += "/";
        }
        return url;
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

    public List<String> getBootstrapAdminMail() {
        return bootstrapAdminMail;
    }

    public void setBootstrapAdminMail(List<String> bootstrapAdminMail) {
        this.bootstrapAdminMail = bootstrapAdminMail;
    }

    // endregion
}
