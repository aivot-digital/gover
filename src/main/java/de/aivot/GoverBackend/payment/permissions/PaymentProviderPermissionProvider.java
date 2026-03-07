package de.aivot.GoverBackend.payment.permissions;

import de.aivot.GoverBackend.permissions.enums.PermissionScope;
import de.aivot.GoverBackend.permissions.models.PermissionEntry;
import de.aivot.GoverBackend.permissions.models.PermissionProvider;
import org.springframework.stereotype.Component;

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

    @Override
    public PermissionScope getScope() {
        return PermissionScope.System;
    }
}
