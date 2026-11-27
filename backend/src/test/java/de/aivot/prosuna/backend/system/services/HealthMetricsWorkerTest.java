package de.aivot.prosuna.backend.system.services;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthMetricsWorkerTest {
    @Test
    void runsTheHealthChecksOnItsOwnThread() throws Exception {
        // A slow external system must not hold up the shared Spring scheduler that runs the process engine jobs.
        var healthEndpoint = mock(HealthEndpoint.class);
        var checkingThread = new CompletableFuture<String>();
        var release = new CountDownLatch(1);
        when(healthEndpoint.health()).thenAnswer(invocation -> {
            checkingThread.complete(Thread.currentThread().getName());
            release.await();
            return null;
        });
        var worker = new HealthMetricsWorker(healthEndpoint, new SimpleMeterRegistry());

        worker.start();
        try {
            assertEquals("health-metrics", checkingThread.get(5, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            worker.stop();
        }
    }
}
