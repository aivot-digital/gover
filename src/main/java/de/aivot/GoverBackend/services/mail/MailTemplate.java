package de.aivot.GoverBackend.services.mail;

import de.aivot.GoverBackend.lib.Identifiable;

public enum MailTemplate implements Identifiable<String> {
    CustomerSubmissionCopy("customer-submission-copy"),

    DepartmentMembershipAdded("department-membership-added"),
    DepartmentMembershipRemoved("department-membership-removed"),

    FormAdded("form-added"),
    FormDeleted("form-deleted"),
    FormPublished("form-published"),
    FormRevoked("form-revoked"),

    SmtpTest("smtp-test"),

    SubmissionArchived("submission-archived"),
    SubmissionAssigned("submission-assigned"),
    SubmissionDestination("submission-destination"),
    SubmissionDestinationFailed("submission-destination-failed"),
    SubmissionReceived("submission-received"),

    UnhandledSystemException("unhandled-system-exception"),
    ;

    private final String key;

    MailTemplate(String key) {
        this.key = key;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public boolean matches(Object other) {
        return key.equals(other);
    }
}
