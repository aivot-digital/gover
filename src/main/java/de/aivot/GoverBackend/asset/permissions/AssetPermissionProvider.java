package de.aivot.GoverBackend.asset.permissions;

import de.aivot.GoverBackend.permissions.enums.PermissionScope;
import de.aivot.GoverBackend.permissions.models.PermissionEntry;
import de.aivot.GoverBackend.permissions.models.PermissionProvider;
import org.springframework.stereotype.Component;

@Component
public class AssetPermissionProvider implements PermissionProvider {
    public static final String ASSET_CREATE = "asset.create";
    public static final String ASSET_READ = "asset.read";
    public static final String ASSET_UPDATE = "asset.update";
    public static final String ASSET_DELETE = "asset.delete";

    @Override
    public String getContextLabel() {
        return "Dateien & Medien";
    }

    @Override
    public PermissionEntry[] getPermissions() {
        return new PermissionEntry[]{
                PermissionEntry.of(
                        ASSET_CREATE,
                        "Dateien & Medien erstellen",
                        "Erlaubt das Erstellen neuer Dateien und Medien."
                ),
                PermissionEntry.of(
                        ASSET_READ,
                        "Dateien & Medien anzeigen",
                        "Erlaubt das Anzeigen und Auflisten von Dateien und Medien."
                ),
                PermissionEntry.of(
                        ASSET_UPDATE,
                        "Dateien & Medien bearbeiten",
                        "Erlaubt das Bearbeiten bestehender Dateien und Medien."
                ),
                PermissionEntry.of(
                        ASSET_DELETE,
                        "Dateien & Medien löschen",
                        "Erlaubt das Löschen von Dateien und Medien."
                )
        };
    }

    @Override
    public PermissionScope getScope() {
        return PermissionScope.System;
    }
}
