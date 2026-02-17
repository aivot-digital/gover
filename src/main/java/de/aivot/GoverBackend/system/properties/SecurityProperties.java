package de.aivot.GoverBackend.system.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "gover.security")
public class SecurityProperties {
    private String keystorePath;
    private String keystorePassword;

    public String getKeystorePath() {
        return keystorePath;
    }

    public SecurityProperties setKeystorePath(String keystorePath) {
        this.keystorePath = keystorePath;
        return this;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public SecurityProperties setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
        return this;
    }
}
