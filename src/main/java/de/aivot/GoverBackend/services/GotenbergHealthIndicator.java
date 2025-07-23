package de.aivot.GoverBackend.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("gotenberg")
@ConditionalOnEnabledHealthIndicator("gotenberg")
public class GotenbergHealthIndicator implements HealthIndicator {
    private static final Logger logger = LoggerFactory.getLogger(GotenbergHealthIndicator.class);
    private final PdfService pdfService;

    @Autowired
    public GotenbergHealthIndicator(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    @Override
    public Health health() {
        try {
            pdfService.testGotenbergConnection();
            return Health.up().build();
        } catch (Exception e) {
            logger.error("Health check for Gotenberg PDF failed", e);
            String message = e.getMessage();
            if (message == null || message.isBlank()) {
                message = e.getClass().getSimpleName();
            }

            return Health
                    .down()
                    .withDetail("error", "PDF Service ist fehlerhaft. Fehlermeldung: " + message)
                    .build();
        }
    }
}
