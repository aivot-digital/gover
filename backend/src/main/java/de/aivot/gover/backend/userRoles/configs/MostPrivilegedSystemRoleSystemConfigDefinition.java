package de.aivot.gover.backend.userRoles.configs;

import de.aivot.gover.backend.config.models.SystemConfigDefinition;
import de.aivot.gover.backend.elements.models.elements.BaseElement;
import de.aivot.gover.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.userRoles.repositories.SystemRoleRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MostPrivilegedSystemRoleSystemConfigDefinition implements SystemConfigDefinition<String> {
    public static final String KEY = "system_roles.most_privileged_role";
    public static final Integer DEFAULT_SYSTEM_ROLE_ID = 1;

    private final SystemRoleRepository systemRoleRepository;

    @Autowired
    public MostPrivilegedSystemRoleSystemConfigDefinition(SystemRoleRepository systemRoleRepository) {
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
        return "Systemrollen";
    }

    @Nonnull
    @Override
    public String getLabel() {
        return "Systemrolle mit höchster Berechtigungsstufe";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Diese Systemrolle gilt in Gover als höchste Berechtigungsstufe. Besitzt keine aktive Mitarbeiter:in diese Rolle, wird sie automatisch dem Administrationskonto zugewiesen, dessen E-Mail-Adresse über die Umgebungsvariable GOVER_BOOTSTRAP_ADMIN_MAIL konfiguriert ist.";
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

    @Override
    public void validate(@Nullable String value) throws ResponseException {
        if (value == null || !systemRoleRepository.existsById(Integer.parseInt(value))) {
            throw ResponseException.badRequest("Die ausgewählte Systemrolle existiert nicht.");
        }
    }
}
