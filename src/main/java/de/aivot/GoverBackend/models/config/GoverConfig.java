package de.aivot.GoverBackend.models.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "gover")
public class GoverConfig {
    private String fromMail;
    private String reportMail;
    private String sentryServer;
    private String sentryWebApp;
    private String environment;

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
}
