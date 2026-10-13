package de.aivot.prosuna.backend.payment.services;

import de.aivot.prosuna.backend.enums.XBezahldienstStatus;
import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.prosuna.backend.payment.models.PaymentTaskRuntimeDataKeys;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;

/**
 * Finds a payment that an earlier attempt of a rerun task has already received.
 *
 * <p>A task can fail after its payment succeeded, e.g. while the payment confirmation is processed. Rerunning the task
 * must not ask the customer to pay again for the same request.
 */
@Service
public class PaymentTaskAttemptService {
    private final ProcessInstanceTaskRepository tasks;
    private final PaymentTransactionService transactions;

    public PaymentTaskAttemptService(@Nonnull ProcessInstanceTaskRepository tasks,
                                     @Nonnull PaymentTransactionService transactions) {
        this.tasks = tasks;
        this.transactions = transactions;
    }

    @Nonnull
    public Optional<PaidAttempt> findPaidEarlierAttempt(@Nonnull ProcessInstanceTaskEntity task) {
        // Only the restart chain counts. A later loop iteration of the same element is a new request and must be paid.
        var visited = new HashSet<Long>();
        var attemptId = task.getRestartForTaskId();
        while (attemptId != null && visited.add(attemptId)) {
            var attempt = tasks.findById(attemptId).orElse(null);
            if (attempt == null
                    || !Objects.equals(attempt.getProcessInstanceId(), task.getProcessInstanceId())
                    || !Objects.equals(attempt.getProcessNodeId(), task.getProcessNodeId())) {
                return Optional.empty();
            }
            var transactionKey = attempt.getRuntimeData().get(PaymentTaskRuntimeDataKeys.PAYMENT_TRANSACTION_KEY);
            if (transactionKey != null) {
                var transaction = transactions.retrieve(String.valueOf(transactionKey)).orElse(null);
                if (transaction != null && transaction.getStatus() == XBezahldienstStatus.PAYED) {
                    return Optional.of(new PaidAttempt(attempt, transaction));
                }
            }
            attemptId = attempt.getRestartForTaskId();
        }
        return Optional.empty();
    }

    /** Records that the rerun task uses the earlier payment, so staff can see why no new payment was requested. */
    public void logReuse(@Nonnull ProcessNodeExecutionLogger logger, @Nonnull PaidAttempt paidAttempt) {
        var details = new LinkedHashMap<String, Object>();
        details.put("transactionKey", paidAttempt.transaction().getKey());
        details.put("earlierTaskId", paidAttempt.attempt().getId());
        logger.logf(
                ProcessNodeExecutionLogLevel.Info,
                false,
                true,
                true,
                null,
                null,
                null,
                "Zahlung aus früherem Versuch übernommen",
                details,
                "Die Zahlung „%s“ wurde bereits in einem früheren Versuch dieser Aufgabe erfolgreich abgeschlossen. " +
                        "Es wird keine neue Zahlung angefordert.",
                paidAttempt.transaction().getKey()
        );
    }

    public record PaidAttempt(@Nonnull ProcessInstanceTaskEntity attempt,
                              @Nonnull PaymentTransactionEntity transaction) {
    }
}
