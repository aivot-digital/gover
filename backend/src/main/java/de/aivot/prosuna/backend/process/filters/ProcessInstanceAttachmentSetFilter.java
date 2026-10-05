package de.aivot.prosuna.backend.process.filters;

import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import de.aivot.prosuna.backend.utils.specification.SpecificationBuilder;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class ProcessInstanceAttachmentSetFilter implements Filter<ProcessInstanceAttachmentSetEntity> {
    private Long processInstanceId;
    private List<Long> processInstanceIds;
    private Long processInstanceTaskId;
    private String name;
    private String dataKey;

    public static ProcessInstanceAttachmentSetFilter create() {
        return new ProcessInstanceAttachmentSetFilter();
    }

    @Nonnull
    @Override
    public Specification<ProcessInstanceAttachmentSetEntity> build() {
        return SpecificationBuilder
                .create(ProcessInstanceAttachmentSetEntity.class)
                .withEquals("processInstanceId", processInstanceId)
                .withInList("processInstanceId", processInstanceIds)
                .withEquals("processInstanceTaskId", processInstanceTaskId)
                .withContains("name", name)
                .withEquals("dataKey", dataKey)
                .build();
    }

    public Long getProcessInstanceId() {
        return processInstanceId;
    }

    public ProcessInstanceAttachmentSetFilter setProcessInstanceId(Long processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    public List<Long> getProcessInstanceIds() {
        return processInstanceIds;
    }

    public ProcessInstanceAttachmentSetFilter setProcessInstanceIds(List<Long> processInstanceIds) {
        this.processInstanceIds = processInstanceIds;
        return this;
    }

    public Long getProcessInstanceTaskId() {
        return processInstanceTaskId;
    }

    public ProcessInstanceAttachmentSetFilter setProcessInstanceTaskId(Long processInstanceTaskId) {
        this.processInstanceTaskId = processInstanceTaskId;
        return this;
    }

    public String getName() {
        return name;
    }

    public ProcessInstanceAttachmentSetFilter setName(String name) {
        this.name = name;
        return this;
    }

    public String getDataKey() {
        return dataKey;
    }

    public ProcessInstanceAttachmentSetFilter setDataKey(String dataKey) {
        this.dataKey = dataKey;
        return this;
    }
}
