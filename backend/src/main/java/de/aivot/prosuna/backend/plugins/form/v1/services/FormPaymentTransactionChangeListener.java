package de.aivot.prosuna.backend.plugins.form.v1.services;

import de.aivot.prosuna.backend.enums.XBezahldienstStatus;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.prosuna.backend.payment.models.PaymentTaskRuntimeDataKeys;
import de.aivot.prosuna.backend.payment.models.PaymentTransactionChangeListener;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.process.services.ProcessNodeExecutionLoggerFactory;
import de.aivot.prosuna.backend.process.workers.ProcessWorker;
import de.aivot.prosuna.backend.utils.specification.SpecificationBuilderJsonEquals;
import jakarta.annotation.Nonnull;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;

@Component
public class FormPaymentTransactionChangeListener implements PaymentTransactionChangeListener {
    private final ProcessInstanceTaskRepository processInstanceTaskRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory;

    public FormPaymentTransactionChangeListener(ProcessInstanceTaskRepository processInstanceTaskRepository,
                                                RabbitTemplate rabbitTemplate,
                                                ProcessNodeExecutionLoggerFactory processNodeExecutionLoggerFactory) {
        this.processInstanceTaskRepository = processInstanceTaskRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.processNodeExecutionLoggerFactory = processNodeExecutionLoggerFactory;
    }

    @Override
    public void onChange(PaymentTransactionEntity paymentTransactionEntity) throws ResponseException {
        var tasks = findTasks(paymentTransactionEntity);
        if (paymentTransactionEntity.getStatus() == XBezahldienstStatus.PAYED) {
            for (var task : tasks) {
                recordPaymentForEndedTask(task, paymentTransactionEntity);
            }
        }
        resume(tasks);
    }

    @Override
    public void onDelete(PaymentTransactionEntity paymentTransactionEntity) throws ResponseException {
        resume(findTasks(paymentTransactionEntity));
    }

    @Nonnull
    private List<ProcessInstanceTaskEntity> findTasks(@Nonnull PaymentTransactionEntity paymentTransactionEntity) {
        var specificationBuilderJsonEquals = new SpecificationBuilderJsonEquals<ProcessInstanceTaskEntity>(
                "runtimeData",
                List.of(PaymentTaskRuntimeDataKeys.PAYMENT_TRANSACTION_KEY),
                paymentTransactionEntity.getKey()
        );

        Specification<ProcessInstanceTaskEntity> spec = specificationBuilderJsonEquals::toPredicate;

        return processInstanceTaskRepository.findAll(spec);
    }

    private void resume(@Nonnull List<ProcessInstanceTaskEntity> tasks) {
        for (ProcessInstanceTaskEntity task : tasks) {
            rabbitTemplate.convertAndSend(
                    ProcessWorker.RESUME_WORK_ON_INSTANCE_QUEUE,
                    new ProcessWorker.ResumeWorkWorkerPayload(
                            task.getProcessInstanceId(),
                            task.getId(),
                            task.getProcessNodeId()
                    ));
        }
    }

    /**
     * Failed and rerun tasks no longer process payment notifications. Record a payment that completes afterwards, so
     * staff can see it before a payment is requested again or refund a payment that was made twice.
     */
    private void recordPaymentForEndedTask(@Nonnull ProcessInstanceTaskEntity task,
                                           @Nonnull PaymentTransactionEntity transaction) {
        var message = switch (task.getStatus()) {
            case Failed -> "Die Zahlung „%s“ wurde abgeschlossen, nachdem diese Aufgabe fehlgeschlagen war. " +
                    "Wenn Sie die Aufgabe erneut ausführen, wird diese Zahlung übernommen und keine neue Zahlung angefordert.";
            case Restarted -> "Die Zahlung „%s“ wurde abgeschlossen, nachdem diese Aufgabe bereits erneut ausgeführt wurde. " +
                    "Sie wird nicht mehr verarbeitet. Prüfen Sie, ob für die erneute Ausführung eine weitere Zahlung " +
                    "angefordert wurde und eine Erstattung erforderlich ist.";
            default -> null;
        };
        if (message == null) {
            return;
        }
        var details = new LinkedHashMap<String, Object>();
        details.put("transactionKey", transaction.getKey());
        details.put("taskStatus", task.getStatus().name());
        processNodeExecutionLoggerFactory
                .create(task.getProcessInstanceId(), task.getId(), null, null)
                .logf(
                        ProcessNodeExecutionLogLevel.Warn,
                        false,
                        true,
                        true,
                        task.getAssignedUserId(),
                        task.getAssignedCustomerIdentityId(),
                        null,
                        "Zahlung für beendete Aufgabe eingegangen",
                        details,
                        message,
                        transaction.getKey()
                );
    }
}
