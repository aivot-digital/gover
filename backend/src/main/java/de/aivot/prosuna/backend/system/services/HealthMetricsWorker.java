package de.aivot.prosuna.backend.system.services;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.actuate.endpoint.CompositeHealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Publishes each health component as 1 (up) or 0 (anything else), because Prometheus cannot read the health endpoint.
 * The interval is long on purpose: a run tests the external systems, including every storage provider.
 */
@Service
public class HealthMetricsWorker {
    private static final Logger logger = LoggerFactory.getLogger(HealthMetricsWorker.class);

    private final HealthEndpoint healthEndpoint;
    private final MultiGauge componentGauge;

    public HealthMetricsWorker(@Nonnull HealthEndpoint healthEndpoint, @Nonnull MeterRegistry meterRegistry) {
        this.healthEndpoint = healthEndpoint;
        this.componentGauge = MultiGauge.builder("prosuna.health.component.up").register(meterRegistry);
    }

    @Scheduled(fixedDelay = 60, timeUnit = TimeUnit.SECONDS)
    public void refresh() {
        try {
            if (healthEndpoint.health() instanceof CompositeHealthDescriptor health && health.getComponents() != null) {
                componentGauge.register(health.getComponents().entrySet().stream()
                        .<MultiGauge.Row<?>>map(component -> MultiGauge.Row.of(Tags.of("component", component.getKey()),
                                Status.UP.equals(component.getValue().getStatus()) ? 1 : 0))
                        .toList(), true);
            }
        } catch (RuntimeException exception) {
            // The gauge keeps its last values; the next run tries again.
            logger.warn("Could not refresh the health metrics", exception);
        }
    }
}
