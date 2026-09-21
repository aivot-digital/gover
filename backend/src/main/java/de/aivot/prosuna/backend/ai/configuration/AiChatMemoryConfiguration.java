package de.aivot.prosuna.backend.ai.configuration;

import de.aivot.prosuna.backend.ai.repositories.DatabaseChatMemoryRepository;
import jakarta.annotation.Nonnull;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiChatMemoryConfiguration {
    @Bean
    @Nonnull
    public ChatMemory chatMemory(@Nonnull DatabaseChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder().chatMemoryRepository(repository).maxMessages(20).build();
    }
}
