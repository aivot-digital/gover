package de.aivot.GoverBackend.secrets.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * This class represents the configuration properties for secret management.
 * The properties are loaded from the application configuration file.
 * The key property is used for encryption and decryption of secrets.
 * The key should be a secure and random string of characters.
 */
@Configuration
@ConfigurationProperties(prefix = "secrets")
public class SecretConfigurationProperties {
    private String key;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
