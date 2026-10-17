package de.aivot.prosuna.backend.ai.configuration;

import com.openai.core.ClientOptions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiCommonProperties;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AiChatTimeoutConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(context -> {
                var sources = context.getEnvironment().getPropertySources();
                // Exercise the shipped defaults independently of the developer's environment.
                sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
                sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
                try {
                    new YamlPropertySourceLoader()
                            .load("application", new ClassPathResource("application.yml"))
                            .forEach(sources::addLast);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            })
            .withConfiguration(AutoConfigurations.of(OpenAiChatAutoConfiguration.class))
            .withBean(ToolCallingManager.class, () -> mock(ToolCallingManager.class))
            .withPropertyValues("spring.ai.openai.api-key=test-key",
                    "spring.ai.openai.base-url=http://localhost:11434/v1");

    @Test
    void defaultsChatClientsToTenMinutes() {
        assertClientTimeout(contextRunner, Duration.ofMinutes(10));
    }

    @Test
    void appliesConfiguredDurationToChatClients() {
        assertClientTimeout(contextRunner.withPropertyValues("PROSUNA_OPENAI_CHAT_TIMEOUT=120s"),
                Duration.ofSeconds(120));
    }

    private void assertClientTimeout(ApplicationContextRunner runner, Duration expected) {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            var model = context.getBean(OpenAiChatModel.class);
            // This verifies client defaults; AiChatRequestTimeoutTest also covers the per-request override.
            for (var field : new String[]{"openAiClient", "openAiClientAsync"}) {
                var client = ReflectionTestUtils.getField(model, field);
                assertThat(client).isNotNull();
                var options = (ClientOptions) ReflectionTestUtils.getField(client, "clientOptions");
                assertThat(options).isNotNull();
                try {
                    assertThat(options.timeout().request()).isEqualTo(expected);
                } finally {
                    options.close();
                }
            }
            assertThat(context.getBean(OpenAiCommonProperties.class).getTimeout())
                    .isEqualTo(Duration.ofSeconds(60));
        });
    }
}
