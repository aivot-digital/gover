package de.aivot.GoverBackend.models;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "gover")
public class GoverConfig {
    private String reportMail;

    public String getReportMail() {
        return reportMail;
    }

    public void setReportMail(String reportMail) {
        this.reportMail = reportMail;
    }
}
