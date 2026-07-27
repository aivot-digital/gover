package de.aivot.gover.backend.secrets.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * This class represents the configuration properties for secret management. The properties are loaded from the application configuration file. The key property is used for
 * encryption and decryption of secrets. The key should be a secure and random string of characters.
 */
@Component
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
