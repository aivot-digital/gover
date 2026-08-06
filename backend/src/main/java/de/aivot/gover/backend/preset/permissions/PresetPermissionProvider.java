package de.aivot.gover.backend.preset.permissions;

import de.aivot.gover.backend.permissions.models.PermissionEntry;
import de.aivot.gover.backend.permissions.models.PermissionProvider;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PresetPermissionProvider implements PermissionProvider {
    public static final String PRESET_CREATE = "preset.create";
    public static final String PRESET_READ = "preset.read";
    public static final String PRESET_UPDATE = "preset.update";
    public static final String PRESET_DELETE = "preset.delete";
    public static final String PRESET_PUBLISH_LOCAL = "preset.publish.local";
    public static final String PRESET_PUBLISH_STORE = "preset.publish.store";

    @Override
    public String getContextLabel() {
        return "Vorlagen";
    }

    @Override
    public PermissionEntry[] getPermissions() {
        return new PermissionEntry[]{
                PermissionEntry.of(PRESET_CREATE, "Vorlage erstellen", "Erlaubt das Erstellen von Vorlagen."),
                PermissionEntry.of(PRESET_READ, "Vorlage anzeigen", "Erlaubt das Anzeigen und Auflisten von Vorlagen."),
                PermissionEntry.of(PRESET_UPDATE, "Vorlage bearbeiten", "Erlaubt das Bearbeiten von Vorlagen."),
                PermissionEntry.of(PRESET_DELETE, "Vorlage löschen", "Erlaubt das Löschen von Vorlagen."),
                PermissionEntry.of(PRESET_PUBLISH_LOCAL, "Vorlage lokal veröffentlichen", "Erlaubt das lokale Veröffentlichen von Vorlagen."),
                PermissionEntry.of(PRESET_PUBLISH_STORE, "Vorlage im Store veröffentlichen", "Erlaubt das Veröffentlichen von Vorlagen im Store."),
        };
    }

    @Nonnull
    @Override
    public Optional<SearchPermission> getSearchPermission() {
        return Optional.of(new PermissionProvider.SearchPermission(
                "presets",
                PRESET_READ
        ));
    }
}
