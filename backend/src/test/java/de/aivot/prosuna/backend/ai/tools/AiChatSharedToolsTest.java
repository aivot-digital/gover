package de.aivot.prosuna.backend.ai.tools;

import de.aivot.prosuna.backend.ai.models.ChatContextModel;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.enums.ElementType;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiChatSharedToolsTest {
    @ParameterizedTest
    @MethodSource("chatModes")
    void returnsGermanModeThroughRegisteredCallback(@Nonnull ChatContextModel context, @Nonnull String expected) {
        var callback = Arrays.stream(ToolCallbacks.from(new AiChatSharedTools()))
                .filter(tool -> tool.getToolDefinition().name().equals("hole-chatmodus"))
                .findFirst().orElseThrow();

        var result = callback.call("{}", new ToolContext(context.toMap()));

        assertEquals(expected, JsonMapperTestUtils.createMapper().readTree(result).asString());
    }

    private static Stream<Arguments> chatModes() {
        return Stream.of(
                Arguments.of(new ChatContextModel("session", ElementType.GroupLayout, null, null), "Formular"),
                Arguments.of(new ChatContextModel("session", null, 42, 1), "Prozess"),
                Arguments.of(new ChatContextModel("session", null, null, null), "Allgemein"),
                Arguments.of(new ChatContextModel("session", ElementType.GroupLayout, 42, 1), "Formular")
        );
    }
}
