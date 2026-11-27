package de.aivot.prosuna.backend.lib.services;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class StoredWorkSignalsTest {
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final StoredWorkSignals signals = new StoredWorkSignals(rabbit);

    @Test
    void reportsWhetherTheBrokerAcceptedTheSignal() {
        doThrow(new AmqpConnectException(new IllegalStateException("broker unavailable"))).when(rabbit)
                .convertAndSend("queue", 2L);

        assertTrue(signals.publish("queue", 1L));
        assertFalse(signals.publish("queue", 2L));
    }

    @Test
    void republishesOnlyClaimedWorkAndContinuesAfterAFailedSignal() {
        doThrow(new AmqpConnectException(new IllegalStateException("broker unavailable"))).when(rabbit)
                .convertAndSend("queue", "a");

        signals.republish("queue", List.of("a", "b", "c"), key -> !key.equals("b"));

        verify(rabbit).convertAndSend("queue", "a");
        verify(rabbit, never()).convertAndSend("queue", "b");
        verify(rabbit).convertAndSend("queue", "c");
    }
}
