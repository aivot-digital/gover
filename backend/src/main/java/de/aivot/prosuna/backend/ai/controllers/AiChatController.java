package de.aivot.prosuna.backend.ai.controllers;

import de.aivot.prosuna.backend.ai.advisors.AiChatTraceAdvisor;
import de.aivot.prosuna.backend.ai.advisors.AiChatAttachmentAdvisor;
import de.aivot.prosuna.backend.ai.data.SystemPrompts;
import de.aivot.prosuna.backend.ai.entities.AiChatSessionEntity;
import de.aivot.prosuna.backend.ai.models.AiChatTraceContext;
import de.aivot.prosuna.backend.ai.models.AiChatAttachmentContext;
import de.aivot.prosuna.backend.ai.models.AiChatAttachmentMetadata;
import de.aivot.prosuna.backend.ai.models.ChatContextModel;
import de.aivot.prosuna.backend.ai.models.AiProcessChatContext;
import de.aivot.prosuna.backend.ai.services.AiChatProcessService;
import de.aivot.prosuna.backend.ai.services.AiChatAttachmentService;
import de.aivot.prosuna.backend.ai.tools.AiChatAttachmentTools;
import de.aivot.prosuna.backend.ai.tools.AiChatProcessTools;
import de.aivot.prosuna.backend.ai.tools.AiChatElementSchemaTools;
import de.aivot.prosuna.backend.ai.permissions.AiChatPermissionProvider;
import de.aivot.prosuna.backend.ai.repositories.AiChatSessionRepository;
import de.aivot.prosuna.backend.ai.repositories.DatabaseChatMemoryRepository;
import de.aivot.prosuna.backend.ai.services.AiChatElementService;
import de.aivot.prosuna.backend.ai.services.AiChatTraceService;
import de.aivot.prosuna.backend.ai.tools.AiChatElementTools;
import de.aivot.prosuna.backend.ai.tools.AiChatSharedTools;
import de.aivot.prosuna.backend.ai.tools.RecoveringToolCallingManager;
import de.aivot.prosuna.backend.ai.tools.TracingToolCallingManager;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.user.services.UserService;
import de.aivot.prosuna.backend.utils.RandomUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


@RestController
@RequestMapping("/api/ai/chat/")
@Tag(name = "AI Chat")
@SecurityRequirement(name = OpenApiConfiguration.Security)
class AiChatController {
    private final ChatClient chatClient;
    private final AiChatElementTools aiChatElementTools;
    private final PermissionService permissionService;
    private final AiChatSessionRepository aiChatSessionRepository;
    private final AiChatElementService aiChatElementService;
    private final AiChatSharedTools aiChatSharedTools;
    private final AiChatTraceService aiChatTraceService;
    private final ChatMemory chatMemory;
    private final AiChatProcessService aiChatProcessService;
    private final AiChatProcessTools aiChatProcessTools;
    private final AiChatElementSchemaTools aiChatElementSchemaTools;
    private final AiChatAttachmentService aiChatAttachmentService;
    private final AiChatAttachmentTools aiChatAttachmentTools;

