package de.aivot.prosuna.backend.ai.advisors;

import de.aivot.prosuna.backend.ai.models.AiChatAttachmentContext;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;

import java.util.ArrayList;

public class AiChatAttachmentAdvisor implements BaseAdvisor {
    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        var value = request.context().get(AiChatAttachmentContext.CONTEXT_KEY);
        if (!(value instanceof AiChatAttachmentContext attachmentContext)) {
            return request;
        }

        var messages = new ArrayList<Message>(request.prompt().getInstructions());
        for (int index = messages.size() - 1; index >= 0; index--) {
            if (messages.get(index) instanceof UserMessage userMessage) {
                var metadata = new java.util.HashMap<>(userMessage.getMetadata());
                metadata.put(AiChatAttachmentContext.MESSAGE_METADATA_KEY, true);
                var text = userMessage.getText() == null ? "" : userMessage.getText();
                metadata.put(AiChatAttachmentContext.ORIGINAL_TEXT_METADATA_KEY, text);
                messages.set(index, userMessage.mutate()
                        .text(text + "\n\n" + attachmentContext.initialContext())
                        .metadata(metadata)
                        .build());
                break;
            }
        }

        return request.mutate()
                .prompt(new Prompt(messages, request.prompt().getOptions()))
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
        return response;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
