package de.aivot.prosuna.backend.mail.dtos;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public record MailConfigurationResponseDTO(
        boolean configured,
        @Nullable String host,
        @Nullable Integer port,
        boolean authenticationEnabled,
        @Nullable String maskedUsername,
        boolean passwordConfigured,
        boolean startTlsEnabled,
        @Nullable String senderName,
        @Nullable String senderAddress,
        @Nonnull List<String> configurationIssues
) {
}
