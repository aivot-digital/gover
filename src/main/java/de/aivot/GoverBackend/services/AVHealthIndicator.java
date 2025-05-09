package de.aivot.GoverBackend.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component("av")
@ConditionalOnEnabledHealthIndicator("av")
public class AVHealthIndicator implements HealthIndicator {
    private final Logger logger = LoggerFactory.getLogger(AVHealthIndicator.class);
    private final AVService avService;

    @Autowired
    public AVHealthIndicator(AVService avService) {
        this.avService = avService;
    }

    @Override
    public Health health() {
        var builder = Health.up();
        try {
            var avOk = avService.testServiceStatus();
            if (!avOk) {
                builder.status(Status.DOWN);
            }
        } catch (IOException e) {
            logger.error("failed to test AV service", e);
            builder.status(Status.DOWN);
            builder.withDetail("error", "Virenscanner nicht erreichbar: " + e.getMessage());
        }
        return builder.build();
    }
}
