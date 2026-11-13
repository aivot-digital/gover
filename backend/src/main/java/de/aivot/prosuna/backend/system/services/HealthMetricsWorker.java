package de.aivot.prosuna.backend.system.services;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.actuate.endpoint.CompositeHealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Publishes each health component as 1 (up) or 0 (anything else), because Prometheus cannot read the health endpoint.
 * The interval is long on purpose: a run tests the external systems, including every storage provider.
 * <p>
 * It runs on its own thread instead of {@code @Scheduled}: the shared Spring scheduler has a single thread, so a slow
 * external system would otherwise delay the process engine jobs. It is no {@code TaskScheduler} bean on purpose,
 * because such a bean would replace the default scheduler for every {@code @Scheduled} job.
 */
@Service
public class HealthMetricsWorker {
    private static final Logger logger = LoggerFactory.getLogger(HealthMetricsWorker.class);

    private final HealthEndpoint healthEndpoint;
    private final MultiGauge componentGauge;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().name("health-metrics").daemon().factory());

    public HealthMetricsWorker(@Nonnull HealthEndpoint healthEndpoint, @Nonnull MeterRegistry meterRegistry) {
        this.healthEndpoint = healthEndpoint;
        this.componentGauge = MultiGauge.builder("prosuna.health.component.up").register(meterRegistry);
    }

    @PostConstruct
    void start() {
        // refresh() must not throw: the executor stops all further runs after an escaped exception.
        scheduler.scheduleWithFixedDelay(this::refresh, 0, 60, TimeUnit.SECONDS);
    }

    @PreDestroy
    void stop() {
        scheduler.shutdownNow();
    }

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
