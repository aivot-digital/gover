package de.aivot.GoverBackend.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("puppet")
@ConditionalOnEnabledHealthIndicator("puppet")
public class PuppetHealthIndicator implements HealthIndicator {
    private final Logger logger = LoggerFactory.getLogger(PuppetHealthIndicator.class);
    private final PdfService pdfService;

    @Autowired
    public PuppetHealthIndicator(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    @Override
    public Health health() {
        try {
            pdfService.testPuppetPdfConnection();
        } catch (Exception e) {
            logger.error("failed to test PDF service", e);
            return Health
                    .down()
                    .withDetail("error", "PDF Service ist fehlerhaft. Fehlermeldung: " + e.getMessage())
                    .build();
        }

        return Health
                .up()
                .build();
    }
}
