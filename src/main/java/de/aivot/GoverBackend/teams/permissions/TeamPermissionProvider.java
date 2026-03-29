package de.aivot.GoverBackend.teams.permissions;

import de.aivot.GoverBackend.permissions.enums.PermissionScope;
import de.aivot.GoverBackend.permissions.models.PermissionEntry;
import de.aivot.GoverBackend.permissions.models.PermissionProvider;
import org.springframework.stereotype.Component;

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
    public PermissionScope getScope() {
        return PermissionScope.System;
    }
}
