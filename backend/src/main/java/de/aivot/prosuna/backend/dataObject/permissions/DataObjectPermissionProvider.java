package de.aivot.prosuna.backend.dataObject.permissions;

import de.aivot.prosuna.backend.permissions.models.PermissionEntry;
import de.aivot.prosuna.backend.permissions.models.PermissionProvider;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.List;

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
                PermissionEntry.of(OBJECT_SCHEMA_CREATE, "Datenmodell erstellen", "Erlaubt das Erstellen von Datenmodellen."),
                PermissionEntry.of(OBJECT_SCHEMA_READ, "Datenmodell anzeigen", "Erlaubt das Anzeigen und Auflisten von Datenmodellen."),
                PermissionEntry.of(OBJECT_SCHEMA_UPDATE, "Datenmodell bearbeiten", "Erlaubt das Bearbeiten von Datenmodellen."),
                PermissionEntry.of(OBJECT_SCHEMA_DELETE, "Datenmodell löschen", "Erlaubt das Löschen von Datenmodellen."),
                PermissionEntry.of(OBJECT_ITEM_CREATE, "Datenobjekt erstellen", "Erlaubt das Erstellen von Datenobjekten."),
                PermissionEntry.of(OBJECT_ITEM_READ, "Datenobjekt anzeigen", "Erlaubt das Anzeigen und Auflisten von Datenobjekten."),
                PermissionEntry.of(OBJECT_ITEM_UPDATE, "Datenobjekt bearbeiten", "Erlaubt das Bearbeiten von Datenobjekten."),
                PermissionEntry.of(OBJECT_ITEM_DELETE, "Datenobjekt löschen", "Erlaubt das Löschen von Datenobjekten."),
        };
    }

    @Nonnull
    @Override
    public List<SearchPermission> getSearchPermissions() {
        return List.of(
                new PermissionProvider.SearchPermission(
                        "data_object_items",
                        OBJECT_ITEM_READ
                ),
                new PermissionProvider.SearchPermission(
                        "data_object_schemas",
                        OBJECT_SCHEMA_READ
                )
        );
    }
}
