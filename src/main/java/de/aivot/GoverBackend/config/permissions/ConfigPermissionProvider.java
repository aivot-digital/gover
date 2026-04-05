package de.aivot.GoverBackend.config.permissions;

import de.aivot.GoverBackend.permissions.enums.PermissionScope;
import de.aivot.GoverBackend.permissions.models.PermissionEntry;
import de.aivot.GoverBackend.permissions.models.PermissionProvider;
import org.springframework.stereotype.Component;

@Component
public class ConfigPermissionProvider implements PermissionProvider {
    public static final String SYSTEM_CONFIG_CREATE = "system_config.create";
    public static final String SYSTEM_CONFIG_READ = "system_config.read";
    public static final String SYSTEM_CONFIG_UPDATE = "system_config.update";
    public static final String SYSTEM_CONFIG_DELETE = "system_config.delete";

    public static final String USER_CONFIG_CREATE = "user_config.create";
    public static final String USER_CONFIG_READ = "user_config.read";
    public static final String USER_CONFIG_UPDATE = "user_config.update";
    public static final String USER_CONFIG_DELETE = "user_config.delete";

    @Override
    public String getContextLabel() {
        return "Konfiguration";
    }

    @Override
    public PermissionEntry[] getPermissions() {
        return new PermissionEntry[]{
                PermissionEntry.of(SYSTEM_CONFIG_CREATE, "Systemkonfiguration erstellen", "Erlaubt das Erstellen von Systemkonfigurationen."),
                PermissionEntry.of(SYSTEM_CONFIG_READ, "Systemkonfiguration anzeigen", "Erlaubt das Anzeigen und Auflisten von Systemkonfigurationen."),
                PermissionEntry.of(SYSTEM_CONFIG_UPDATE, "Systemkonfiguration bearbeiten", "Erlaubt das Bearbeiten von Systemkonfigurationen."),
                PermissionEntry.of(SYSTEM_CONFIG_DELETE, "Systemkonfiguration löschen", "Erlaubt das Löschen von Systemkonfigurationen."),
                PermissionEntry.of(USER_CONFIG_CREATE, "Benutzerkonfiguration erstellen", "Erlaubt das Erstellen von Benutzerkonfigurationen."),
                PermissionEntry.of(USER_CONFIG_READ, "Benutzerkonfiguration anzeigen", "Erlaubt das Anzeigen und Auflisten von Benutzerkonfigurationen."),
                PermissionEntry.of(USER_CONFIG_UPDATE, "Benutzerkonfiguration bearbeiten", "Erlaubt das Bearbeiten von Benutzerkonfigurationen."),
                PermissionEntry.of(USER_CONFIG_DELETE, "Benutzerkonfiguration löschen", "Erlaubt das Löschen von Benutzerkonfigurationen."),
        };
    }

    @Override
    public PermissionScope getScope() {
        return PermissionScope.System;
    }
}
