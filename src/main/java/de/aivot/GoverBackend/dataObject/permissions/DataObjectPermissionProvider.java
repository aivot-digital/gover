package de.aivot.GoverBackend.dataObject.permissions;

import de.aivot.GoverBackend.permissions.enums.PermissionScope;
import de.aivot.GoverBackend.permissions.models.PermissionEntry;
import de.aivot.GoverBackend.permissions.models.PermissionProvider;
import org.springframework.stereotype.Component;

@Component
public class DataObjectPermissionProvider implements PermissionProvider {
    public static final String OBJECT_SCHEMA_CREATE = "object_schema.create";
    public static final String OBJECT_SCHEMA_READ = "object_schema.read";
    public static final String OBJECT_SCHEMA_UPDATE = "object_schema.update";
    public static final String OBJECT_SCHEMA_DELETE = "object_schema.delete";

    public static final String OBJECT_ITEM_CREATE = "object_item.create";
    public static final String OBJECT_ITEM_READ = "object_item.read";
    public static final String OBJECT_ITEM_UPDATE = "object_item.update";
    public static final String OBJECT_ITEM_DELETE = "object_item.delete";

    @Override
    public String getContextLabel() {
        return "Datenobjekte";
    }

    @Override
    public PermissionEntry[] getPermissions() {
        return new PermissionEntry[]{
                PermissionEntry.of(OBJECT_SCHEMA_CREATE, "Objektschema erstellen", "Erlaubt das Erstellen von Objektschemata."),
                PermissionEntry.of(OBJECT_SCHEMA_READ, "Objektschema anzeigen", "Erlaubt das Anzeigen und Auflisten von Objektschemata."),
                PermissionEntry.of(OBJECT_SCHEMA_UPDATE, "Objektschema bearbeiten", "Erlaubt das Bearbeiten von Objektschemata."),
                PermissionEntry.of(OBJECT_SCHEMA_DELETE, "Objektschema löschen", "Erlaubt das Löschen von Objektschemata."),
                PermissionEntry.of(OBJECT_ITEM_CREATE, "Objekt erstellen", "Erlaubt das Erstellen von Objekten."),
                PermissionEntry.of(OBJECT_ITEM_READ, "Objekt anzeigen", "Erlaubt das Anzeigen und Auflisten von Objekten."),
                PermissionEntry.of(OBJECT_ITEM_UPDATE, "Objekt bearbeiten", "Erlaubt das Bearbeiten von Objekten."),
                PermissionEntry.of(OBJECT_ITEM_DELETE, "Objekt löschen", "Erlaubt das Löschen von Objekten."),
        };
    }

    @Override
    public PermissionScope getScope() {
        return PermissionScope.System;
    }
}
