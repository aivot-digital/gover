package de.aivot.prosuna.backend.system.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulingPoolConfigurationTest {
    @Test
    void jobWaitingForAnExternalSystemDoesNotDelayOtherScheduledJobs() {
        // With Spring Boot's single-thread default, e.g. a hanging payment provider would stop the process engine jobs.
        var yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        var poolSize = yaml.getObject().getProperty("spring.task.scheduling.pool.size");

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(TaskSchedulingAutoConfiguration.class))
                .withPropertyValues("spring.task.scheduling.pool.size=" + poolSize)
                .withUserConfiguration(Jobs.class)
                .run(context -> {
                    var jobs = context.getBean(Jobs.class);
                    try {
                        assertTrue(jobs.otherJobRan.await(10, TimeUnit.SECONDS));
                    } finally {
                        jobs.externalSystemAnswers.countDown();
                    }
                });
    }

    @Configuration
    @EnableScheduling
    static class Jobs {
        private final CountDownLatch waitingForExternalSystem = new CountDownLatch(1);
        private final CountDownLatch externalSystemAnswers = new CountDownLatch(1);
        private final CountDownLatch otherJobRan = new CountDownLatch(1);

        @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
        void callExternalSystem() throws InterruptedException {
            waitingForExternalSystem.countDown();
            externalSystemAnswers.await();
        }

        // Only counts once the other job blocks, so a single thread fails regardless of which job it runs first.
        @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
        void runProcessEngine() throws InterruptedException {
            if (waitingForExternalSystem.await(5, TimeUnit.SECONDS)) {
                otherJobRan.countDown();
            }
        }
    }
}
