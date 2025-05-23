package de.aivot.GoverBackend.services;

import de.aivot.GoverBackend.services.storages.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("s3")
@ConditionalOnEnabledHealthIndicator("s3")
public class S3HealthIndicator implements HealthIndicator {
    private final Logger logger = LoggerFactory.getLogger(S3HealthIndicator.class);

    private final StorageService storageService;

    @Autowired
    public S3HealthIndicator(StorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public Health health() {
        if (storageService.isLocalStorageEnabled()) {
            return Health
                    .unknown()
                    .withDetail("hint", "Externer Datenspeicher ist nicht aktiviert. Lokaler Datenspeicher wird verwendet.")
                    .build();
        }

        try {
            storageService.testRemoteStorageBucketExists();
        } catch (Exception e) {
            logger
                    .warn("failed to test remote data store", e);
            return Health
                    .down()
                    .withDetail("error", "Externer Datenspeicher ist fehlerhaft. Fehlermeldung: " + e.getMessage())
                    .build();
        }

        return Health
                .up()
                .build();
    }
}
