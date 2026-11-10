package de.aivot.prosuna.backend.staffEvents.services;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.staffEvents.models.StaffEvent;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StaffEventServiceTest {
    private static final StaffEvent INSTANCE_7_CHANGED = new StaffEvent("process-instance", "7", null);
    private static final StaffEvent INSTANCE_8_CHANGED = new StaffEvent("process-instance", "8", null);

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final UserRepository users = mock(UserRepository.class);
    private final StaffEventService service = new StaffEventService(rabbitTemplate, JsonMapperTestUtils.createMapper(),
            List.of(), users);

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void publishSendsTheEventToEveryApplicationInstance() {
        service.publish(INSTANCE_7_CHANGED);

        verify(rabbitTemplate).convertAndSend(StaffEventService.EXCHANGE, "",
                "{\"type\":\"process-instance\",\"scope\":\"7\",\"userId\":null}");
    }

    @Test
    void publishSendsTheDistinctEventsOfATransactionOnceItCommits() {
        beginTransaction();

        service.publish(INSTANCE_7_CHANGED);
        service.publish(INSTANCE_8_CHANGED);
        service.publish(INSTANCE_7_CHANGED);
        verifyNoInteractions(rabbitTemplate);

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(rabbitTemplate).convertAndSend(StaffEventService.EXCHANGE, "",
                "{\"type\":\"process-instance\",\"scope\":\"7\",\"userId\":null}");
        verify(rabbitTemplate).convertAndSend(StaffEventService.EXCHANGE, "",
                "{\"type\":\"process-instance\",\"scope\":\"8\",\"userId\":null}");
    }

    @Test
    void publishSendsNothingWhenTheTransactionRollsBack() {
        beginTransaction();

        service.publish(INSTANCE_7_CHANGED);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void publishDoesNotFailTheCallerWhenTheBrokerIsUnavailable() {
        doThrow(new AmqpException("unavailable")).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        assertDoesNotThrow(() -> service.publish(INSTANCE_7_CHANGED));
    }

    @Test
    void sendHeartbeatsKeepsAStreamThatOpensWhileTheUsersAreChecked() {
        service.subscribe("checked");
        var opened = new AtomicReference<SseEmitter>();
        when(users.findActiveIdsByIdIn(anyCollection())).thenAnswer(invocation -> {
            opened.set(service.subscribe("unchecked"));
            return List.of("checked");
        });

        service.sendHeartbeats();

        // A completed stream rejects every further event.
        assertDoesNotThrow(() -> opened.get().send(SseEmitter.event().comment("")));
    }

    private static void beginTransaction() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }
}
