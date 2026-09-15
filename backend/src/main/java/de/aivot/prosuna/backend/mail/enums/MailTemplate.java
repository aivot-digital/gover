package de.aivot.prosuna.backend.mail.enums;

import de.aivot.prosuna.backend.lib.models.Identifiable;

import de.aivot.prosuna.backend.mail.config.*;
import jakarta.annotation.Nullable;

public enum MailTemplate implements Identifiable<String> {
    DepartmentMembershipAdded("department-membership-added", DepartmentNotificationMembershipAddedUserConfigDefinition.KEY),
    DepartmentMembershipRemoved("department-membership-removed", DepartmentNotificationMembershipRemovedUserConfigDefinition.KEY),
    DepartmentMembershipRoleChanged("department-membership-role-changed", DepartmentNotificationMembershipChangedUserConfigDefinition.KEY),

    GenericEmailMessage("generic-email-message", null),

    SmtpTest("smtp-test", null),
    StaffAccountCredentials("staff-account-credentials", null),

    ProcessEmail("process-email", null),
    ProcessTaskAssigned("process-task-assigned", ProcessNotificationTaskAssignedUserConfigDefinition.KEY),
    ProcessPaymentRequested("process-payment-requested", null),

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
