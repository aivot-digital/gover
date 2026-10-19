package de.aivot.prosuna.backend.ai.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolCallLimitExceededException;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.annotation.Tool;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecoveringToolCallingManagerTest {
    private static final String READ = "hole-formularstruktur";
    private static final String TYPO = "hol-formularstruktur";
    private static final String WRITE = "erstelle-element";
    private final TestTools tools = new TestTools();
    private final OpenAiChatOptions options = OpenAiChatOptions.builder().toolCallbacks(ToolCallbacks.from(tools))
            .toolContext(Map.of("sessionId", "session")).build();
    private final Prompt initial = new Prompt(List.of(new UserMessage("Auftrag")), options);
    private final RecoveringToolCallingManager manager = new RecoveringToolCallingManager(ToolCallingManager.builder().build());

    @Test
    void returnsMatchingErrorsForEveryUnknownCallAndExecutesKnownCallsOnce() {
        var response = calls("bad-a", TYPO, "create", WRITE, "bad-b", TYPO, "bad-c", "unknown");
        var result = manager.executeToolCalls(initial, response);
        var replies = replies(result);

        assertThat(replies).extracting(ToolResponseMessage.ToolResponse::id)
                .containsExactly("bad-a", "create", "bad-b", "bad-c");
        assertThat(replies.getFirst().name()).isEqualTo(TYPO);
        assertThat(replies.getFirst().responseData()).contains(TYPO, READ, WRITE, "nicht wiederholt");
        assertThat(replies.get(1).responseData()).contains("created-id");
        assertThat(replies.get(2).responseData()).isEqualTo(replies.getFirst().responseData());
        assertThat(result.returnDirect()).isFalse();
        assertThat(tools.writes).isEqualTo(1);
        assertThat(tools.context).containsEntry("sessionId", "session");
        assertThat(result.conversationHistory().getFirst()).isSameAs(initial.getInstructions().getFirst());
        assertThat(result.conversationHistory().get(1)).isSameAs(response.getResult().getOutput());
        assertThat(options.getToolCallbacks()).hasSize(2);
        assertThat(manager.resolveToolDefinitions(options)).extracting(definition -> definition.name())
                .containsExactlyInAnyOrder(READ, WRITE);

        var corrected = manager.executeToolCalls(next(result), calls("corrected", READ));
        assertThat(replies(corrected).getFirst().responseData()).contains("structure");
        assertThat(tools.writes).isEqualTo(1);
    }

    @Test
    void countsFailedRoundsRatherThanCallsAndChecksLimitBeforeMutations() {
        var first = manager.executeToolCalls(initial, calls("a", TYPO, "b", "unknown", "c", TYPO));
        var second = manager.executeToolCalls(next(first), calls("d", TYPO));

        assertThatThrownBy(() -> manager.executeToolCalls(next(second), calls("write", WRITE, "e", TYPO)))
                .isInstanceOf(RecoveringToolCallingManager.CorrectionLimitExceededException.class);
        assertThat(tools.writes).isZero();

        var valid = manager.executeToolCalls(next(second), calls("valid", WRITE));
        assertThat(replies(valid).getFirst().responseData()).contains("created-id");
        assertThat(tools.writes).isEqualTo(1);
    }

    @Test
    void resetsAtNewUserMessageAndKeepsRequestsIndependent() {
        var first = manager.executeToolCalls(initial, calls("a", TYPO));
        var second = manager.executeToolCalls(next(first), calls("b", TYPO));
        assertThat(manager.executeToolCalls(initial, calls("independent", TYPO)).conversationHistory()).hasSize(3);

        var history = new ArrayList<>(second.conversationHistory());
        history.add(new UserMessage("Neuer Auftrag"));
        assertThat(replies(manager.executeToolCalls(new Prompt(history, options), calls("new", TYPO))))
                .hasSize(1);
        assertThatThrownBy(() -> manager.executeToolCalls(next(second), calls("old", TYPO)))
                .isInstanceOf(RecoveringToolCallingManager.CorrectionLimitExceededException.class);
    }

    @Test
    void handlesRequestsWithoutAvailableCallbacks() {
        var result = manager.executeToolCalls(new Prompt("Auftrag", OpenAiChatOptions.builder().build()), calls("a", TYPO));
        assertThat(replies(result).getFirst().responseData()).contains("keine Tools verfügbar");
    }

    @Test
    void delegatesValidCallsDefinitionsAndOtherFailuresUnchanged() {
        var delegate = mock(ToolCallingManager.class);
        var wrapper = new RecoveringToolCallingManager(delegate);
        var definitions = manager.resolveToolDefinitions(options);
        when(delegate.resolveToolDefinitions(options)).thenReturn(definitions);
        assertThat(wrapper.resolveToolDefinitions(options)).isSameAs(definitions);

        var response = calls("read", READ);
        var result = ToolExecutionResult.builder().conversationHistory(initial.getInstructions()).returnDirect(true).build();
        when(delegate.executeToolCalls(initial, response)).thenReturn(result);
        assertThat(wrapper.executeToolCalls(initial, response)).isSameAs(result);
        var error = new IllegalStateException("Tool failed");
        when(delegate.executeToolCalls(initial, response)).thenThrow(error);
        assertThatThrownBy(() -> wrapper.executeToolCalls(initial, response)).isSameAs(error);
    }

    @Test
    void preservesExistingResponseMetadataAndDoesNotMutateDelegateHistory() {
        var delegate = mock(ToolCallingManager.class);
        var original = ToolResponseMessage.builder().metadata(Map.of("existing", "value"))
                .responses(List.of(new ToolResponseMessage.ToolResponse("a", TYPO, "error"))).build();
        var history = List.<Message>of(new UserMessage("Auftrag"), calls("a", TYPO).getResult().getOutput(), original);
        doReturn(ToolExecutionResult.builder().conversationHistory(history).build()).when(delegate).executeToolCalls(any(), any());

        var result = new RecoveringToolCallingManager(delegate).executeToolCalls(initial, calls("a", TYPO));

        assertThat(result.conversationHistory().getLast().getMetadata())
                .containsEntry("existing", "value").containsEntry("prosuna.unknownToolRound", true);
        assertThat(original.getMetadata()).doesNotContainKey("prosuna.unknownToolRound");
    }

    @Test
    void preservesDelegateExecutionLimitsWithoutRetryingPartialWork() {
        var limited = new RecoveringToolCallingManager(ToolCallingManager.builder().maxTotalToolCalls(1).build());

        assertThatThrownBy(() -> limited.executeToolCalls(initial, calls("write", WRITE, "bad", TYPO)))
                .isInstanceOf(ToolCallLimitExceededException.class);
        assertThat(tools.writes).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void recoversWithinTheSameToolLoopWithoutAdvertisingErrorCallbacks(boolean streaming) {
        var model = mock(ChatModel.class, CALLS_REAL_METHODS);
        when(model.getOptions()).thenReturn(OpenAiChatOptions.builder().build());
        var prompts = new ArrayList<Prompt>();
        var answer = (org.mockito.stubbing.Answer<ChatResponse>) invocation -> {
            prompts.add(invocation.getArgument(0));
            return switch (prompts.size()) {
                case 1 -> calls("write", WRITE, "bad", TYPO);
                case 2 -> calls("corrected", READ);
                default -> new ChatResponse(List.of(new Generation(new AssistantMessage("Fertig"))));
            };
        };
        doAnswer(answer).when(model).call(any(Prompt.class));
        doAnswer(invocation -> Flux.just(answer.answer(invocation))).when(model).stream(any(Prompt.class));
        var client = ChatClient.builder(model).defaultAdvisors(ToolCallingAdvisor.builder()
                .toolCallingManager(manager).build()).build();
        var request = client.prompt().user("Auftrag").options(options.mutate());

        var result = streaming ? request.stream().content().collectList().block(Duration.ofSeconds(10)).getLast()
                : request.call().content();

        assertThat(result).isEqualTo("Fertig");
        assertThat(tools.writes).isEqualTo(1);
        assertThat(prompts).hasSize(3).allSatisfy(prompt ->
                assertThat(((OpenAiChatOptions) prompt.getOptions()).getToolCallbacks())
                        .extracting(callback -> callback.getToolDefinition().name()).containsExactlyInAnyOrder(READ, WRITE));
        assertThat(((ToolResponseMessage) prompts.get(1).getInstructions().getLast()).getResponses())
                .extracting(ToolResponseMessage.ToolResponse::id).containsExactly("write", "bad");
    }

    private Prompt next(ToolExecutionResult result) {
        return new Prompt(result.conversationHistory(), options);
    }

    private List<ToolResponseMessage.ToolResponse> replies(ToolExecutionResult result) {
        return ((ToolResponseMessage) result.conversationHistory().getLast()).getResponses();
    }

    private static ChatResponse calls(String... idsAndNames) {
        var calls = new ArrayList<AssistantMessage.ToolCall>();
        for (int i = 0; i < idsAndNames.length; i += 2) {
            calls.add(new AssistantMessage.ToolCall(idsAndNames[i], "function", idsAndNames[i + 1], "{}"));
        }
        return new ChatResponse(List.of(new Generation(AssistantMessage.builder().toolCalls(calls).build())));
    }

    public static class TestTools {
        int writes;
        Map<String, Object> context;

        @Tool(name = READ, description = "Read")
        public String read() {
            return "structure";
        }

        @Tool(name = WRITE, description = "Create", returnDirect = true)
        public String create(ToolContext context) {
            writes++;
            this.context = context.getContext();
            return "created-id";
        }
    }
}
