package de.aivot.prosuna.backend.user.configs;

import de.aivot.prosuna.backend.config.models.SystemConfigDefinition;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.userRoles.repositories.SystemRoleRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DefaultUserSystemRoleSystemConfigDefinition implements SystemConfigDefinition<String> {
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
    public BaseElement getConfigElement() {
        return new SelectInputElement()
                .setOptions(
                        systemRoleRepository
                                .findAll()
                                .stream()
                                .map(role -> SelectInputElementOption.of(role.getId().toString(), role.getName()))
                                .toList()
                )
                .setLabel(getLabel())
                .setHint(getDescription())
                .setId(getKey());
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
        return "Legt fest, welche Systemrolle Mitarbeiter:innen automatisch erhalten, wenn sie neu in Prosuna synchronisiert oder importiert werden.";
    }

    @Override
    public String getDefaultValue() {
        return DEFAULT_SYSTEM_ROLE_ID.toString();
    }

    @Nullable
    @Override
    public String parseValueFromDB(@Nonnull String value) throws ResponseException {
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw ResponseException.internalServerError("Ungültiger Wert für " + getKey() + ": " + value);
        }
        return value;
    }
}
