package de.aivot.GoverBackend.submission.filters;

import de.aivot.GoverBackend.enums.SubmissionStatus;
import de.aivot.GoverBackend.enums.UserRole;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.submission.entities.SubmissionWithMembership;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;

public class SubmissionWithMembershipFilter implements Filter<SubmissionWithMembership> {
    private String id;

    private Integer formId;
    private String assigneeId;
    private SubmissionStatus status;
    private String fileNumber;
    private Integer destinationId;
    private Boolean notArchived;
    private Boolean notPending;
    private Boolean notTestSubmission;

    private Integer developingDepartmentId;
    private Integer responsibleDepartmentId;
    private Integer managingDepartmentId;

    private String userId;
    private UserRole userRole;

    public static SubmissionWithMembershipFilter create() {
        return new SubmissionWithMembershipFilter();
    }

    public String getId() {
        return id;
    }

    public SubmissionWithMembershipFilter setId(String id) {
        this.id = id;
        return this;
    }

    public Integer getFormId() {
        return formId;
    }

    public SubmissionWithMembershipFilter setFormId(Integer formId) {
        this.formId = formId;
        return this;
    }

    public String getAssigneeId() {
        return assigneeId;
    }

    public SubmissionWithMembershipFilter setAssigneeId(String assigneeId) {
        this.assigneeId = assigneeId;
        return this;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public SubmissionWithMembershipFilter setStatus(SubmissionStatus status) {
        this.status = status;
        return this;
    }

    public String getFileNumber() {
        return fileNumber;
    }

    public SubmissionWithMembershipFilter setFileNumber(String fileNumber) {
        this.fileNumber = fileNumber;
        return this;
    }

    public Integer getDestinationId() {
        return destinationId;
    }

    public SubmissionWithMembershipFilter setDestinationId(Integer destinationId) {
        this.destinationId = destinationId;
        return this;
    }

    public Boolean getNotArchived() {
        return notArchived;
    }

    public SubmissionWithMembershipFilter setNotArchived(Boolean notArchived) {
        this.notArchived = notArchived;
        return this;
    }

    public Boolean getNotPending() {
        return notPending;
    }

    public SubmissionWithMembershipFilter setNotPending(Boolean notPending) {
        this.notPending = notPending;
        return this;
    }

    public Boolean getNotTestSubmission() {
        return notTestSubmission;
    }

    public SubmissionWithMembershipFilter setNotTestSubmission(Boolean notTestSubmission) {
        this.notTestSubmission = notTestSubmission;
        return this;
    }

    public Integer getDevelopingDepartmentId() {
        return developingDepartmentId;
    }

    public SubmissionWithMembershipFilter setDevelopingDepartmentId(Integer developingDepartmentId) {
        this.developingDepartmentId = developingDepartmentId;
        return this;
    }

    public Integer getResponsibleDepartmentId() {
        return responsibleDepartmentId;
    }

    public SubmissionWithMembershipFilter setResponsibleDepartmentId(Integer responsibleDepartmentId) {
        this.responsibleDepartmentId = responsibleDepartmentId;
        return this;
    }

    public Integer getManagingDepartmentId() {
        return managingDepartmentId;
    }

    public SubmissionWithMembershipFilter setManagingDepartmentId(Integer managingDepartmentId) {
        this.managingDepartmentId = managingDepartmentId;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public SubmissionWithMembershipFilter setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public UserRole getUserRole() {
        return userRole;
    }

    public SubmissionWithMembershipFilter setUserRole(UserRole userRole) {
        this.userRole = userRole;
        return this;
    }

    @Nonnull
    @Override
    public Specification<SubmissionWithMembership> build() {
        var spec = SpecificationBuilder
                .create(SubmissionWithMembership.class)
                .withEquals("id", id)

                .withEquals("formId", formId)
                .withEquals("assigneeId", assigneeId)
                .withEquals("status", status)
                .withEquals("fileNumber", fileNumber)
                .withEquals("destinationId", destinationId);

        if (Boolean.TRUE.equals(notTestSubmission)) {
            spec.withEquals("isTestSubmission", false);
        }

        if (Boolean.TRUE.equals(notArchived)) {
            spec.withNotEquals("status", SubmissionStatus.Archived);
        }

        if (Boolean.TRUE.equals(notPending)) {
            spec.withNotEquals("status", SubmissionStatus.Pending);
        }

        return spec
                .withEquals("developingDepartmentId", developingDepartmentId)
                .withEquals("responsibleDepartmentId", responsibleDepartmentId)
                .withEquals("managingDepartmentId", managingDepartmentId)
                .withEquals("userId", userId)
                .withEquals("userRole", userRole)

                .build();
    }

    // TODO
}
