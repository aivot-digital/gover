package de.aivot.GoverBackend.models.dtos;

import de.aivot.GoverBackend.models.entities.Submission;

import java.util.Map;


public class SubmissionDetailsDto extends SubmissionListDto {

    private Map<String, Object> customerInput;

    public SubmissionDetailsDto() {
    }

    public SubmissionDetailsDto(Submission submission) {
        super(submission);

        customerInput = submission.getCustomerInput();
    }

    // region Getters & Setters

    public Map<String, Object> getCustomerInput() {
        return customerInput;
    }

    public void setCustomerInput(Map<String, Object> customerInput) {
        this.customerInput = customerInput;
    }

    // endregion
}
