package de.aivot.prosuna.backend.ai.tools;

import de.aivot.prosuna.backend.ai.models.AiChatTraceContext;
import de.aivot.prosuna.backend.ai.services.AiChatTraceService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class TracingToolCallingManagerTest {
    private final ToolCallingManager delegate = mock(ToolCallingManager.class);
    private final AiChatTraceService traceService = mock(AiChatTraceService.class);
    private final AiChatTraceContext context = new AiChatTraceContext("owner", "session", "turn");
    private final Prompt prompt = new Prompt("Anfrage", OpenAiChatOptions.builder()
            .toolContext(Map.of(AiChatTraceContext.CONTEXT_KEY, context))
            .build());
    private final ChatResponse response = new ChatResponse(List.of(new Generation(AssistantMessage.builder()
            .toolCalls(List.of(new AssistantMessage.ToolCall("call-1", "function", "tool", "{}")))
            .build())));

    @Test
    void recordsSuccessfulToolExecution() {
        var result = mock(ToolExecutionResult.class);
        when(delegate.executeToolCalls(prompt, response)).thenReturn(result);

        new TracingToolCallingManager(delegate, traceService).executeToolCalls(prompt, response);

        verify(traceService).recordToolExecution(eq(context), eq(response), eq(result), anyLong());
    }

    @Test
    void recordsAndRethrowsToolExecutionFailure() {
        var failure = new IllegalStateException("Tool failed");
        when(delegate.executeToolCalls(prompt, response)).thenThrow(failure);

        assertThatThrownBy(() -> new TracingToolCallingManager(delegate, traceService)
                .executeToolCalls(prompt, response)).isSameAs(failure);

        verify(traceService).recordToolFailure(eq(context), eq(response), same(failure), anyLong());
    }
}
