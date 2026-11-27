package de.aivot.prosuna.backend.communication.services;

import de.aivot.prosuna.backend.identity.services.IdentityProviderService;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * Validates that identity providers used by process identities have at least one usable communication binding.
 */
@Service
public class IdentityCommunicationAvailabilityService {
    private static final String UNNAMED_IDENTITY = "Unbenannte Identität";

    private final IdentityProviderService identityProviderService;
    private final CommunicationService communicationService;

    public IdentityCommunicationAvailabilityService(IdentityProviderService identityProviderService,
                                                    CommunicationService communicationService) {
        this.identityProviderService = identityProviderService;
        this.communicationService = communicationService;
    }

    /**
     * Checks provider usages in declaration order and groups all affected identities into one error per provider.
     * Providers that no longer exist are ignored here because identity-configuration validation reports them separately.
     */
    @Nonnull
    public ValidationResult validate(@Nonnull Collection<IdentityProviderUsage> usages) {
        var identityNamesByProviderKey = new LinkedHashMap<UUID, LinkedHashSet<String>>();
        for (var usage : usages) {
            if (usage == null || usage.identityProviderKey() == null) {
                continue;
            }

            identityNamesByProviderKey
                    .computeIfAbsent(usage.identityProviderKey(), ignored -> new LinkedHashSet<>())
                    .add(resolveIdentityName(usage.identityName()));
        }

        var errors = new ArrayList<String>();
        try {
            for (var entry : identityNamesByProviderKey.entrySet()) {
                var identityProvider = identityProviderService
                        .retrieve(entry.getKey())
                        .orElse(null);
                if (identityProvider == null || !communicationService.getUsableBindings(identityProvider).isEmpty()) {
                    continue;
                }

                errors.add(String.format(
                        "Für den Identitätsanbieter \"%s\" (%s) ist keine verwendbare Kommunikationsanbindung konfiguriert.",
                        identityProvider.getName(),
                        String.join(", ", entry.getValue())
                ));
            }
        } catch (Exception ignored) {
            return new ValidationResult(false, List.of());
        }

        return new ValidationResult(true, List.copyOf(errors));
    }

    @Nonnull
    private static String resolveIdentityName(@Nullable String identityName) {
        var normalizedIdentityName = StringUtils.toNullableTrimmedString(identityName);
        return normalizedIdentityName == null ? UNNAMED_IDENTITY : normalizedIdentityName;
    }

    public record IdentityProviderUsage(
            @Nullable UUID identityProviderKey,
            @Nullable String identityName
    ) {
    }

    public record ValidationResult(
            boolean successful,
            @Nonnull List<String> errors
    ) {
    }
}
