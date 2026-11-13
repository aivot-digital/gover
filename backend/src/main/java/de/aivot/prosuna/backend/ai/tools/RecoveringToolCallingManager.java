package de.aivot.prosuna.backend.ai.tools;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

public final class RecoveringToolCallingManager implements ToolCallingManager {
    private static final int MAX_ERROR_ROUNDS = 2;
    private static final String ERROR_ROUND = "prosuna.unknownToolRound";

    private final ToolCallingManager delegate;

    public RecoveringToolCallingManager(@Nonnull ToolCallingManager delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Nonnull
    @Override
    public List<ToolDefinition> resolveToolDefinitions(@Nonnull ToolCallingChatOptions options) {
        return delegate.resolveToolDefinitions(options);
    }

    @Nonnull
    @Override
    public ToolExecutionResult executeToolCalls(@Nonnull Prompt prompt, @Nonnull ChatResponse response) {
        var generation = response.getResults().stream().filter(result -> result.getOutput().hasToolCalls()).findFirst();
        if (generation.isEmpty() || !(prompt.getOptions() instanceof ToolCallingChatOptions options)) {
            return delegate.executeToolCalls(prompt, response);
        }

        var callbacks = new ArrayList<ToolCallback>();
        if (options.getToolCallbacks() != null) {
            callbacks.addAll(options.getToolCallbacks());
        }
        var availableNames = new TreeSet<String>();
        callbacks.forEach(callback -> availableNames.add(callback.getToolDefinition().name()));
        var unknownNames = new TreeSet<String>();
        generation.get().getOutput().getToolCalls().forEach(call -> {
            if (!availableNames.contains(call.name())) {
                unknownNames.add(call.name());
            }
        });
        if (unknownNames.isEmpty()) {
            return delegate.executeToolCalls(prompt, response);
        }

        int errorRounds = 0;
        for (var message : prompt.getInstructions()) {
            if (message instanceof UserMessage) {
                errorRounds = 0;
            } else if (message instanceof ToolResponseMessage && Boolean.TRUE.equals(message.getMetadata().get(ERROR_ROUND))) {
                errorRounds++;
            }
        }
        // Check the whole batch before executing any of its tools, including valid mutations.
        if (errorRounds >= MAX_ERROR_ROUNDS) {
            throw new CorrectionLimitExceededException();
        }

        for (var name : unknownNames) {
            var message = "Unbekanntes Tool „" + name + "“. "
                    + (availableNames.isEmpty() ? "Für diese Anfrage sind keine Tools verfügbar. "
                    : "Verfügbare Tools: " + String.join(", ", availableNames) + ". Verwenden Sie diese Namen unverändert. ")
                    + "Bereits erfolgreich ausgeführte Aufrufe dürfen nicht wiederholt werden.";
            callbacks.add(new UnknownToolCallback(ToolDefinition.builder().name(name)
                    .description("Meldet einen unbekannten Tool-Namen.").inputSchema("{\"type\":\"object\"}").build(), message));
        }

        // These callbacks exist only during execution; never advertise them in the next model request.
        var executionOptions = options.mutate().toolCallbacks(callbacks).build();
        var result = delegate.executeToolCalls(new Prompt(prompt.getInstructions(), executionOptions), response);
        var history = new ArrayList<>(result.conversationHistory());
        var toolResponse = (ToolResponseMessage) history.getLast();
        var metadata = new HashMap<>(toolResponse.getMetadata());
        metadata.put(ERROR_ROUND, true);
        history.set(history.size() - 1, ToolResponseMessage.builder()
                .responses(toolResponse.getResponses()).metadata(metadata).build());
        return ToolExecutionResult.builder().conversationHistory(history).returnDirect(result.returnDirect()).build();
    }

    private record UnknownToolCallback(@Nonnull ToolDefinition definition, @Nonnull String message) implements ToolCallback {
        @Nonnull
        @Override
        public ToolDefinition getToolDefinition() {
            return definition;
        }

        @Nonnull
        @Override
        public String call(@Nonnull String input) {
            return message;
        }

        @Nonnull
        @Override
        public String call(@Nonnull String input, @Nullable ToolContext context) {
            return message;
        }
    }

    public static final class CorrectionLimitExceededException extends RuntimeException {
        public CorrectionLimitExceededException() {
            super("Die Bearbeitung wurde beendet, weil wiederholt unbekannte Werkzeuge aufgerufen wurden. "
                    + "Prüfen Sie gegebenenfalls bereits ausgeführte Änderungen im aktuellen Entwurf.");
        }
    }
}
