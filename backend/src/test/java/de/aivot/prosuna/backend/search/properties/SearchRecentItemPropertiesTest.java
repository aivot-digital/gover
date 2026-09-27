package de.aivot.prosuna.backend.search.properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class SearchRecentItemPropertiesTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void bindsProperties() {
        contextRunner
                .withPropertyValues(
                        "prosuna.search.recent.max-items-per-user=25",
                        "prosuna.search.recent.retention-days=180"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(SearchRecentItemProperties.class).getMaxItemsPerUser()).isEqualTo(25);
                    assertThat(context.getBean(SearchRecentItemProperties.class).getRetentionDays()).isEqualTo(180);
                });
    }

    @Test
    void rejectsNonPositiveValues() {
        contextRunner
                .withPropertyValues(
                        "prosuna.search.recent.max-items-per-user=0",
                        "prosuna.search.recent.retention-days=180"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SearchRecentItemProperties.class)
    static class TestConfiguration {
    }
}
