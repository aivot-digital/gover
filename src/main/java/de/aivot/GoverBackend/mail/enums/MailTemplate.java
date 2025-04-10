package de.aivot.GoverBackend.mail.enums;

import de.aivot.GoverBackend.lib.models.Identifiable;
import de.aivot.GoverBackend.mail.config.*;

import javax.annotation.Nullable;

public enum MailTemplate implements Identifiable<String> {
    CustomerSubmissionCopy("customer-submission-copy", null),

    DepartmentMembershipAdded("department-membership-added", DepartmentNotificationMembershipAddedUserConfigDefinition.KEY),
    DepartmentMembershipRemoved("department-membership-removed", DepartmentNotificationMembershipRemovedUserConfigDefinition.KEY),
    DepartmentMembershipRoleChanged("department-membership-role-changed", DepartmentNotificationMembershipChangedUserConfigDefinition.KEY),

    FormAdded("form-added", FormNotificationAddedFormUserConfigDefinition.KEY),
    FormDeleted("form-deleted", FormNotificationDeletedFormUserConfigDefinition.KEY),
    FormPublished("form-published", FormNotificationPublishedFormUserConfigDefinition.KEY),
    FormRevoked("form-revoked", FormNotificationRevokedFormUserConfigDefinition.KEY),

    SmtpTest("smtp-test", null),

    SubmissionArchived("submission-archived", SubmissionNotificationArchivedUserConfigDefinition.KEY),
    SubmissionAssigned("submission-assigned", SubmissionNotificationAssignedUserConfigDefinition.KEY),
    SubmissionReassigned("submission-reassigned", SubmissionNotificationAssignedUserConfigDefinition.KEY),
    SubmissionDestination("submission-destination", null),
    SubmissionDestinationFailed("submission-destination-failed", SubmissionNotificationDestinationUserConfigDefinition.KEY),
    SubmissionPaymentFailed("submission-payment-failed", SubmissionNotificationPaymentFailedUserConfigDefinition.KEY),
    SubmissionReceived("submission-received", SubmissionNotificationReceivedUserConfigDefinition.KEY),

    UnhandledSystemException("unhandled-system-exception", null),
    ;

    private final String key;

    @Nullable
    private final String userConfigKey;

    MailTemplate(String key, @Nullable String userConfigKey) {
        this.key = key;
        this.userConfigKey = userConfigKey;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public boolean matches(Object other) {
        return key.equals(other);
    }

    @Nullable
    public String getUserConfigKey() {
        return userConfigKey;
    }
}
