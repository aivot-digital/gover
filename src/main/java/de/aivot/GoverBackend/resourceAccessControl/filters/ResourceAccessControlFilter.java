package de.aivot.GoverBackend.resourceAccessControl.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.resourceAccessControl.entities.ResourceAccessControlEntity;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

public class ResourceAccessControlFilter implements Filter<ResourceAccessControlEntity> {
    private Integer id;
    private List<Integer> ids;
    private Integer sourceTeamId;
    private List<Integer> sourceTeamIds;
    private Integer sourceOrgUnitId;
    private List<Integer> sourceOrgUnitIds;
    private Integer targetFormId;
    private List<Integer> targetFormIds;
    private Integer targetProcessId;
    private List<Integer> targetProcessIds;
    private Integer targetProcessInstanceId;
    private List<Integer> targetProcessInstanceIds;

    private Boolean formPermissionCreate;
    private Boolean formPermissionRead;
    private Boolean formPermissionEdit;
    private Boolean formPermissionDelete;
    private Boolean formPermissionAnnotate;
    private Boolean formPermissionPublish;

    private Boolean processPermissionCreate;
    private Boolean processPermissionRead;
    private Boolean processPermissionEdit;
    private Boolean processPermissionDelete;
    private Boolean processPermissionAnnotate;
    private Boolean processPermissionPublish;

    private Boolean processInstancePermissionCreate;
    private Boolean processInstancePermissionRead;
    private Boolean processInstancePermissionEdit;
    private Boolean processInstancePermissionDelete;
    private Boolean processInstancePermissionAnnotate;

    private LocalDateTime created;
    private LocalDateTime updated;

    public static ResourceAccessControlFilter create() {
        return new ResourceAccessControlFilter();
    }

    @Override
    public Specification<ResourceAccessControlEntity> build() {
        return SpecificationBuilder
                .create(ResourceAccessControlEntity.class)
                .withEquals("id", id)
                .withInList("id", ids)
                .withEquals("sourceTeamId", sourceTeamId)
                .withInList("sourceTeamId", sourceTeamIds)
                .withEquals("sourceOrgUnitId", sourceOrgUnitId)
                .withInList("sourceOrgUnitId", sourceOrgUnitIds)
                .withEquals("targetFormId", targetFormId)
                .withInList("targetFormId", targetFormIds)
                .withEquals("targetProcessId", targetProcessId)
                .withInList("targetProcessId", targetProcessIds)
                .withEquals("targetProcessInstanceId", targetProcessInstanceId)
                .withInList("targetProcessInstanceId", targetProcessInstanceIds)
                .withEquals("formPermissionCreate", formPermissionCreate)
                .withEquals("formPermissionRead", formPermissionRead)
                .withEquals("formPermissionEdit", formPermissionEdit)
                .withEquals("formPermissionDelete", formPermissionDelete)
                .withEquals("formPermissionAnnotate", formPermissionAnnotate)
                .withEquals("formPermissionPublish", formPermissionPublish)
                .withEquals("processPermissionCreate", processPermissionCreate)
                .withEquals("processPermissionRead", processPermissionRead)
                .withEquals("processPermissionEdit", processPermissionEdit)
                .withEquals("processPermissionDelete", processPermissionDelete)
                .withEquals("processPermissionAnnotate", processPermissionAnnotate)
                .withEquals("processPermissionPublish", processPermissionPublish)
                .withEquals("processInstancePermissionCreate", processInstancePermissionCreate)
                .withEquals("processInstancePermissionRead", processInstancePermissionRead)
                .withEquals("processInstancePermissionEdit", processInstancePermissionEdit)
                .withEquals("processInstancePermissionDelete", processInstancePermissionDelete)
                .withEquals("processInstancePermissionAnnotate", processInstancePermissionAnnotate)
                .withEquals("created", created)
                .withEquals("updated", updated)
                .build();
    }

    // region Getters and Setters