    AiChatController(ChatClient.Builder chatClientBuilder,
                     AiChatElementTools aiChatElementTools,
                     PermissionService permissionService,
                     AiChatSessionRepository aiChatSessionRepository,
                     AiChatElementService aiChatElementService,
                     AiChatSharedTools aiChatSharedTools,
                     AiChatElementSchemaTools aiChatElementSchemaTools,
                     AiChatProcessTools aiChatProcessTools,
                     AiChatProcessService aiChatProcessService,
                     AiChatAttachmentService aiChatAttachmentService,
                     AiChatAttachmentTools aiChatAttachmentTools,
                     @Nonnull AiChatTraceService aiChatTraceService,
                     @Nonnull ToolCallingManager toolCallingManager,
                     @Nonnull ChatMemory chatMemory,
                     @Nonnull @Value("${spring.ai.openai.chat.timeout}") Duration chatTimeout) {
        // Spring AI 2.0.1 otherwise overrides the client's timeout with the model options' one-minute default.
        this.chatClient = chatClientBuilder
                .defaultOptions(OpenAiChatOptions.builder().timeout(chatTimeout))
                .defaultAdvisors(
                        // Remember the original user text before document enrichment, outside the tool loop.
                        MessageChatMemoryAdvisor.builder(chatMemory).order(Ordered.HIGHEST_PRECEDENCE + 50).build(),
                        ToolCallingAdvisor.builder()
                                .toolCallingManager(new TracingToolCallingManager(
                                        new RecoveringToolCallingManager(toolCallingManager), aiChatTraceService
                                )).build(),
                        new AiChatTraceAdvisor(aiChatTraceService))
                .build();
        this.aiChatElementTools = aiChatElementTools;
        this.permissionService = permissionService;
        this.aiChatSessionRepository = aiChatSessionRepository;
        this.aiChatElementService = aiChatElementService;
        this.aiChatSharedTools = aiChatSharedTools;
        this.aiChatTraceService = aiChatTraceService;
        this.chatMemory = chatMemory;
        this.aiChatElementSchemaTools = aiChatElementSchemaTools;
        this.aiChatProcessTools = aiChatProcessTools;
        this.aiChatProcessService = aiChatProcessService;
        this.aiChatAttachmentService = aiChatAttachmentService;
        this.aiChatAttachmentTools = aiChatAttachmentTools;
    }

    @PostMapping("start/")
    public SessionStartResponse startChatSession(
            @Nonnull @AuthenticationPrincipal final Jwt jwt
    ) throws ResponseException {
        String userId = UserService
                .getIdFromJWT(jwt);
        assert userId != null;

        permissionService
                .requireSystemPermission(userId, AiChatPermissionProvider.AI_CHAT_USE);

        AiChatSessionEntity chatSessionEntity = new AiChatSessionEntity()
                .setUserId(userId)
                .setSessionId(RandomUtils.generateRandomString(32));

        var sessionId = aiChatSessionRepository
                .save(chatSessionEntity)
                .getSessionId();

        return new SessionStartResponse(sessionId);
    }

    @Nonnull
    @GetMapping("element/")
    @Operation(summary = "Retrieve the cached form draft of an owned chat session",
            description = "Requires ai_chat.use. Returns 404 if the session or cached form draft is unavailable.")
    public BaseElement getCurrentElement(
            @Nonnull @AuthenticationPrincipal final Jwt jwt,
            @Nonnull @RequestParam final String chatSessionId
    ) throws ResponseException {
        return aiChatElementService
                .getCurrentElement(UserService.getIdFromJWT(jwt), chatSessionId);
    }

    @Nonnull
    @GetMapping("messages/")
    @Operation(summary = "Retrieve the visible messages of an owned chat session",
            description = "Requires ai_chat.use. Returns at most the 20 messages retained for the model context.")
    public List<ChatMessageResponse> getMessages(
            @Nonnull @AuthenticationPrincipal final Jwt jwt,
            @Nonnull @RequestParam final String chatSessionId
    ) throws ResponseException {
        var userId = UserService.getIdFromJWT(jwt);
        permissionService.requireSystemPermission(userId, AiChatPermissionProvider.AI_CHAT_USE);
        if (aiChatSessionRepository.findByUserIdAndSessionId(userId, chatSessionId).isEmpty()) {
            throw ResponseException.notFound();
        }
        return chatMemory.get(DatabaseChatMemoryRepository.conversationId(userId, chatSessionId)).stream()
                .map(message -> {
                    if (message instanceof UserMessage) {
                        return new ChatMessageResponse(
                                "user",
                                Objects.requireNonNullElse(message.getText(), ""),
                                attachmentMetadata(message.getMetadata().get(AiChatAttachmentContext.ATTACHMENTS_METADATA_KEY))
                        );
                    }
                    if (message instanceof AssistantMessage) {
                        return new ChatMessageResponse("assistant", Objects.requireNonNullElse(message.getText(), ""), List.of());
                    }
                    throw new IllegalStateException("Unsupported persisted chat message type");
                })
                .toList();
    }

