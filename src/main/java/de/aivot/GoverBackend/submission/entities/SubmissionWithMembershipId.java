package de.aivot.GoverBackend.submission.entities;

import java.util.Objects;

public class SubmissionWithMembershipId {
    private String id;
    private Integer formId;
    private Integer formVersion;
    private String userId;

    public SubmissionWithMembershipId() {
    }

    public SubmissionWithMembershipId(String id, Integer formId, Integer formVersion, String userId) {
        this.id = id;
        this.formId = formId;
        this.formVersion = formVersion;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        SubmissionWithMembershipId that = (SubmissionWithMembershipId) object;
        return Objects.equals(id, that.id) && Objects.equals(formId, that.formId) && Objects.equals(formVersion, that.formVersion) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(formId);
        result = 31 * result + Objects.hashCode(formVersion);
        result = 31 * result + Objects.hashCode(userId);
        return result;
    }

    public String getId() {
        return id;
    }

    public SubmissionWithMembershipId setId(String id) {
        this.id = id;
        return this;
    }

    public Integer getFormId() {
        return formId;
    }

    public SubmissionWithMembershipId setFormId(Integer formId) {
        this.formId = formId;
        return this;
    }

    public Integer getFormVersion() {
        return formVersion;
    }

    public SubmissionWithMembershipId setFormVersion(Integer formVersion) {
        this.formVersion = formVersion;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public SubmissionWithMembershipId setUserId(String userId) {
        this.userId = userId;
        return this;
    }
}
