package de.aivot.gover.backend.teams.permissions;

import de.aivot.gover.backend.permissions.models.PermissionEntry;
import de.aivot.gover.backend.permissions.models.PermissionProvider;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
public class TeamPermissionProvider implements PermissionProvider {
    public static final String TEAM_CREATE = "team.create";
    public static final String TEAM_READ = "team.read";
    public static final String TEAM_UPDATE = "team.update";
    public static final String TEAM_DELETE = "team.delete";

    public static final String TEAM_MEMBERSHIP_CREATE = "team_membership.create";
    public static final String TEAM_MEMBERSHIP_READ = "team_membership.read";
    public static final String TEAM_MEMBERSHIP_UPDATE = "team_membership.update";
    public static final String TEAM_MEMBERSHIP_DELETE = "team_membership.delete";

    @Override
    public String getContextLabel() {
        return "Teams";
    }

    @Override
    public PermissionEntry[] getPermissions() {
        return new PermissionEntry[]{
                PermissionEntry.of(TEAM_CREATE, "Team erstellen", "Erlaubt das Erstellen von Teams."),
                PermissionEntry.of(TEAM_READ, "Team anzeigen", "Erlaubt das Anzeigen und Auflisten von Teams."),
                PermissionEntry.of(TEAM_UPDATE, "Team bearbeiten", "Erlaubt das Bearbeiten von Teams."),
                PermissionEntry.of(TEAM_DELETE, "Team löschen", "Erlaubt das Löschen von Teams."),
                PermissionEntry.of(TEAM_MEMBERSHIP_CREATE, "Teammitgliedschaft erstellen", "Erlaubt das Erstellen von Teammitgliedschaften."),
                PermissionEntry.of(TEAM_MEMBERSHIP_READ, "Teammitgliedschaft anzeigen", "Erlaubt das Anzeigen und Auflisten von Teammitgliedschaften."),
                PermissionEntry.of(TEAM_MEMBERSHIP_UPDATE, "Teammitgliedschaft bearbeiten", "Erlaubt das Bearbeiten von Teammitgliedschaften."),
                PermissionEntry.of(TEAM_MEMBERSHIP_DELETE, "Teammitgliedschaft löschen", "Erlaubt das Löschen von Teammitgliedschaften."),
        };
    }

    @Override
    public boolean supportsDomainRoleAssignment() {
        return true;
    }

    @Override
    public Set<String> getExcludedFromDomainRoleAssignment() {
        return Set.of(TEAM_CREATE);
    }

    @Override
    public String getDomainRoleAssignmentHint() {
        return "Das Erstellen von Teams ist systemweit geregelt und kann nicht über Domänenrollen vergeben werden.";
    }

    @Nonnull
    @Override
    public Optional<SearchPermission> getSearchPermission() {
        return Optional.of(new PermissionProvider.SearchPermission(
                "teams",
                TEAM_READ
        ));
    }
}
