package de.aivot.GoverBackend.secrets.services;

import de.aivot.GoverBackend.secrets.properties.SecretConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

/**
 * This service is responsible for checking the secret key on application startup.
 * The secret key is used for encryption and decryption of secrets.
 */
@Component
public class SecretStartupService implements ApplicationListener<ApplicationReadyEvent> {
    private final Logger logger = LoggerFactory.getLogger(SecretStartupService.class);

    private final SecretConfigurationProperties secretConfigurationProperties;
    private final static String INVALID_SECRET_MESSAGE = "Secret key is not set or does not fulfill the requirements. The key must be a secure and random string of at least 32 characters.";

    @Autowired
    public SecretStartupService(
            SecretConfigurationProperties secretConfigurationProperties
    ) {
        this.secretConfigurationProperties = secretConfigurationProperties;
    }

    @Override
    public void onApplicationEvent(@Nonnull ApplicationReadyEvent event) {
        // Check if a secret key is set and fulfills the requirements
        var key = secretConfigurationProperties.getKey();
        if (key == null || key.isBlank() || key.length() < 32) {
            var exception = new IllegalStateException(INVALID_SECRET_MESSAGE);
            logger
                    .atError()
                    .setMessage(INVALID_SECRET_MESSAGE)
                    .setCause(exception)
                    .log();
            throw exception;
        }
    }
}
