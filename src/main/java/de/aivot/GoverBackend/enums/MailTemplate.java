package de.aivot.GoverBackend.enums;

import de.aivot.GoverBackend.lib.Identifiable;

public enum MailTemplate implements Identifiable<String> {
    ExceptionMail("system-mail-exception"),
    SystemInfoMail("system-mail-info"),
    DestinationMail("destination-mail"),
    SmtpTestMail("system-test-mail"),
    CustomerMail("customer-mail");

    private final String key;

    private MailTemplate(String key) {
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
