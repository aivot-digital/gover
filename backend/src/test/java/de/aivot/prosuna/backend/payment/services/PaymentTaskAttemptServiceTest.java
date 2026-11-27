package de.aivot.prosuna.backend.payment.services;

import de.aivot.prosuna.backend.enums.XBezahldienstStatus;
import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.prosuna.backend.payment.models.PaymentTaskRuntimeDataKeys;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentInformation;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEventEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceHistoryEventRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PaymentTaskAttemptServiceTest {
    private static final long INSTANCE_ID = 7L;
    private static final int NODE_ID = 3;

    private final ProcessInstanceTaskRepository tasks = mock(ProcessInstanceTaskRepository.class);
    private final PaymentTransactionService transactions = mock(PaymentTransactionService.class);
    private final PaymentTaskAttemptService service = new PaymentTaskAttemptService(tasks, transactions);

    @Test
    void findsThePaymentOfTheRestartedAttempt() {
        var earlier = stored(attempt(1L, null, "tx-1"));
        var paid = transaction("tx-1", XBezahldienstStatus.PAYED);

        var result = service.findPaidEarlierAttempt(attempt(2L, 1L, null)).orElseThrow();

        assertSame(earlier, result.attempt());
        assertSame(paid, result.transaction());
    }

    @Test
    void keepsSearchingTheRestartChainPastAttemptsWithoutAPayment() {
        stored(attempt(3L, 2L, "tx-3"));
        transaction("tx-3", XBezahldienstStatus.CANCELED);
        stored(attempt(2L, 1L, null));
        var earliest = stored(attempt(1L, null, "tx-1"));
        transaction("tx-1", XBezahldienstStatus.PAYED);

        var result = service.findPaidEarlierAttempt(attempt(4L, 3L, null)).orElseThrow();

        assertSame(earliest, result.attempt());
    }

    @Test
    void ignoresPendingPayments() {
        stored(attempt(1L, null, "tx-1"));
        transaction("tx-1", XBezahldienstStatus.INITIAL);

        assertTrue(service.findPaidEarlierAttempt(attempt(2L, 1L, null)).isEmpty());
    }

    @Test
    void doesNotReuseAPaymentForATaskThatIsNoRerun() {
        // A later loop iteration of the same element is a new payment request.
        assertTrue(service.findPaidEarlierAttempt(attempt(2L, null, null)).isEmpty());

        verifyNoInteractions(tasks, transactions);
    }

    @Test
    void ignoresAttemptsOfAnotherInstanceOrElement() {
        stored(attempt(1L, null, "tx-1").setProcessInstanceId(INSTANCE_ID + 1));
        stored(attempt(2L, null, "tx-2").setProcessNodeId(NODE_ID + 1));
        transaction("tx-1", XBezahldienstStatus.PAYED);
        transaction("tx-2", XBezahldienstStatus.PAYED);

        assertTrue(service.findPaidEarlierAttempt(attempt(3L, 1L, null)).isEmpty());
        assertTrue(service.findPaidEarlierAttempt(attempt(4L, 2L, null)).isEmpty());
    }

    @Test
    void stopsAtACycleInTheRestartChain() {
        stored(attempt(1L, 2L, null));
        stored(attempt(2L, 1L, null));

        assertTrue(service.findPaidEarlierAttempt(attempt(3L, 1L, null)).isEmpty());
    }

    @Test
    void recordsTheReuseInTheHistory() {
        var events = mock(ProcessInstanceHistoryEventRepository.class);
        var paidAttempt = new PaymentTaskAttemptService.PaidAttempt(attempt(1L, null, "tx-1"),
                new PaymentTransactionEntity().setKey("tx-1"));

        service.logReuse(new ProcessNodeExecutionLogger(INSTANCE_ID, 2L, null, null, events), paidAttempt);

        var event = ArgumentCaptor.forClass(ProcessInstanceEventEntity.class);
        verify(events).save(event.capture());
        assertEquals("Zahlung aus früherem Versuch übernommen", event.getValue().getTitle());
        assertTrue(event.getValue().getHistoryRelevant());
        assertEquals("tx-1", event.getValue().getDetails().get("transactionKey"));
        assertEquals(1L, ((Number) event.getValue().getDetails().get("earlierTaskId")).longValue());
    }

    private ProcessInstanceTaskEntity stored(ProcessInstanceTaskEntity attempt) {
        when(tasks.findById(attempt.getId())).thenReturn(Optional.of(attempt));
        return attempt;
    }

    private PaymentTransactionEntity transaction(String key, XBezahldienstStatus status) {
        var information = new XBezahldienstePaymentInformation();
        information.setStatus(status);
        var transaction = new PaymentTransactionEntity().setKey(key).setPaymentInformation(information);
        when(transactions.retrieve(key)).thenReturn(Optional.of(transaction));
        return transaction;
    }

    private static ProcessInstanceTaskEntity attempt(Long id, Long restartForTaskId, String transactionKey) {
        return new ProcessInstanceTaskEntity()
                .setId(id)
                .setProcessInstanceId(INSTANCE_ID)
                .setProcessNodeId(NODE_ID)
                .setStatus(restartForTaskId == null ? ProcessTaskStatus.Restarted : ProcessTaskStatus.Running)
                .setRestartForTaskId(restartForTaskId)
                .setRuntimeData(transactionKey == null
                        ? Map.of()
                        : Map.of(PaymentTaskRuntimeDataKeys.PAYMENT_TRANSACTION_KEY, transactionKey))
                .setNodeData(Map.of());
    }
}
