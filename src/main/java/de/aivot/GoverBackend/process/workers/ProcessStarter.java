package de.aivot.GoverBackend.process.workers;

import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@EnableScheduling
public class ProcessStarter {
    private static final Logger logger = LoggerFactory.getLogger(ProcessStarter.class);

    private final ProcessInstanceRepository processInstanceRepository;
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public ProcessStarter(ProcessInstanceRepository processInstanceRepository,
                          RabbitTemplate rabbitTemplate) {
        this.processInstanceRepository = processInstanceRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedRate = 1000 * 10) // every 10 seconds
    public void startProcesses() {
        var allUnstartedProcesses = processInstanceRepository
                .findAllByStatus(ProcessInstanceStatus.Created);

        for (var processInstance : allUnstartedProcesses) {
            logger
                    .atDebug()
                    .setMessage("Starting process instance with ID: {}")
                    .addArgument(processInstance.getId())
                    .log();

            var initialNode = processInstance
                    .getInitialNodeId();

            var payload = new ProcessWorker.WorkerPayload(
                    processInstance.getId(),
                    null,
                    initialNode
            );

            rabbitTemplate.convertAndSend(
                    ProcessWorker.DO_WORK_ON_INSTANCE_QUEUE,
                    payload
            );

            processInstance.setStatus(ProcessInstanceStatus.Running);
            processInstanceRepository.save(processInstance);
        }
    }
}
