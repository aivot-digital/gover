package de.aivot.prosuna.backend.core.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "prosuna.http")
public class HttpServiceProperties {
    private long connectionTimeoutSeconds;
    private long readTimeoutSeconds;

    public long getConnectionTimeoutSeconds() {
        return connectionTimeoutSeconds;
    }

    public void setConnectionTimeoutSeconds(long connectionTimeoutSeconds) {
        this.connectionTimeoutSeconds = connectionTimeoutSeconds;
    }

    public long getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    public void setReadTimeoutSeconds(long readTimeoutSeconds) {
        this.readTimeoutSeconds = readTimeoutSeconds;
    }
}
