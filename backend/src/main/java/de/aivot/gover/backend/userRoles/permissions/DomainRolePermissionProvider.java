package de.aivot.gover.backend.userRoles.permissions;

import de.aivot.gover.backend.permissions.models.PermissionEntry;
import de.aivot.gover.backend.permissions.models.PermissionProvider;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DomainRolePermissionProvider implements PermissionProvider {
    public static final String DOMAIN_ROLE_CREATE = "domain_role.create";
    public static final String DOMAIN_ROLE_READ = "domain_role.read";
    public static final String DOMAIN_ROLE_UPDATE = "domain_role.update";
    public static final String DOMAIN_ROLE_DELETE = "domain_role.delete";

    @Override
    public String getContextLabel() {
        return "Domänenrollen";
    }

    @Override
    public PermissionEntry[] getPermissions() {
        return new PermissionEntry[]{
                PermissionEntry.of(DOMAIN_ROLE_CREATE, "Domänenrolle erstellen", "Erlaubt das Erstellen von Domänenrollen."),
                PermissionEntry.of(DOMAIN_ROLE_READ, "Domänenrolle anzeigen", "Erlaubt das Anzeigen und Auflisten von Domänenrollen."),
                PermissionEntry.of(DOMAIN_ROLE_UPDATE, "Domänenrolle bearbeiten", "Erlaubt das Bearbeiten von Domänenrollen."),
                PermissionEntry.of(DOMAIN_ROLE_DELETE, "Domänenrolle löschen", "Erlaubt das Löschen von Domänenrollen."),
        };
    }

    @Nonnull
    @Override
    public Optional<SearchPermission> getSearchPermission() {
        return Optional.of(new PermissionProvider.SearchPermission(
                "domain_roles",
                DOMAIN_ROLE_READ
        ));
    }
}
