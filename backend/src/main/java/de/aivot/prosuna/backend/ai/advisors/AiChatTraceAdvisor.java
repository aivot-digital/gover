package de.aivot.prosuna.backend.ai.advisors;

import de.aivot.prosuna.backend.ai.models.AiChatTraceContext;
import de.aivot.prosuna.backend.ai.services.AiChatTraceService;
import jakarta.annotation.Nonnull;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AiChatTraceAdvisor implements StreamAdvisor {
    private static final int ORDER = ToolCallingAdvisor.DEFAULT_ORDER + 100;

    private final AiChatTraceService traceService;
    private final ChatClientMessageAggregator aggregator = new ChatClientMessageAggregator();

    public AiChatTraceAdvisor(@Nonnull AiChatTraceService traceService) {
        this.traceService = traceService;
    }

    @Nonnull
    @Override
    public Flux<ChatClientResponse> adviseStream(@Nonnull ChatClientRequest request,
                                                 @Nonnull StreamAdvisorChain chain) {
        var context = AiChatTraceContext.from(request.context());
        if (context == null) {
            return chain.nextStream(request);
        }

        var round = traceService.recordModelRequest(context, request);
        var started = System.nanoTime();
        var terminalRecorded = new AtomicBoolean(false);
        var receivedResponse = new AtomicBoolean(false);
        var responses = chain.nextStream(request).doOnNext(ignored -> receivedResponse.set(true));

        return aggregator.aggregateChatClientResponse(responses, response -> {
                    if (terminalRecorded.compareAndSet(false, true)) {
                        if (receivedResponse.get()) {
                            traceService.recordModelResponse(context, round, response, elapsedMillis(started));
                        } else {
                            traceService.recordEmptyModelResponse(context, round, elapsedMillis(started));
                        }
                    }
                })
                .doOnComplete(() -> {
                    if (terminalRecorded.compareAndSet(false, true)) {
                        traceService.recordEmptyModelResponse(context, round, elapsedMillis(started));
                    }
                })
                .doOnError(error -> {
                    if (terminalRecorded.compareAndSet(false, true)) {
                        traceService.recordModelFailure(context, round, error, elapsedMillis(started));
                    }
                })
                .doFinally(signal -> {
                    if (signal == SignalType.CANCEL && terminalRecorded.compareAndSet(false, true)) {
                        traceService.recordModelCancellation(context, round, elapsedMillis(started));
                    }
                });
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Nonnull
    @Override
    public String getName() {
        return "AI Chat Trace Advisor";
    }

    private static long elapsedMillis(long started) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    }
}