    @Nonnull
    @GetMapping(value = "trace/", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Download the AI execution trace of an owned chat session",
            description = "Requires ai_chat.use. The trace can contain prompts, model responses and tool data.")
    public ResponseEntity<ByteArrayResource> downloadTrace(
            @Nonnull @AuthenticationPrincipal final Jwt jwt,
            @Nonnull @RequestParam final String chatSessionId
    ) throws ResponseException {
        var trace = aiChatTraceService.exportTrace(UserService.getIdFromJWT(jwt), chatSessionId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .contentLength(trace.content().length)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(trace.filename(), StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(new ByteArrayResource(trace.content()));
    }

    @PostMapping(value = "send/", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Chat in an owned session",
            description = "Requires ai_chat.use. Accepts multipart form fields chatSessionId, userInput, "
                    + "and optional processId and processVersion. Optional application/json parts targetRootType "
                    + "and currentState contain the numeric form element type and the unsaved form draft respectively. "
                    + "One supported text document may be supplied as an attachments part. "
                    + "A supplied currentState replaces the session's cached form draft and enables form editing. "
                    + "Without currentState, form editing tools are unavailable. processId and processVersion must be paired "
                    + "and cannot be combined with form editing. Process tools require process_definition.read, "
                    + "and mutations additionally process_definition.update and Drafted status. "
                    + "Process changes are persisted immediately. Returns a text/event-stream.")
    public Flux<String> send(
            @Nonnull @AuthenticationPrincipal final Jwt jwt,
            @Nonnull @RequestParam(required = true) final String chatSessionId,
            @Nonnull @RequestParam final String userInput,
            @Nullable @RequestPart(required = false) final ElementType targetRootType,
            @Nullable @RequestPart(required = false) final BaseElement currentState,
            @Nullable @RequestParam(required = false) final Integer processId,
            @Nullable @RequestParam(required = false) final Integer processVersion,
            @Nullable @RequestPart(required = false) final MultipartFile[] attachments
    ) throws ResponseException {
        String userId = UserService
                .getIdFromJWT(jwt);
        assert userId != null;

        if ((processId == null) != (processVersion == null)) {
            throw ResponseException.badRequest("Prozess-ID und Prozessversion müssen gemeinsam angegeben werden.");
        }
        if (processId != null && (currentState != null || targetRootType != null)) {
            throw ResponseException.badRequest("Formular- und Prozessbearbeitung können nicht in derselben Anfrage kombiniert werden.");
        }
        var processContext = processId == null ? null : new AiProcessChatContext(userId, chatSessionId, processId, processVersion);
        if (processContext != null) aiChatProcessService.requireContext(processContext);

        var effectiveRootType = aiChatElementService
                .cacheCurrentElement(userId, chatSessionId, targetRootType, currentState);

        var context = new ChatContextModel(
                chatSessionId,
                effectiveRootType,
                processId,
                processVersion
        );

        var traceContext = aiChatTraceService.startTurn(userId, chatSessionId, userInput, context, attachments);
        var turnStarted = System.nanoTime();
        var traceFinished = new AtomicBoolean(false);

        try {
            var attachmentContext = aiChatAttachmentService.prepare(attachments);
            var toolContext = new HashMap<>(context.toMap());
            toolContext.put(AiChatTraceContext.CONTEXT_KEY, traceContext);
            if (processContext != null) toolContext.put(AiProcessChatContext.KEY, processContext);
            if (attachmentContext != null) toolContext.put(AiChatAttachmentContext.CONTEXT_KEY, attachmentContext);

            var request = chatClient
                    .prompt()
                    .toolContext(toolContext)
                    .tools(aiChatSharedTools);

            if (attachmentContext != null) {
                request.tools(aiChatAttachmentTools);
            }

            if (context.getAppContext() == ChatContextModel.AppContext.FormEditor) {
                request.tools(aiChatElementSchemaTools, aiChatElementTools);
            } else if (context.getAppContext() == ChatContextModel.AppContext.ProcessEditor) {
                request.tools(aiChatElementSchemaTools, aiChatProcessTools);
            }

            var streamedContent = new StringBuilder();
            var streamedChunks = new AtomicInteger();
            var hasContent = new AtomicBoolean(false);
            var streamError = new AtomicReference<Throwable>();

            return request
                    .system(SystemPrompts.getSystemPrompt(context))
                    .user(user -> {
                        user.text(userInput);
                        if (attachmentContext != null) {
                            user.metadata(
                                    AiChatAttachmentContext.ATTACHMENTS_METADATA_KEY,
                                    List.of(attachmentContext.metadata())
                            );
                        }
                    })
                    .advisors(a -> {
                        // Set the conversation ID.
                        a.param(ChatMemory.CONVERSATION_ID,
                                DatabaseChatMemoryRepository.conversationId(userId, chatSessionId));
                        a.param(AiChatTraceContext.CONTEXT_KEY, traceContext);

                        if (attachmentContext != null) {
                            a.param(AiChatAttachmentContext.CONTEXT_KEY, attachmentContext);
                            a.advisors(new AiChatAttachmentAdvisor());
                        }
                    })
                    .stream()
                    .content()
                    .doOnError(streamError::set)
                    .onErrorResume(error -> {
                        var cause = NestedExceptionUtils.getMostSpecificCause(error);
                        if (cause instanceof RecoveringToolCallingManager.CorrectionLimitExceededException) {
                            return Flux.just("\n\n" + cause.getMessage());
                        }
                        return Flux.error(error);
                    })
                    .mapNotNull(s -> {
                        // SSE removes leading spaces from the content, which can break formatting.
                        // This is a workaround to preserve leading spaces.
                        if (s.startsWith(" ")) {
                            return " " + s;
                        }
                        return s;
                    })
                    .doOnNext(chunk -> {
                        streamedChunks.incrementAndGet();
                        streamedContent.append(chunk);
                        if (!chunk.isBlank()) {
                            hasContent.set(true);
                        }
                    })
                    .doFinally(signal -> {
                        if (!traceFinished.compareAndSet(false, true)) {
                            return;
                        }
                        var error = streamError.get();
                        var status = signal == SignalType.CANCEL
                                ? AiChatTraceService.TurnStatus.CANCELLED
                                : error != null
                                ? AiChatTraceService.TurnStatus.FAILED
                                : hasContent.get()
                                ? AiChatTraceService.TurnStatus.COMPLETED
                                : AiChatTraceService.TurnStatus.EMPTY_RESPONSE;
                        aiChatTraceService.finishTurn(
                                traceContext,
                                status,
                                streamedContent.toString(),
                                streamedChunks.get(),
                                elapsedMillis(turnStarted),
                                error
                        );
                    });
        } catch (ResponseException | RuntimeException exception) {
            if (traceFinished.compareAndSet(false, true)) {
                aiChatTraceService.finishTurn(
                        traceContext,
                        AiChatTraceService.TurnStatus.FAILED,
                        "",
                        0,
                        elapsedMillis(turnStarted),
                        exception
                );
            }
            throw exception;
        }
    }

    private static long elapsedMillis(long started) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    }

    @Nonnull
    private static List<AiChatAttachmentMetadata> attachmentMetadata(@Nullable Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        return values.stream().map(item -> {
            if (item instanceof AiChatAttachmentMetadata metadata) {
                return metadata;
            }
            if (item instanceof java.util.Map<?, ?> map
                    && map.get("name") instanceof String name
                    && map.get("size") instanceof Number size) {
                var contentType = map.get("contentType") instanceof String type ? type : null;
                return new AiChatAttachmentMetadata(name, size.longValue(), contentType);
            }
            return null;
        }).filter(Objects::nonNull).toList();
    }

    public record SessionStartResponse(String sessionId) {
    }

    public record ChatMessageResponse(
            @Nonnull String role,
            @Nonnull String content,
            @Nonnull List<AiChatAttachmentMetadata> attachments
    ) {
    }
}
