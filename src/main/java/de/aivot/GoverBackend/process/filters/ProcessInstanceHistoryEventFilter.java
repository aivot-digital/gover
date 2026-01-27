package de.aivot.GoverBackend.process.filters;

import de.aivot.GoverBackend.process.entities.ProcessInstanceEventEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import jakarta.annotation.Nonnull;

public class ProcessInstanceHistoryEventFilter implements Filter<ProcessInstanceEventEntity> {
    private Long id;
    private String triggeringUserId;
    private Long processInstanceId;
    private Long processInstanceTaskId;

    public static ProcessInstanceHistoryEventFilter create() {
        return new ProcessInstanceHistoryEventFilter();
    }

    @Nonnull
    @Override
    public Specification<ProcessInstanceEventEntity> build() {
        var builder = SpecificationBuilder
                .create(ProcessInstanceEventEntity.class)
                .withEquals("id", id)
                .withEquals("triggeringUserId", triggeringUserId)
                .withEquals("processInstanceId", processInstanceId)
                .withEquals("processInstanceTaskId", processInstanceTaskId);

        return builder.build();
    }

    public Long getId() {
        return id;
    }

    public ProcessInstanceHistoryEventFilter setId(Long id) {
        this.id = id;
        return this;
    }

    public String getTriggeringUserId() {
        return triggeringUserId;
    }

    public ProcessInstanceHistoryEventFilter setTriggeringUserId(String triggeringUserId) {
        this.triggeringUserId = triggeringUserId;
        return this;
    }

    public Long getProcessInstanceId() {
        return processInstanceId;
    }

    public ProcessInstanceHistoryEventFilter setProcessInstanceId(Long processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    public Long getProcessInstanceTaskId() {
        return processInstanceTaskId;
    }

    public ProcessInstanceHistoryEventFilter setProcessInstanceTaskId(Long processInstanceTaskId) {
        this.processInstanceTaskId = processInstanceTaskId;
        return this;
    }
}

