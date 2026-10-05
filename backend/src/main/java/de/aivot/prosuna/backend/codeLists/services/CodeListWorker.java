package de.aivot.prosuna.backend.codeLists.services;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.io.Serializable;

@Service
public class CodeListWorker {
    public static final String DO_WORK_ON_CODE_LIST_QUEUE = "do-work-on-code-list-queue";

    private final CodeListService codeListService;
    private final RabbitTemplate rabbitTemplate;

    public CodeListWorker(CodeListService codeListService, RabbitTemplate rabbitTemplate) {
        this.codeListService = codeListService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Bean
    public Queue doWorkOnCodeListQueue() {
        return new Queue(DO_WORK_ON_CODE_LIST_QUEUE, true);
    }

    @RabbitListener(queues = DO_WORK_ON_CODE_LIST_QUEUE)
    public void listen(CodeListUpdateMessage message) {
        codeListService.syncCodeList(message.codeListKey(), message.keepOutdated());
    }

    public void triggerCodeListUpdate(String codeListKey, boolean keepOutdated) {
        rabbitTemplate
                .convertAndSend(
                        CodeListWorker.DO_WORK_ON_CODE_LIST_QUEUE,
                        new CodeListUpdateMessage(codeListKey, keepOutdated)
                );
    }

    public record CodeListUpdateMessage(
            String codeListKey,
            boolean keepOutdated
    ) implements Serializable {
    }
}
