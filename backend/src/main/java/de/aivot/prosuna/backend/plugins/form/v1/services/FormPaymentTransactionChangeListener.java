package de.aivot.prosuna.backend.plugins.form.v1.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.prosuna.backend.payment.models.PaymentTaskRuntimeDataKeys;
import de.aivot.prosuna.backend.payment.models.PaymentTransactionChangeListener;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.process.workers.ProcessWorker;
import de.aivot.prosuna.backend.utils.specification.SpecificationBuilderJsonEquals;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FormPaymentTransactionChangeListener implements PaymentTransactionChangeListener {
    private final ProcessInstanceTaskRepository processInstanceTaskRepository;
    private final RabbitTemplate rabbitTemplate;

    public FormPaymentTransactionChangeListener(ProcessInstanceTaskRepository processInstanceTaskRepository, RabbitTemplate rabbitTemplate) {
        this.processInstanceTaskRepository = processInstanceTaskRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void onChange(PaymentTransactionEntity paymentTransactionEntity) throws ResponseException {
        handle(paymentTransactionEntity);
    }

    @Override
    public void onDelete(PaymentTransactionEntity paymentTransactionEntity) throws ResponseException {
        handle(paymentTransactionEntity);
    }

    private void handle(PaymentTransactionEntity paymentTransactionEntity) throws ResponseException {
        var specificationBuilderJsonEquals = new SpecificationBuilderJsonEquals<ProcessInstanceTaskEntity>(
                "runtimeData",
                List.of(PaymentTaskRuntimeDataKeys.PAYMENT_TRANSACTION_KEY),
                paymentTransactionEntity.getKey()
        );

        Specification<ProcessInstanceTaskEntity> spec = specificationBuilderJsonEquals::toPredicate;

        List<ProcessInstanceTaskEntity> tasks = processInstanceTaskRepository.findAll(spec);

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
}
