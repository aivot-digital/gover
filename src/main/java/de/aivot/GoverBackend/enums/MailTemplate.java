package de.aivot.GoverBackend.enums;

import de.aivot.GoverBackend.lib.Identifiable;

public enum MailTemplate implements Identifiable<String> {
    CustomerMail("customer-mail"),
    DestinationMail("destination-mail"),
    NewSubmissionMail("new-submission-mail"),
    SystemExceptionMail("system-exception-mail"),
    SystemTestMail("system-test-mail"),
    UserCreatedMail("user-created-mail"),
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
