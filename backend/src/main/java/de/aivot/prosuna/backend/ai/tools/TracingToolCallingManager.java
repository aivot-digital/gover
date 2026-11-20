package de.aivot.prosuna.backend.ai.tools;

import de.aivot.prosuna.backend.ai.models.AiChatTraceContext;
import de.aivot.prosuna.backend.ai.services.AiChatTraceService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class TracingToolCallingManager implements ToolCallingManager {
    private final ToolCallingManager delegate;
    private final AiChatTraceService traceService;

    public TracingToolCallingManager(@Nonnull ToolCallingManager delegate,
                                     @Nonnull AiChatTraceService traceService) {
        this.delegate = Objects.requireNonNull(delegate);
        this.traceService = Objects.requireNonNull(traceService);
    }

    @Nonnull
    @Override
    public List<ToolDefinition> resolveToolDefinitions(@Nonnull ToolCallingChatOptions options) {
        return delegate.resolveToolDefinitions(options);
    }

    @Nonnull
    @Override
    public ToolExecutionResult executeToolCalls(@Nonnull Prompt prompt, @Nonnull ChatResponse response) {
        var context = traceContext(prompt);
        var started = System.nanoTime();
        try {
            var result = delegate.executeToolCalls(prompt, response);
            if (context != null) {
                traceService.recordToolExecution(context, response, result, elapsedMillis(started));
            }
            return result;
        } catch (RuntimeException exception) {
            if (context != null) {
                traceService.recordToolFailure(context, response, exception, elapsedMillis(started));
            }
            throw exception;
        }
    }

    @Nullable
    private AiChatTraceContext traceContext(@Nonnull Prompt prompt) {
        if (!(prompt.getOptions() instanceof ToolCallingChatOptions options) || options.getToolContext() == null) {
            return null;
        }
        return AiChatTraceContext.from(options.getToolContext());
    }

    private static long elapsedMillis(long started) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    }
}