    public Integer getId() {
        return id;
    }

    public ResourceAccessControlFilter setId(Integer id) {
        this.id = id;
        return this;
    }

    public List<Integer> getIds() {
        return ids;
    }

    public ResourceAccessControlFilter setIds(List<Integer> ids) {
        this.ids = ids;
        return this;
    }

    public Integer getSourceTeamId() {
        return sourceTeamId;
    }

    public ResourceAccessControlFilter setSourceTeamId(Integer sourceTeamId) {
        this.sourceTeamId = sourceTeamId;
        return this;
    }

    public List<Integer> getSourceTeamIds() {
        return sourceTeamIds;
    }

    public ResourceAccessControlFilter setSourceTeamIds(List<Integer> sourceTeamIds) {
        this.sourceTeamIds = sourceTeamIds;
        return this;
    }

    public Integer getSourceOrgUnitId() {
        return sourceOrgUnitId;
    }

    public ResourceAccessControlFilter setSourceOrgUnitId(Integer sourceOrgUnitId) {
        this.sourceOrgUnitId = sourceOrgUnitId;
        return this;
    }

    public List<Integer> getSourceOrgUnitIds() {
        return sourceOrgUnitIds;
    }

    public ResourceAccessControlFilter setSourceOrgUnitIds(List<Integer> sourceOrgUnitIds) {
        this.sourceOrgUnitIds = sourceOrgUnitIds;
        return this;
    }

    public Integer getTargetFormId() {
        return targetFormId;
    }

    public ResourceAccessControlFilter setTargetFormId(Integer targetFormId) {
        this.targetFormId = targetFormId;
        return this;
    }

    public List<Integer> getTargetFormIds() {
        return targetFormIds;
    }

    public ResourceAccessControlFilter setTargetFormIds(List<Integer> targetFormIds) {
        this.targetFormIds = targetFormIds;
        return this;
    }

    public Integer getTargetProcessId() {
        return targetProcessId;
    }

    public ResourceAccessControlFilter setTargetProcessId(Integer targetProcessId) {
        this.targetProcessId = targetProcessId;
        return this;
    }

    public List<Integer> getTargetProcessIds() {
        return targetProcessIds;
    }

    public ResourceAccessControlFilter setTargetProcessIds(List<Integer> targetProcessIds) {
        this.targetProcessIds = targetProcessIds;
        return this;
    }

    public Integer getTargetProcessInstanceId() {
        return targetProcessInstanceId;
    }

    public ResourceAccessControlFilter setTargetProcessInstanceId(Integer targetProcessInstanceId) {
        this.targetProcessInstanceId = targetProcessInstanceId;
        return this;
    }

    public List<Integer> getTargetProcessInstanceIds() {
        return targetProcessInstanceIds;
    }

    public ResourceAccessControlFilter setTargetProcessInstanceIds(List<Integer> targetProcessInstanceIds) {
        this.targetProcessInstanceIds = targetProcessInstanceIds;
        return this;
    }

    public Boolean getFormPermissionCreate() {
        return formPermissionCreate;
    }

    public ResourceAccessControlFilter setFormPermissionCreate(Boolean formPermissionCreate) {
        this.formPermissionCreate = formPermissionCreate;
        return this;
    }

    public Boolean getFormPermissionRead() {
        return formPermissionRead;
    }

    public ResourceAccessControlFilter setFormPermissionRead(Boolean formPermissionRead) {
        this.formPermissionRead = formPermissionRead;
        return this;
    }

    public Boolean getFormPermissionEdit() {
        return formPermissionEdit;
    }

    public ResourceAccessControlFilter setFormPermissionEdit(Boolean formPermissionEdit) {
        this.formPermissionEdit = formPermissionEdit;
        return this;
    }

    public Boolean getFormPermissionDelete() {
        return formPermissionDelete;
    }

    public ResourceAccessControlFilter setFormPermissionDelete(Boolean formPermissionDelete) {
        this.formPermissionDelete = formPermissionDelete;
        return this;
    }

