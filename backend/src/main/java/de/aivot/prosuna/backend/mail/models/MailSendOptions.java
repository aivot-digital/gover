package de.aivot.prosuna.backend.mail.models;

/**
 * Controls optional content added by the central mail renderer rather than by an individual template.
 */
public record MailSendOptions(
        boolean includeDefaultMailSignature
) {
    /**
     * Organization mails include their resolved default signature unless a caller explicitly opts out.
     */
    public static MailSendOptions defaults() {
        return new MailSendOptions(true);
    }
}
