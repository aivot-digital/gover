package de.aivot.GoverBackend.elements.models.elements.form.input;

import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class AssignmentContextInputElementValue implements Serializable {
    @Nullable
    private List<DomainAndUserSelectInputElementValue> domainAndUserSelection;

    @Nullable
    private Boolean preferPreviousTaskAssignee;

    @Nullable
    private Boolean preferUninvolvedUser;

    @Nullable
    private Boolean preferProcessInstanceAssignee;

    public boolean isEmpty() {
        var hasSelection = domainAndUserSelection != null && !domainAndUserSelection.isEmpty();
        var hasPreference = Boolean.TRUE.equals(preferPreviousTaskAssignee)
                || Boolean.TRUE.equals(preferUninvolvedUser)
                || Boolean.TRUE.equals(preferProcessInstanceAssignee);

        return !hasSelection && !hasPreference;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AssignmentContextInputElementValue that = (AssignmentContextInputElementValue) o;
        return Objects.equals(domainAndUserSelection, that.domainAndUserSelection)
                && Objects.equals(preferPreviousTaskAssignee, that.preferPreviousTaskAssignee)
                && Objects.equals(preferUninvolvedUser, that.preferUninvolvedUser)
                && Objects.equals(preferProcessInstanceAssignee, that.preferProcessInstanceAssignee);
    }

    @Override
    public int hashCode() {
        return Objects.hash(domainAndUserSelection, preferPreviousTaskAssignee, preferUninvolvedUser, preferProcessInstanceAssignee);
    }

    @Nullable
    public List<DomainAndUserSelectInputElementValue> getDomainAndUserSelection() {
        return domainAndUserSelection;
    }

    public AssignmentContextInputElementValue setDomainAndUserSelection(@Nullable List<DomainAndUserSelectInputElementValue> domainAndUserSelection) {
        this.domainAndUserSelection = domainAndUserSelection;
        return this;
    }

    @Nullable
    public Boolean getPreferPreviousTaskAssignee() {
        return preferPreviousTaskAssignee;
    }

    public AssignmentContextInputElementValue setPreferPreviousTaskAssignee(@Nullable Boolean preferPreviousTaskAssignee) {
        this.preferPreviousTaskAssignee = preferPreviousTaskAssignee;
        return this;
    }

    @Nullable
    public Boolean getPreferUninvolvedUser() {
        return preferUninvolvedUser;
    }

    public AssignmentContextInputElementValue setPreferUninvolvedUser(@Nullable Boolean preferUninvolvedUser) {
        this.preferUninvolvedUser = preferUninvolvedUser;
        return this;
    }

    @Nullable
    public Boolean getPreferProcessInstanceAssignee() {
        return preferProcessInstanceAssignee;
    }

    public AssignmentContextInputElementValue setPreferProcessInstanceAssignee(@Nullable Boolean preferProcessInstanceAssignee) {
        this.preferProcessInstanceAssignee = preferProcessInstanceAssignee;
        return this;
    }
}
