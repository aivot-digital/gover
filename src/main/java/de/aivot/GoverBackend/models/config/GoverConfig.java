package de.aivot.GoverBackend.models.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "gover")
public class GoverConfig {
    private String fromMail;
    private String reportMail;
    private String sentryServer;
    private String sentryWebApp;
    private String environment;
    private List<String> fileExtensions;
    private List<String> contentTypes;
    private String hostname;
    private Integer maxSubmissionCopyRetryCount;

    
    // region Getters & Setters

    public String getFromMail() {
        return fromMail;
    }

    public void setFromMail(String fromMail) {
        this.fromMail = fromMail;
    }

    public String getReportMail() {
        return reportMail;
    }

    public void setReportMail(String reportMail) {
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

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Integer getMaxSubmissionCopyRetryCount() {
        return maxSubmissionCopyRetryCount;
    }

    public void setMaxSubmissionCopyRetryCount(Integer maxSubmissionCopyRetryCount) {
        this.maxSubmissionCopyRetryCount = maxSubmissionCopyRetryCount;
    }


    // endregion
}
