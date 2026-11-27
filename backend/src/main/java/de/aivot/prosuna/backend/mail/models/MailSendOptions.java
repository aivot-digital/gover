package de.aivot.prosuna.backend.mail.models;

import jakarta.annotation.Nullable;

/**
 * Controls optional content and envelope headers applied by the central mail renderer.
 */
public record MailSendOptions(
        boolean includeDefaultMailSignature,
        @Nullable String senderName,
        @Nullable String senderAddress,
        @Nullable String replyToAddress
) {
    public MailSendOptions(boolean includeDefaultMailSignature) {
        this(includeDefaultMailSignature, null, null, null);
    }

    /**
     * Organization mails include their resolved default signature unless a caller explicitly opts out.
     */
    public static MailSendOptions defaults() {
        return new MailSendOptions(true, null, null, null);
    }
}