    public Boolean getFormPermissionAnnotate() {
        return formPermissionAnnotate;
    }

    public ResourceAccessControlFilter setFormPermissionAnnotate(Boolean formPermissionAnnotate) {
        this.formPermissionAnnotate = formPermissionAnnotate;
        return this;
    }

    public Boolean getFormPermissionPublish() {
        return formPermissionPublish;
    }

    public ResourceAccessControlFilter setFormPermissionPublish(Boolean formPermissionPublish) {
        this.formPermissionPublish = formPermissionPublish;
        return this;
    }

    public Boolean getProcessPermissionCreate() {
        return processPermissionCreate;
    }

    public ResourceAccessControlFilter setProcessPermissionCreate(Boolean processPermissionCreate) {
        this.processPermissionCreate = processPermissionCreate;
        return this;
    }

    public Boolean getProcessPermissionRead() {
        return processPermissionRead;
    }

    public ResourceAccessControlFilter setProcessPermissionRead(Boolean processPermissionRead) {
        this.processPermissionRead = processPermissionRead;
        return this;
    }

    public Boolean getProcessPermissionEdit() {
        return processPermissionEdit;
    }

    public ResourceAccessControlFilter setProcessPermissionEdit(Boolean processPermissionEdit) {
        this.processPermissionEdit = processPermissionEdit;
        return this;
    }

    public Boolean getProcessPermissionDelete() {
        return processPermissionDelete;
    }

    public ResourceAccessControlFilter setProcessPermissionDelete(Boolean processPermissionDelete) {
        this.processPermissionDelete = processPermissionDelete;
        return this;
    }

    public Boolean getProcessPermissionAnnotate() {
        return processPermissionAnnotate;
    }

    public ResourceAccessControlFilter setProcessPermissionAnnotate(Boolean processPermissionAnnotate) {
        this.processPermissionAnnotate = processPermissionAnnotate;
        return this;
    }

    public Boolean getProcessPermissionPublish() {
        return processPermissionPublish;
    }

    public ResourceAccessControlFilter setProcessPermissionPublish(Boolean processPermissionPublish) {
        this.processPermissionPublish = processPermissionPublish;
        return this;
    }

    public Boolean getProcessInstancePermissionCreate() {
        return processInstancePermissionCreate;
    }

    public ResourceAccessControlFilter setProcessInstancePermissionCreate(Boolean processInstancePermissionCreate) {
        this.processInstancePermissionCreate = processInstancePermissionCreate;
        return this;
    }

    public Boolean getProcessInstancePermissionRead() {
        return processInstancePermissionRead;
    }

    public ResourceAccessControlFilter setProcessInstancePermissionRead(Boolean processInstancePermissionRead) {
        this.processInstancePermissionRead = processInstancePermissionRead;
        return this;
    }

    public Boolean getProcessInstancePermissionEdit() {
        return processInstancePermissionEdit;
    }

    public ResourceAccessControlFilter setProcessInstancePermissionEdit(Boolean processInstancePermissionEdit) {
        this.processInstancePermissionEdit = processInstancePermissionEdit;
        return this;
    }

    public Boolean getProcessInstancePermissionDelete() {
        return processInstancePermissionDelete;
    }

    public ResourceAccessControlFilter setProcessInstancePermissionDelete(Boolean processInstancePermissionDelete) {
        this.processInstancePermissionDelete = processInstancePermissionDelete;
        return this;
    }

    public Boolean getProcessInstancePermissionAnnotate() {
        return processInstancePermissionAnnotate;
    }

    public ResourceAccessControlFilter setProcessInstancePermissionAnnotate(Boolean processInstancePermissionAnnotate) {
        this.processInstancePermissionAnnotate = processInstancePermissionAnnotate;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public ResourceAccessControlFilter setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public ResourceAccessControlFilter setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    // endregion
}

