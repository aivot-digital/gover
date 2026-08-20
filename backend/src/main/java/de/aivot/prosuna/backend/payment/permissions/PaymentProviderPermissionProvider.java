package de.aivot.prosuna.backend.payment.permissions;

import de.aivot.prosuna.backend.permissions.models.PermissionEntry;
import de.aivot.prosuna.backend.permissions.models.PermissionProvider;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PaymentProviderPermissionProvider implements PermissionProvider {
    public static final String PAYMENT_PROVIDER_CREATE = "payment_provider.create";
    public static final String PAYMENT_PROVIDER_READ = "payment_provider.read";
    public static final String PAYMENT_PROVIDER_UPDATE = "payment_provider.update";
    public static final String PAYMENT_PROVIDER_DELETE = "payment_provider.delete";

    @Override
    public String getContextLabel() {
        return "Zahlungsanbieter";
    }

    @Override
    public PermissionEntry[] getPermissions() {
        return new PermissionEntry[]{
                PermissionEntry.of(PAYMENT_PROVIDER_CREATE, "Zahlungsanbieter erstellen", "Erlaubt das Erstellen von Zahlungsanbietern."),
                PermissionEntry.of(PAYMENT_PROVIDER_READ, "Zahlungsanbieter anzeigen", "Erlaubt das Anzeigen und Auflisten von Zahlungsanbietern."),
                PermissionEntry.of(PAYMENT_PROVIDER_UPDATE, "Zahlungsanbieter bearbeiten", "Erlaubt das Bearbeiten von Zahlungsanbietern."),
                PermissionEntry.of(PAYMENT_PROVIDER_DELETE, "Zahlungsanbieter löschen", "Erlaubt das Löschen von Zahlungsanbietern."),
        };
    }

    @Nonnull
    @Override
    public Optional<SearchPermission> getSearchPermission() {
        return Optional.of(new PermissionProvider.SearchPermission(
                "payment_providers",
                PAYMENT_PROVIDER_READ
        ));
    }
}
