package de.aivot.GoverBackend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ConnectException;

@Component("av")
@ConditionalOnEnabledHealthIndicator("av")
public class AVHealthIndicator implements HealthIndicator {
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
            builder.status(Status.DOWN);
            builder.withDetail("error", "Virenscanner nicht erreichbar: " + e.getMessage());
        }
        return builder.build();
    }
}
