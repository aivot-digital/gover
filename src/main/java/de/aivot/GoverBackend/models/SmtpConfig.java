package de.aivot.GoverBackend.models;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "smtp")
public class SmtpConfig {
    private String host;
    private String port;
    private String username;
    private String password;
    private String useTls;

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUseTls() {
        return useTls;
    }

    public void setUseTls(String useTls) {
        this.useTls = useTls;
    }
}
