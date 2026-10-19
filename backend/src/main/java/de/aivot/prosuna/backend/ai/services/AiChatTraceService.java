package de.aivot.prosuna.backend.ai.services;

import de.aivot.prosuna.backend.ai.models.AiChatTraceContext;
import de.aivot.prosuna.backend.ai.models.AiChatAttachmentContext;
import de.aivot.prosuna.backend.ai.models.ChatContextModel;
import de.aivot.prosuna.backend.ai.permissions.AiChatPermissionProvider;
import de.aivot.prosuna.backend.ai.properties.AiChatTraceProperties;
import de.aivot.prosuna.backend.ai.repositories.AiChatSessionRepository;
import de.aivot.prosuna.backend.ai.tools.AiChatAttachmentTools;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.content.MediaContent;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AiChatTraceService {
    private static final Logger logger = LoggerFactory.getLogger(AiChatTraceService.class);
    private static final int SCHEMA_VERSION = 1;

    private final AiChatTracePersistenceService persistenceService;
    private final AiChatSessionRepository sessionRepository;
    private final PermissionService permissionService;
    private final AiChatTraceProperties properties;
    private final JsonMapper jsonMapper;
    private final Map<String, AtomicInteger> roundCounters = new ConcurrentHashMap<>();

    public AiChatTraceService(@Nonnull AiChatTracePersistenceService persistenceService,
                              @Nonnull AiChatSessionRepository sessionRepository,
                              @Nonnull PermissionService permissionService,
                              @Nonnull AiChatTraceProperties properties,
                              @Nonnull JsonMapper jsonMapper) {
        this.persistenceService = persistenceService;
        this.sessionRepository = sessionRepository;
        this.permissionService = permissionService;
        this.properties = properties;
        this.jsonMapper = jsonMapper;
    }

    @Nonnull
    public AiChatTraceContext startTurn(@Nonnull String userId,
                                        @Nonnull String sessionId,
                                        @Nonnull String userInput,
                                        @Nonnull ChatContextModel chatContext,
                                        @Nullable MultipartFile[] attachments) {
        var context = new AiChatTraceContext(userId, sessionId, UUID.randomUUID().toString());
        roundCounters.put(context.turnId(), new AtomicInteger());

        var data = new LinkedHashMap<String, Object>();
        data.put("userInput", userInput);
        data.put("appContext", chatContext.getAppContext().name());
        if (chatContext.targetRootType() != null) {
            data.put("targetRootType", Map.of(
                    "key", chatContext.targetRootType().getKey(),
                    "name", chatContext.targetRootType().name()
            ));
        }
        putIfNotNull(data, "processId", chatContext.processId());
        putIfNotNull(data, "processVersion", chatContext.processVersion());
        if (attachments != null && attachments.length > 0) {
            var attachmentData = new ArrayList<Map<String, Object>>();
            for (var attachment : attachments) {
                var item = new LinkedHashMap<String, Object>();
                putIfNotNull(item, "filename", attachment.getOriginalFilename());
                putIfNotNull(item, "contentType", attachment.getContentType());
                item.put("size", attachment.getSize());
                item.put("empty", attachment.isEmpty());
                attachmentData.add(item);
            }
            data.put("attachments", attachmentData);
        }
        append(context, "turn_started", null, data);
        return context;
    }

    public int recordModelRequest(@Nonnull AiChatTraceContext context,
                                  @Nonnull ChatClientRequest request) {
        var round = roundCounters.computeIfAbsent(context.turnId(), ignored -> new AtomicInteger())
                .incrementAndGet();
        try {
            var data = new LinkedHashMap<String, Object>();
            data.put("messages", request.prompt().getInstructions().stream().map(this::messageData).toList());
            var options = request.prompt().getOptions();
            if (options != null) {
                data.put("options", optionsData(options));
                if (options instanceof ToolCallingChatOptions toolOptions && toolOptions.getToolCallbacks() != null) {
                    data.put("tools", toolOptions.getToolCallbacks().stream().map(callback -> {
                        var definition = callback.getToolDefinition();
                        var tool = new LinkedHashMap<String, Object>();
                        tool.put("name", definition.name());
                        tool.put("description", definition.description());
                        tool.put("inputSchema", parseJson(definition.inputSchema()));
                        return tool;
                    }).toList());
                }
            }
            append(context, "model_request", round, data);
        } catch (RuntimeException exception) {
            logRecordingFailure(context, "model_request", exception);
        }
        return round;
    }

    public void recordModelResponse(@Nonnull AiChatTraceContext context,
                                    int round,
                                    @Nonnull ChatClientResponse response,
                                    long durationMillis) {
        try {
            var data = new LinkedHashMap<String, Object>();
            data.put("durationMillis", durationMillis);
            if (response.chatResponse() != null) {
                data.putAll(responseData(response.chatResponse()));
            } else {
                data.put("empty", true);
            }
            append(context, "model_response", round, data);
        } catch (RuntimeException exception) {
            logRecordingFailure(context, "model_response", exception);
        }
    }

    public void recordEmptyModelResponse(@Nonnull AiChatTraceContext context, int round, long durationMillis) {
        try {
            append(context, "model_response", round, Map.of(
                    "durationMillis", durationMillis,
                    "empty", true
            ));
        } catch (RuntimeException exception) {
            logRecordingFailure(context, "model_response", exception);
        }
    }

    public void recordModelFailure(@Nonnull AiChatTraceContext context,
                                   int round,
                                   @Nonnull Throwable error,
                                   long durationMillis) {
        try {
            append(context, "model_failure", round, Map.of(
                    "durationMillis", durationMillis,
                    "error", errorData(error)
            ));
        } catch (RuntimeException exception) {
            logRecordingFailure(context, "model_failure", exception);
        }
    }

    public void recordModelCancellation(@Nonnull AiChatTraceContext context, int round, long durationMillis) {
        try {
            append(context, "model_cancelled", round, Map.of("durationMillis", durationMillis));
        } catch (RuntimeException exception) {
            logRecordingFailure(context, "model_cancelled", exception);
        }
    }

    public void recordToolExecution(@Nonnull AiChatTraceContext context,
                                    @Nonnull ChatResponse response,
                                    @Nonnull ToolExecutionResult result,
                                    long durationMillis) {
        try {
            var data = new LinkedHashMap<String, Object>();
            data.put("durationMillis", durationMillis);
            data.put("calls", toolCalls(response));
            var responseMessages = result.conversationHistory().stream()
                    .filter(ToolResponseMessage.class::isInstance)
                    .map(ToolResponseMessage.class::cast)
                    .toList();
            data.put("responses", (responseMessages.isEmpty()
                    ? List.<ToolResponseMessage.ToolResponse>of()
                    : responseMessages.getLast().getResponses()).stream()
                    .map(this::toolResponseData)
                    .toList());
            data.put("returnDirect", result.returnDirect());
            append(context, "tool_execution", currentRound(context), data);
        } catch (RuntimeException exception) {
            logRecordingFailure(context, "tool_execution", exception);
        }
    }

    public void recordToolFailure(@Nonnull AiChatTraceContext context,
                                  @Nonnull ChatResponse response,
                                  @Nonnull Throwable error,
                                  long durationMillis) {
        try {
            append(context, "tool_failure", currentRound(context), Map.of(
                    "durationMillis", durationMillis,
                    "calls", toolCalls(response),
                    "error", errorData(error)
            ));
        } catch (RuntimeException exception) {
            logRecordingFailure(context, "tool_failure", exception);
        }
    }

    public void finishTurn(@Nonnull AiChatTraceContext context,
                           @Nonnull TurnStatus status,
                           @Nonnull String streamedContent,
                           int chunkCount,
                           long durationMillis,
                           @Nullable Throwable error) {
        try {
            var data = new LinkedHashMap<String, Object>();
            data.put("status", status.name());
            data.put("streamedContent", streamedContent);
            data.put("chunkCount", chunkCount);
            data.put("durationMillis", durationMillis);
            if (error != null) {
                data.put("error", errorData(error));
            }
            append(context, "turn_finished", null, data);
        } catch (RuntimeException exception) {
            logRecordingFailure(context, "turn_finished", exception);
        } finally {
            roundCounters.remove(context.turnId());
        }
    }

    @Nonnull
    public TraceExport exportTrace(@Nullable String userId, @Nonnull String sessionId) throws ResponseException {
        permissionService.requireSystemPermission(userId, AiChatPermissionProvider.AI_CHAT_USE);
        assert userId != null : "userId must not be null after permission check";

        var session = sessionRepository.findByUserIdAndSessionId(userId, sessionId)
                .orElseThrow(ResponseException::notFound);
        var updated = session.getUpdated();
        var cutoff = Instant.now().minus(properties.getRetentionDays(), ChronoUnit.DAYS);
        if (session.getMessages().isEmpty() || updated == null || updated.isBefore(cutoff)) {
            throw ResponseException.notFound();
        }

        var export = new LinkedHashMap<String, Object>();
        export.put("schemaVersion", SCHEMA_VERSION);
        export.put("sessionId", session.getSessionId());
        export.put("createdAt", session.getCreated());
        export.put("updatedAt", updated);
        export.put("expiresAt", updated.plus(properties.getRetentionDays(), ChronoUnit.DAYS));
        export.put("events", session.getMessages());

        try {
            return new TraceExport(
                    jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(export),
                    "prosuna-ai-chat-trace-%s.json".formatted(session.getSessionId())
            );
        } catch (JacksonException exception) {
            throw ResponseException.internalServerError("Die KI-Diagnose konnte nicht erstellt werden.", exception);
        }
    }

    private void append(@Nonnull AiChatTraceContext context,
                        @Nonnull String eventType,
                        @Nullable Integer round,
                        @Nonnull Map<String, Object> data) {
        var event = new LinkedHashMap<String, Object>();
        event.put("timestamp", Instant.now().toString());
        event.put("turnId", context.turnId());
        event.put("eventType", eventType);
        if (round != null) {
            event.put("round", round);
        }
        event.put("data", data);

        try {
            persistenceService.append(context, event);
        } catch (RuntimeException exception) {
            logger.warn("Failed to persist AI chat trace event {} for session {} and turn {}.",
                    eventType, context.sessionId(), context.turnId(), exception);
        }
    }

    private int currentRound(@Nonnull AiChatTraceContext context) {
        var counter = roundCounters.get(context.turnId());
        return counter != null ? counter.get() : 0;
    }

    @Nonnull
    private Map<String, Object> responseData(@Nonnull ChatResponse response) {
        var result = new LinkedHashMap<String, Object>();
        result.put("generations", response.getResults().stream().map(generation -> {
            var item = new LinkedHashMap<String, Object>();
            item.put("message", messageData(generation.getOutput()));
            putIfNotNull(item, "finishReason", generation.getMetadata().getFinishReason());
            if (!generation.getMetadata().getContentFilters().isEmpty()) {
                item.put("contentFilters", generation.getMetadata().getContentFilters());
            }
            if (!generation.getMetadata().isEmpty()) {
                var metadata = new LinkedHashMap<String, Object>();
                generation.getMetadata().entrySet()
                        .forEach(entry -> metadata.put(entry.getKey(), jsonValue(entry.getValue())));
                item.put("metadata", metadata);
            }
            return item;
        }).toList());

        var responseMetadata = response.getMetadata();
        var metadata = new LinkedHashMap<String, Object>();
        putIfNotBlank(metadata, "id", responseMetadata.getId());
        putIfNotBlank(metadata, "model", responseMetadata.getModel());
        var usage = responseMetadata.getUsage();
        var usageData = new LinkedHashMap<String, Object>();
        putIfNotNull(usageData, "promptTokens", usage.getPromptTokens());
        putIfNotNull(usageData, "completionTokens", usage.getCompletionTokens());
        putIfNotNull(usageData, "totalTokens", usage.getTotalTokens());
        putIfNotNull(usageData, "cacheReadInputTokens", usage.getCacheReadInputTokens());
        putIfNotNull(usageData, "cacheWriteInputTokens", usage.getCacheWriteInputTokens());
        putIfNotNull(usageData, "nativeUsage", jsonValue(usage.getNativeUsage()));
        metadata.put("usage", usageData);
        var rateLimit = responseMetadata.getRateLimit();
        var rateLimitData = new LinkedHashMap<String, Object>();
        putIfNotNull(rateLimitData, "requestsLimit", rateLimit.getRequestsLimit());
        putIfNotNull(rateLimitData, "requestsRemaining", rateLimit.getRequestsRemaining());
        putIfNotNull(rateLimitData, "requestsReset", jsonValue(rateLimit.getRequestsReset()));
        putIfNotNull(rateLimitData, "tokensLimit", rateLimit.getTokensLimit());
        putIfNotNull(rateLimitData, "tokensRemaining", rateLimit.getTokensRemaining());
        putIfNotNull(rateLimitData, "tokensReset", jsonValue(rateLimit.getTokensReset()));
        metadata.put("rateLimit", rateLimitData);
        var promptMetadata = new ArrayList<Map<String, Object>>();
        responseMetadata.getPromptMetadata().forEach(item -> {
            var normalized = new LinkedHashMap<String, Object>();
            normalized.put("promptIndex", item.getPromptIndex());
            putIfNotNull(normalized, "contentFilterMetadata", jsonValue(item.getContentFilterMetadata()));
            promptMetadata.add(normalized);
        });
        if (!promptMetadata.isEmpty()) {
            metadata.put("promptMetadata", promptMetadata);
        }
        if (!responseMetadata.isEmpty()) {
            var additional = new LinkedHashMap<String, Object>();
            responseMetadata.entrySet().forEach(entry -> additional.put(entry.getKey(), jsonValue(entry.getValue())));
            metadata.put("additional", additional);
        }
        result.put("metadata", metadata);
        return result;
    }

    @Nonnull
    private Map<String, Object> messageData(@Nonnull Message message) {
        var result = new LinkedHashMap<String, Object>();
        result.put("role", message.getMessageType().getValue());
        var attachmentContext = Boolean.TRUE.equals(
                message.getMetadata().get(AiChatAttachmentContext.MESSAGE_METADATA_KEY)
        );
        putIfNotNull(result, "content", attachmentContext
                ? message.getMetadata().get(AiChatAttachmentContext.ORIGINAL_TEXT_METADATA_KEY)
                : message.getText());
        if (attachmentContext) {
            result.put("attachmentContextRedacted", true);
        }

        var metadata = new LinkedHashMap<String, Object>();
        message.getMetadata().forEach((key, value) -> {
            if (!"messageType".equals(key)
                    && !AiChatAttachmentContext.MESSAGE_METADATA_KEY.equals(key)
                    && !AiChatAttachmentContext.ORIGINAL_TEXT_METADATA_KEY.equals(key)) {
                metadata.put(key, jsonValue(value));
            }
        });
        if (!metadata.isEmpty()) {
            result.put("metadata", metadata);
        }

        if (message instanceof AssistantMessage assistant && assistant.hasToolCalls()) {
            result.put("toolCalls", assistant.getToolCalls().stream().map(this::toolCallData).toList());
        }
        if (message instanceof ToolResponseMessage toolResponse) {
            result.put("toolResponses", toolResponse.getResponses().stream().map(this::toolResponseData).toList());
        }
        if (message instanceof MediaContent mediaContent && !mediaContent.getMedia().isEmpty()) {
            result.put("media", mediaContent.getMedia().stream().map(media -> {
                var item = new LinkedHashMap<String, Object>();
                putIfNotNull(item, "id", media.getId());
                item.put("name", media.getName());
                item.put("mimeType", media.getMimeType().toString());
                item.put("dataType", media.getData().getClass().getName());
                return item;
            }).toList());
        }
        return result;
    }

    @Nonnull
    private List<Map<String, Object>> toolCalls(@Nonnull ChatResponse response) {
        return response.getResults().stream()
                .flatMap(generation -> generation.getOutput().getToolCalls().stream())
                .map(this::toolCallData)
                .toList();
    }

    @Nonnull
    private Map<String, Object> toolCallData(@Nonnull AssistantMessage.ToolCall call) {
        var result = new LinkedHashMap<String, Object>();
        putIfNotNull(result, "id", call.id());
        putIfNotNull(result, "type", call.type());
        putIfNotNull(result, "name", call.name());
        putIfNotNull(result, "arguments", call.arguments());
        return result;
    }

    @Nonnull
    private Map<String, Object> toolResponseData(@Nonnull ToolResponseMessage.ToolResponse response) {
        var result = new LinkedHashMap<String, Object>();
        putIfNotNull(result, "id", response.id());
        putIfNotNull(result, "name", response.name());
        putIfNotNull(result, "responseData", AiChatAttachmentTools.TOOL_NAME.equals(response.name())
                ? "[Dateiinhalt ausgeblendet]"
                : response.responseData());
        return result;
    }

    @Nonnull
    private Map<String, Object> optionsData(@Nonnull ChatOptions options) {
        var result = new LinkedHashMap<String, Object>();
        result.put("type", options.getClass().getName());
        putIfNotNull(result, "model", options.getModel());
        putIfNotNull(result, "frequencyPenalty", options.getFrequencyPenalty());
        putIfNotNull(result, "maxTokens", options.getMaxTokens());
        putIfNotNull(result, "presencePenalty", options.getPresencePenalty());
        putIfNotNull(result, "stopSequences", options.getStopSequences());
        putIfNotNull(result, "temperature", options.getTemperature());
        putIfNotNull(result, "topK", options.getTopK());
        putIfNotNull(result, "topP", options.getTopP());

        if (options instanceof OpenAiChatOptions openAi) {
            putIfNotNull(result, "timeout", openAi.getTimeout() == null ? null : openAi.getTimeout().toString());
            putIfNotNull(result, "maxRetries", openAi.getMaxRetries());
            putIfNotNull(result, "logitBias", openAi.getLogitBias());
            putIfNotNull(result, "logprobs", openAi.getLogprobs());
            putIfNotNull(result, "topLogprobs", openAi.getTopLogprobs());
            putIfNotNull(result, "maxCompletionTokens", openAi.getMaxCompletionTokens());
            putIfNotNull(result, "numberOfCompletions", openAi.getN());
            putIfNotNull(result, "outputModalities", openAi.getOutputModalities());
            putIfNotNull(result, "responseFormat", jsonValue(openAi.getResponseFormat()));
            putIfNotNull(result, "streamOptions", jsonValue(openAi.getStreamOptions()));
            putIfNotNull(result, "seed", openAi.getSeed());
            putIfNotNull(result, "toolChoice", jsonValue(openAi.getToolChoice()));
            putIfNotNull(result, "user", openAi.getUser());
            putIfNotNull(result, "parallelToolCalls", openAi.getParallelToolCalls());
            putIfNotNull(result, "store", openAi.getStore());
            putIfNotNull(result, "strict", openAi.getStrict());
            putIfNotNull(result, "metadata", openAi.getMetadata());
            putIfNotNull(result, "reasoningEffort", openAi.getReasoningEffort());
            putIfNotNull(result, "verbosity", openAi.getVerbosity());
            putIfNotNull(result, "serviceTier", openAi.getServiceTier());
            putIfNotNull(result, "promptCacheKey", openAi.getPromptCacheKey());
            putIfNotNull(result, "extraBody", openAi.getExtraBody());
        }
        return result;
    }

    @Nonnull
    private Map<String, Object> errorData(@Nonnull Throwable error) {
        var result = new LinkedHashMap<String, Object>();
        result.put("type", error.getClass().getName());
        putIfNotNull(result, "message", error.getMessage());
        var cause = NestedExceptionUtils.getMostSpecificCause(error);
        result.put("rootCauseType", cause.getClass().getName());
        putIfNotNull(result, "rootCauseMessage", cause.getMessage());
        return result;
    }

    private void logRecordingFailure(@Nonnull AiChatTraceContext context,
                                     @Nonnull String eventType,
                                     @Nonnull RuntimeException exception) {
        logger.warn("Failed to build AI chat trace event {} for session {} and turn {}.",
                eventType, context.sessionId(), context.turnId(), exception);
    }

    @Nullable
    private Object jsonValue(@Nullable Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof byte[] bytes) {
            return Map.of("binaryBytes", bytes.length);
        }
        if (value instanceof Enum<?> enumeration) {
            return enumeration.name();
        }
        if (value instanceof Duration duration) {
            return duration.toString();
        }
        if (value instanceof Instant instant) {
            return instant.toString();
        }
        if (value instanceof Map<?, ?> map) {
            var result = new LinkedHashMap<String, Object>();
            map.forEach((key, item) -> result.put(String.valueOf(key), jsonValue(item)));
            return result;
        }
        if (value instanceof Iterable<?> iterable) {
            var result = new ArrayList<>();
            iterable.forEach(item -> result.add(jsonValue(item)));
            return result;
        }
        try {
            return jsonMapper.convertValue(value, Object.class);
        } catch (IllegalArgumentException exception) {
            return String.valueOf(value);
        }
    }

    @Nonnull
    private Object parseJson(@Nonnull String value) {
        try {
            return jsonMapper.readValue(value, Object.class);
        } catch (JacksonException exception) {
            return value;
        }
    }

    private static void putIfNotNull(@Nonnull Map<String, Object> target,
                                     @Nonnull String key,
                                     @Nullable Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private static void putIfNotBlank(@Nonnull Map<String, Object> target,
                                      @Nonnull String key,
                                      @Nullable String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    public enum TurnStatus {
        COMPLETED,
        EMPTY_RESPONSE,
        FAILED,
        CANCELLED
    }

    public record TraceExport(@Nonnull byte[] content, @Nonnull String filename) {
    }
}
