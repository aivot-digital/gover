package de.aivot.GoverBackend.models.config;

import de.aivot.GoverBackend.utils.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "puppetpdf")
public class PuppetPdfConfig {
    private String host;
    private String port;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }
}
