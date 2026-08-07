package de.aivot.gover.backend.payment.services;

import de.aivot.gover.backend.enums.XBezahldienstStatus;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.gover.backend.payment.models.PaymentTaskRuntimeDataKeys;
import de.aivot.gover.backend.payment.models.PaymentTransactionChangeListener;
import de.aivot.gover.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.gover.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.gover.backend.process.workers.ProcessWorker;
import de.aivot.gover.backend.utils.specification.SpecificationBuilderJsonEquals;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentTransactionTaskResumeListener implements PaymentTransactionChangeListener {
    private final ProcessInstanceTaskRepository processInstanceTaskRepository;
    private final RabbitTemplate rabbitTemplate;

    public PaymentTransactionTaskResumeListener(ProcessInstanceTaskRepository processInstanceTaskRepository,
                                                RabbitTemplate rabbitTemplate) {
        this.processInstanceTaskRepository = processInstanceTaskRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void onChange(PaymentTransactionEntity paymentTransactionEntity) throws ResponseException {
        if (paymentTransactionEntity.getStatus() == XBezahldienstStatus.PAYED) {
            resumeTasksFor(paymentTransactionEntity);
        }
    }

    @Override
    public void onDelete(PaymentTransactionEntity paymentTransactionEntity) throws ResponseException {
        resumeTasksFor(paymentTransactionEntity);
    }

    private void resumeTasksFor(PaymentTransactionEntity paymentTransactionEntity) {
        var specificationBuilderJsonEquals = new SpecificationBuilderJsonEquals<ProcessInstanceTaskEntity>(
                "runtimeData",
                List.of(PaymentTaskRuntimeDataKeys.PAYMENT_TRANSACTION),
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
