package de.aivot.GoverBackend.user.configs;

import de.aivot.GoverBackend.config.entities.SystemConfigEntity;
import de.aivot.GoverBackend.config.enums.ConfigType;
import de.aivot.GoverBackend.config.models.SystemConfigDefinition;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.userRoles.repositories.SystemRoleRepository;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DefaultUserSystemRoleSystemConfigDefinition implements SystemConfigDefinition {
    public static final String KEY = "users.default_system_role";
    public static final Integer DEFAULT_SYSTEM_ROLE_ID = 3;

    private final SystemRoleRepository systemRoleRepository;

    @Autowired
    public DefaultUserSystemRoleSystemConfigDefinition(SystemRoleRepository systemRoleRepository) {
        this.systemRoleRepository = systemRoleRepository;
    }

    @Nonnull
    @Override
    public String getKey() {
        return KEY;
    }

    @Nonnull
    @Override
    public ConfigType getType() {
        return ConfigType.TEXT;
    }

    @Nonnull
    @Override
    public String getCategory() {
        return "Benutzer:innen";
    }

    @Nonnull
    @Override
    public String getLabel() {
        return "Standard-Systemrolle für automatische Importe";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Legt fest, welche Systemrolle Mitarbeiter:innen automatisch erhalten, wenn sie neu in Gover synchronisiert oder importiert werden.";
    }

    @Override
    public Object getDefaultValue() {
        return DEFAULT_SYSTEM_ROLE_ID;
    }

    @Override
    public void validate(@Nonnull SystemConfigEntity entity) throws ResponseException {
        var systemRoleId = entity.getValueAsInteger()
                .orElseThrow(() -> ResponseException.badRequest("Bitte wählen Sie eine gültige Standard-Systemrolle aus."));

        if (!systemRoleRepository.existsById(systemRoleId)) {
            throw ResponseException.badRequest("Die ausgewählte Standard-Systemrolle existiert nicht.");
        }
    }
}
