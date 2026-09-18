package de.aivot.prosuna.backend.plugins.form.v1.services;

import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.process.workers.ProcessWorker;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FormPaymentTransactionChangeListenerTest {
    @Test
    void onChange_QueuesResumeWorkForMatchingTasks() throws Exception {
        var repository = mock(ProcessInstanceTaskRepository.class);
        var rabbitTemplate = mock(RabbitTemplate.class);
        var listener = new FormPaymentTransactionChangeListener(repository, rabbitTemplate);
        var task = new ProcessInstanceTaskEntity()
                .setId(456L)
                .setProcessInstanceId(99L)
                .setProcessNodeId(123);

        when(repository.findAll(any(Specification.class)))
                .thenReturn(List.of(task));

        listener.onChange(new PaymentTransactionEntity().setKey("tx-1"));

        verify(rabbitTemplate).convertAndSend(
                eq(ProcessWorker.RESUME_WORK_ON_INSTANCE_QUEUE),
                eq(new ProcessWorker.ResumeWorkWorkerPayload(99L, 456L, 123))
        );
    }
}
