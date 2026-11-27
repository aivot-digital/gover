package de.aivot.prosuna.backend.communication.models;

import jakarta.annotation.Nullable;

/** Optional mail headers for messages sent through the default mail transport. */
public record MailCommunicationSendOptions(
        boolean useDefaultSender,
        @Nullable String senderName,
        @Nullable String senderAddress,
        @Nullable String replyToAddress
) {
    public static MailCommunicationSendOptions defaults() {
        return defaultSender(null);
    }

    public static MailCommunicationSendOptions defaultSender(@Nullable String replyToAddress) {
        return new MailCommunicationSendOptions(true, null, null, replyToAddress);
    }

    public static MailCommunicationSendOptions customSender(
            @Nullable String senderName,
            @Nullable String senderAddress,
            @Nullable String replyToAddress
    ) {
        return new MailCommunicationSendOptions(false, senderName, senderAddress, replyToAddress);
    }
}
