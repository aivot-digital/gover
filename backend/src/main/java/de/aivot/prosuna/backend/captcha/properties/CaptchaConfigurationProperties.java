package de.aivot.prosuna.backend.captcha.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * This class represents the configuration properties for the captcha integration.
 * The properties are loaded from the application configuration file.
 * The key property is used for encryption and decryption of secrets.
 * The key should be a secure and random string of characters.
 */
@Component
@ConfigurationProperties(prefix = "captcha")
public class CaptchaConfigurationProperties {
    private String key;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
