package de.aivot.prosuna.backend.elements.models.elements.form.input;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class AssignmentContextInputElementValue implements Serializable {
    public static final String GENERAL_ASSIGNEE_PREFERENCE_PREVIOUS_PROCESS_STEP_ASSIGNEE = "previousProcessStepAssignee";
    public static final String GENERAL_ASSIGNEE_PREFERENCE_UNINVOLVED_USER = "uninvolvedUser";
    public static final String GENERAL_ASSIGNEE_PREFERENCE_PROCESS_INSTANCE_ASSIGNEE = "processInstanceAssignee";

    public static final String REPEAT_EXECUTION_ASSIGNEE_PREFERENCE_PREVIOUS_ITERATION_ASSIGNEE = "previousIterationAssignee";
    public static final String REPEAT_EXECUTION_ASSIGNEE_PREFERENCE_DIFFERENT_FROM_PREVIOUS_ITERATION_ASSIGNEE = "differentFromPreviousIterationAssignee";

    @Nullable
    private List<DomainAndUserSelectInputElementValue> domainAndUserSelection;

    @Nullable
    private String generalAssigneePreference;

    @Nullable
    private String repeatExecutionAssigneePreference;

    @JsonIgnore
    public boolean isEmpty() {
        var hasSelection = domainAndUserSelection != null && !domainAndUserSelection.isEmpty();
        var hasPreference = generalAssigneePreference != null
                || repeatExecutionAssigneePreference != null;

        return !hasSelection && !hasPreference;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AssignmentContextInputElementValue that = (AssignmentContextInputElementValue) o;
        return Objects.equals(domainAndUserSelection, that.domainAndUserSelection)
                && Objects.equals(generalAssigneePreference, that.generalAssigneePreference)
                && Objects.equals(repeatExecutionAssigneePreference, that.repeatExecutionAssigneePreference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(domainAndUserSelection, generalAssigneePreference, repeatExecutionAssigneePreference);
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
    public String getGeneralAssigneePreference() {
        return generalAssigneePreference;
    }

    public AssignmentContextInputElementValue setGeneralAssigneePreference(@Nullable String generalAssigneePreference) {
        if (generalAssigneePreference == null || generalAssigneePreference.isBlank() || "none".equalsIgnoreCase(generalAssigneePreference)) {
            this.generalAssigneePreference = null;
        } else if (GENERAL_ASSIGNEE_PREFERENCE_PREVIOUS_PROCESS_STEP_ASSIGNEE.equalsIgnoreCase(generalAssigneePreference)) {
            this.generalAssigneePreference = GENERAL_ASSIGNEE_PREFERENCE_PREVIOUS_PROCESS_STEP_ASSIGNEE;
        } else if (GENERAL_ASSIGNEE_PREFERENCE_UNINVOLVED_USER.equalsIgnoreCase(generalAssigneePreference)) {
            this.generalAssigneePreference = GENERAL_ASSIGNEE_PREFERENCE_UNINVOLVED_USER;
        } else if (GENERAL_ASSIGNEE_PREFERENCE_PROCESS_INSTANCE_ASSIGNEE.equalsIgnoreCase(generalAssigneePreference)) {
            this.generalAssigneePreference = GENERAL_ASSIGNEE_PREFERENCE_PROCESS_INSTANCE_ASSIGNEE;
        } else {
            this.generalAssigneePreference = generalAssigneePreference.trim();
        }
        return this;
    }

    @Nullable
    public String getRepeatExecutionAssigneePreference() {
        return repeatExecutionAssigneePreference;
    }

    public AssignmentContextInputElementValue setRepeatExecutionAssigneePreference(@Nullable String repeatExecutionAssigneePreference) {
        if (repeatExecutionAssigneePreference == null || repeatExecutionAssigneePreference.isBlank() || "none".equalsIgnoreCase(repeatExecutionAssigneePreference)) {
            this.repeatExecutionAssigneePreference = null;
        } else if (REPEAT_EXECUTION_ASSIGNEE_PREFERENCE_PREVIOUS_ITERATION_ASSIGNEE.equalsIgnoreCase(repeatExecutionAssigneePreference)) {
            this.repeatExecutionAssigneePreference = REPEAT_EXECUTION_ASSIGNEE_PREFERENCE_PREVIOUS_ITERATION_ASSIGNEE;
        } else if (REPEAT_EXECUTION_ASSIGNEE_PREFERENCE_DIFFERENT_FROM_PREVIOUS_ITERATION_ASSIGNEE.equalsIgnoreCase(repeatExecutionAssigneePreference)) {
            this.repeatExecutionAssigneePreference = REPEAT_EXECUTION_ASSIGNEE_PREFERENCE_DIFFERENT_FROM_PREVIOUS_ITERATION_ASSIGNEE;
        } else {
            this.repeatExecutionAssigneePreference = repeatExecutionAssigneePreference.trim();
        }
        return this;
    }

    @JsonIgnore
    public boolean prefersPreviousProcessStepAssignee() {
        return GENERAL_ASSIGNEE_PREFERENCE_PREVIOUS_PROCESS_STEP_ASSIGNEE.equals(generalAssigneePreference);
    }

    @JsonIgnore
    public boolean prefersUninvolvedUser() {
        return GENERAL_ASSIGNEE_PREFERENCE_UNINVOLVED_USER.equals(generalAssigneePreference);
    }

    @JsonIgnore
    public boolean prefersProcessInstanceAssignee() {
        return GENERAL_ASSIGNEE_PREFERENCE_PROCESS_INSTANCE_ASSIGNEE.equals(generalAssigneePreference);
    }

    @JsonIgnore
    public boolean prefersPreviousIterationAssignee() {
        return REPEAT_EXECUTION_ASSIGNEE_PREFERENCE_PREVIOUS_ITERATION_ASSIGNEE.equals(repeatExecutionAssigneePreference);
    }

    @JsonIgnore
    public boolean prefersDifferentFromPreviousIterationAssignee() {
        return REPEAT_EXECUTION_ASSIGNEE_PREFERENCE_DIFFERENT_FROM_PREVIOUS_ITERATION_ASSIGNEE.equals(repeatExecutionAssigneePreference);
    }
}
