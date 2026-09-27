package de.aivot.prosuna.backend.department.permissions;

import de.aivot.prosuna.backend.permissions.models.PermissionEntry;
import de.aivot.prosuna.backend.permissions.models.PermissionProvider;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
public class DepartmentPermissionProvider implements PermissionProvider {
    public static final String DEPARTMENT_CREATE = "department.create";
    public static final String DEPARTMENT_READ = "department.read";
    public static final String DEPARTMENT_UPDATE = "department.update";
    public static final String DEPARTMENT_DELETE = "department.delete";

    public static final String DEPARTMENT_MEMBERSHIP_CREATE = "department_membership.create";
    public static final String DEPARTMENT_MEMBERSHIP_READ = "department_membership.read";
    public static final String DEPARTMENT_MEMBERSHIP_UPDATE = "department_membership.update";
    public static final String DEPARTMENT_MEMBERSHIP_DELETE = "department_membership.delete";

    @Override
    public String getContextLabel() {
        return "Organisationseinheiten";
    }

    @Override
    public PermissionEntry[] getPermissions() {
        return new PermissionEntry[]{
                PermissionEntry.of(DEPARTMENT_CREATE, "Organisationseinheit erstellen", "Erlaubt das Erstellen von Organisationseinheiten."),
                PermissionEntry.of(DEPARTMENT_READ, "Organisationseinheit anzeigen", "Erlaubt das Anzeigen und Auflisten von Organisationseinheiten."),
                PermissionEntry.of(DEPARTMENT_UPDATE, "Organisationseinheit bearbeiten", "Erlaubt das Bearbeiten von Organisationseinheiten."),
                PermissionEntry.of(DEPARTMENT_DELETE, "Organisationseinheit löschen", "Erlaubt das Löschen von Organisationseinheiten."),
                PermissionEntry.of(DEPARTMENT_MEMBERSHIP_CREATE, "Mitgliedschaft erstellen", "Erlaubt das Erstellen von Mitgliedschaften in Organisationseinheiten."),
                PermissionEntry.of(DEPARTMENT_MEMBERSHIP_READ, "Mitgliedschaft anzeigen", "Erlaubt das Anzeigen und Auflisten von Mitgliedschaften in Organisationseinheiten."),
                PermissionEntry.of(DEPARTMENT_MEMBERSHIP_UPDATE, "Mitgliedschaft bearbeiten", "Erlaubt das Bearbeiten von Mitgliedschaften in Organisationseinheiten."),
                PermissionEntry.of(DEPARTMENT_MEMBERSHIP_DELETE, "Mitgliedschaft löschen", "Erlaubt das Löschen von Mitgliedschaften in Organisationseinheiten."),
        };
    }

    @Override
    public boolean supportsDomainRoleAssignment() {
        return true;
    }

    @Override
    public Set<String> getExcludedFromDomainRoleAssignment() {
        return Set.of(DEPARTMENT_CREATE);
    }

    @Override
    public String getDomainRoleAssignmentHint() {
        return "Das Erstellen von Organisationseinheiten ist systemweit geregelt und kann nicht über Domänenrollen vergeben werden.";
    }

    @Nonnull
    @Override
    public Optional<SearchPermission> getSearchPermission() {
        return Optional.of(new PermissionProvider.SearchPermission(
                "departments",
                DEPARTMENT_READ
        ));
    }
}
