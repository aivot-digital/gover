package de.aivot.prosuna.backend.ai.controllers;

import de.aivot.prosuna.backend.ai.advisors.AiChatTraceAdvisor;
import de.aivot.prosuna.backend.ai.data.SystemPrompts;
import de.aivot.prosuna.backend.ai.entities.AiChatSessionEntity;
import de.aivot.prosuna.backend.ai.models.AiChatTraceContext;
import de.aivot.prosuna.backend.ai.models.ChatContextModel;
import de.aivot.prosuna.backend.ai.permissions.AiChatPermissionProvider;
import de.aivot.prosuna.backend.ai.repositories.AiChatSessionRepository;
import de.aivot.prosuna.backend.ai.repositories.DatabaseChatMemoryRepository;
import de.aivot.prosuna.backend.ai.services.AiChatElementService;
import de.aivot.prosuna.backend.ai.services.AiChatTraceService;
import de.aivot.prosuna.backend.ai.tools.AiChatElementTools;
import de.aivot.prosuna.backend.ai.tools.AiChatSharedTools;
import de.aivot.prosuna.backend.ai.tools.RecoveringToolCallingManager;
import de.aivot.prosuna.backend.ai.tools.TracingToolCallingManager;
import de.aivot.prosuna.backend.av.services.AVService;
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
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
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
    private final AVService aVService;
    private final PermissionService permissionService;
    private final EmbeddingModel embeddingModel;
    private final AiChatSessionRepository aiChatSessionRepository;
    private final AiChatElementService aiChatElementService;
    private final AiChatSharedTools aiChatSharedTools;
    private final AiChatTraceService aiChatTraceService;
    private final ChatMemory chatMemory;

    AiChatController(ChatClient.Builder chatClientBuilder,
                     AiChatElementTools aiChatElementTools,
                     AVService aVService,
                     PermissionService permissionService,
                     EmbeddingModel embeddingModel,
                     AiChatSessionRepository aiChatSessionRepository,
                     AiChatElementService aiChatElementService,
                     AiChatSharedTools aiChatSharedTools,
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
        this.aVService = aVService;
        this.permissionService = permissionService;
        this.embeddingModel = embeddingModel;
        this.aiChatSessionRepository = aiChatSessionRepository;
        this.aiChatElementService = aiChatElementService;
        this.aiChatSharedTools = aiChatSharedTools;
        this.aiChatTraceService = aiChatTraceService;
        this.chatMemory = chatMemory;
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
                        return new ChatMessageResponse("user", Objects.requireNonNullElse(message.getText(), ""));
                    }
                    if (message instanceof AssistantMessage) {
                        return new ChatMessageResponse("assistant", Objects.requireNonNullElse(message.getText(), ""));
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
                    + "Files may be supplied as repeated attachments parts. "
                    + "A supplied currentState replaces the session's cached form draft and enables form editing. "
                    + "Without currentState, form editing tools are unavailable. Returns a text/event-stream.")
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
            VectorStore transientVectorStore = getTransientVectorStore(attachments);
            var toolContext = new HashMap<>(context.toMap());
            toolContext.put(AiChatTraceContext.CONTEXT_KEY, traceContext);

            var request = chatClient
                    .prompt()
                    .toolContext(toolContext)
                    .tools(aiChatSharedTools);

            if (context.getAppContext() == ChatContextModel.AppContext.FormEditor) {
                request.tools(aiChatElementTools);
            }

            var streamedContent = new StringBuilder();
            var streamedChunks = new AtomicInteger();
            var hasContent = new AtomicBoolean(false);
            var streamError = new AtomicReference<Throwable>();

            return request
                    .system(SystemPrompts.getSystemPrompt(context))
                    .user(userInput)
                    .advisors(a -> {
                        // Set the conversation ID.
                        a.param(ChatMemory.CONVERSATION_ID,
                                DatabaseChatMemoryRepository.conversationId(userId, chatSessionId));
                        a.param(AiChatTraceContext.CONTEXT_KEY, traceContext);

                        if (transientVectorStore != null) {
                            var qaAdvisor = QuestionAnswerAdvisor.builder(transientVectorStore)
                                    // Enrich the original request once, outside the tool-calling loop.
                                    .order(Ordered.HIGHEST_PRECEDENCE + 100);
                            if (context.getAppContext() == ChatContextModel.AppContext.FormEditor) {
                                qaAdvisor.promptTemplate(new PromptTemplate(SystemPrompts.ELEMENT_DOCUMENT_CONTEXT_PROMPT));
                            }
                            a.advisors(qaAdvisor.build());
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

    @Nullable
    private VectorStore getTransientVectorStore(@Nullable MultipartFile[] attachments) throws ResponseException {
        if (attachments == null || attachments.length == 0) {
            return null;
        }
        List<Document> documents = new ArrayList<>();
        aVService.testMultipartFiles(attachments);
        for (MultipartFile attachment : attachments) {
            if (attachment.isEmpty()) {
                continue;
            }
            InputStream attachmentInputStream;
            try {
                attachmentInputStream = attachment
                        .getInputStream();
            } catch (IOException e) {
                throw ResponseException.internalServerError(
                        "Failed to read attachment input stream", e
                );
            }

            // 1. Read document content from the upload stream
            var resource = new InputStreamResource(attachmentInputStream);
            var reader = new TikaDocumentReader(resource);
            List<Document> rawDocuments = reader.get();

            // 2. Split text into manageable chunks
            var textSplitter = TokenTextSplitter
                    .builder()
                    .build();
            List<Document> splitDocuments = textSplitter.split(rawDocuments);

            splitDocuments.stream()
                    .filter(document -> document.getText() != null && !document.getText().isBlank())
                    .forEach(documents::add);
        }
        if (documents.isEmpty()) {
            return null;
        }
        VectorStore transientVectorStore = SimpleVectorStore.builder(embeddingModel).build();
        transientVectorStore.add(documents);
        return transientVectorStore;
    }

    public record SessionStartResponse(String sessionId) {
    }

    public record ChatMessageResponse(@Nonnull String role, @Nonnull String content) {
    }
}
