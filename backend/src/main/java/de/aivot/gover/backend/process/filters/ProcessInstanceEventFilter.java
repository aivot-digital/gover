package de.aivot.gover.backend.process.filters;

import de.aivot.gover.backend.lib.models.Filter;
import de.aivot.gover.backend.process.entities.ProcessInstanceEventEntity;
import de.aivot.gover.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.gover.backend.utils.specification.SpecificationBuilder;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class ProcessInstanceEventFilter implements Filter<ProcessInstanceEventEntity> {
    private String triggeringUserId;
    private Long processInstanceId;
    private List<Long> processInstanceIds;
    private Long processInstanceTaskId;
    private ProcessNodeExecutionLogLevel level;
    private Boolean isTechnical;
    private Boolean isNotTechnical;
    private Boolean isAudit;
    private Boolean isNotAudit;
    private String title;

    public static ProcessInstanceEventFilter create() {
        return new ProcessInstanceEventFilter();
    }

    @Nonnull
    @Override
    public Specification<ProcessInstanceEventEntity> build() {
        var builder = SpecificationBuilder
                .create(ProcessInstanceEventEntity.class)
                .withEquals("triggeringUserId", triggeringUserId)
                .withEquals("processInstanceId", processInstanceId)
                .withInList("processInstanceId", processInstanceIds)
                .withEquals("processInstanceTaskId", processInstanceTaskId)
                .withEquals("level", level)
                .withEquals("isTechnical", isTechnical)
                .withNotEquals("isTechnical", isNotTechnical)
                .withEquals("isAudit", isAudit)
                .withNotEquals("isAudit", isNotAudit)
                .withContains("title", title);

        return builder.build();
    }

    public String getTriggeringUserId() {
        return triggeringUserId;
    }

    public ProcessInstanceEventFilter setTriggeringUserId(String triggeringUserId) {
        this.triggeringUserId = triggeringUserId;
        return this;
    }

    public Long getProcessInstanceId() {
        return processInstanceId;
    }

    public ProcessInstanceEventFilter setProcessInstanceId(Long processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    public List<Long> getProcessInstanceIds() {
        return processInstanceIds;
    }

    public ProcessInstanceEventFilter setProcessInstanceIds(List<Long> processInstanceIds) {
        this.processInstanceIds = processInstanceIds;
        return this;
    }

    public Long getProcessInstanceTaskId() {
        return processInstanceTaskId;
    }

    public ProcessInstanceEventFilter setProcessInstanceTaskId(Long processInstanceTaskId) {
        this.processInstanceTaskId = processInstanceTaskId;
        return this;
    }

    public ProcessNodeExecutionLogLevel getLevel() {
        return level;
    }

    public ProcessInstanceEventFilter setLevel(ProcessNodeExecutionLogLevel level) {
        this.level = level;
        return this;
    }

    public Boolean getTechnical() {
        return isTechnical;
    }

    public ProcessInstanceEventFilter setTechnical(Boolean technical) {
        isTechnical = technical;
        return this;
    }

    public Boolean getNotTechnical() {
        return isNotTechnical;
    }

    public ProcessInstanceEventFilter setNotTechnical(Boolean notTechnical) {
        isNotTechnical = notTechnical;
        return this;
    }

    public Boolean getAudit() {
        return isAudit;
    }

    public ProcessInstanceEventFilter setAudit(Boolean audit) {
        isAudit = audit;
        return this;
    }

    public Boolean getNotAudit() {
        return isNotAudit;
    }

    public ProcessInstanceEventFilter setNotAudit(Boolean notAudit) {
        isNotAudit = notAudit;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public ProcessInstanceEventFilter setTitle(String title) {
        this.title = title;
        return this;
    }
}
