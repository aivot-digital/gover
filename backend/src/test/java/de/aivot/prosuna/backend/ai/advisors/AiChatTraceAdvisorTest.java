package de.aivot.prosuna.backend.ai.advisors;

import de.aivot.prosuna.backend.ai.models.AiChatTraceContext;
import de.aivot.prosuna.backend.ai.services.AiChatTraceService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class AiChatTraceAdvisorTest {
    private final AiChatTraceService traceService = mock(AiChatTraceService.class);
    private final StreamAdvisorChain chain = mock(StreamAdvisorChain.class);
    private final AiChatTraceContext traceContext = new AiChatTraceContext("owner", "session", "turn");
    private final ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt("Anfrage"))
            .context(Map.of(AiChatTraceContext.CONTEXT_KEY, traceContext))
            .build();

    @Test
    void recordsTheEffectiveRequestAndAggregatedModelResponse() {
        var response = ChatClientResponse.builder()
                .chatResponse(new ChatResponse(List.of(new Generation(new AssistantMessage("Antwort")))))
                .context(Map.of(AiChatTraceContext.CONTEXT_KEY, traceContext))
                .build();
        when(traceService.recordModelRequest(traceContext, request)).thenReturn(3);
        when(chain.nextStream(request)).thenReturn(Flux.just(response));

        var result = new AiChatTraceAdvisor(traceService).adviseStream(request, chain).collectList().block();

        assertThat(result).containsExactly(response);
        verify(traceService).recordModelResponse(eq(traceContext), eq(3), any(ChatClientResponse.class), anyLong());
        verify(traceService, never()).recordEmptyModelResponse(any(), anyInt(), anyLong());
    }

    @Test
    void recordsWhenTheProviderCompletesWithoutAResponse() {
        when(traceService.recordModelRequest(traceContext, request)).thenReturn(1);
        when(chain.nextStream(request)).thenReturn(Flux.empty());

        assertThat(new AiChatTraceAdvisor(traceService).adviseStream(request, chain).collectList().block()).isEmpty();

        verify(traceService).recordEmptyModelResponse(eq(traceContext), eq(1), anyLong());
        verify(traceService, never()).recordModelResponse(any(), anyInt(), any(), anyLong());
    }
}
