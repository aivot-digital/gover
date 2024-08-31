package de.aivot.GoverBackend.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.GoverBackend.lib.Identifiable;

public enum SubmissionStatus implements Identifiable<Integer> {
    /**
     * Payment is pending
     */
    PaymentPending(-1),

    /**
     * Callback from destination is pending
     */
    DestinationCallbackPending(-2),

    /**
     * Submission is received, payment is done and is open for working. No destination was called
     */
    OpenForManualWork(0),

    /**
     * Submission is received, payment is done and a staff member is assigned. No destination was called
     */
    ManualWorkingOn(1),

    /**
     * Submission is received but payment has failed
     */
    HasPaymentError(500),

    /**
     * Submission is received, payment is done (if necessary) and destination submit was unsuccessful
     */
    HasDestinationError(501),

    /**
     * Submission is received, payment is done and destination submit was successful or staff member was done working
     */
    Archived(999);

    private final Integer key;

    SubmissionStatus(Integer key) {
        this.key = key;
    }

    @Override
    @JsonValue
    public Integer getKey() {
        return key;
    }

    @Override
    public boolean matches(Object other) {
        return key.equals(other);
    }
}
