package de.aivot.prosuna.backend.mail.config;

import de.aivot.prosuna.backend.config.enums.ConfigType;
import de.aivot.prosuna.backend.config.models.ConfigDefinitionOption;
import de.aivot.prosuna.backend.config.models.UserConfigDefinition;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class ProcessNotificationInstanceAssignmentChangedUserConfigDefinition implements UserConfigDefinition {
    public static final String KEY = "mail.notification.process.instance-assignment-changed";

    @Nonnull
    @Override
    public String getKey() {
        return KEY;
    }

    @Nonnull
    @Override
    public ConfigType getType() {
        return ConfigType.LIST;
    }

    @Nonnull
    @Override
    public String getCategory() {
        return "Benachrichtigungen";
    }

    @Nullable
    @Override
    public String getSubCategory() {
        return "Prozesse";
    }

    @Nonnull
    @Override
    public String getLabel() {
        return "Vorgangszuweisung geändert";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Erhalten Sie eine E-Mail, wenn Ihnen ein Vorgang zugewiesen wird oder Ihre Zuweisung endet.";
    }

    @Override
    public List<String> getDefaultValue() {
        return List.of("mail");
    }

    @Nonnull
    @Override
    public String serializeValueToDB(@Nullable Object value) throws ResponseException {
        if (value == null) {
            return "";
        }
        if (value instanceof List<?> list) {
            return String.join(",", (List<String>) list);
        }
        throw ResponseException.badRequest("Der Wert ist vom falschen Typ");
    }

    @Nullable
    @Override
    public List<String> parseValueFromDB(@Nonnull String value) throws ResponseException {
        if (value.isBlank()) {
            return null;
        }
        return Arrays.asList(value.split(","));
    }

    @Nullable
    @Override
    public List<ConfigDefinitionOption> getOptions() {
        return List.of(new ConfigDefinitionOption("E-Mail", "mail"));
    }

    @Override
    public int getSubCategoryOrder() {
        return 4;
    }

    @Override
    public int getDefinitionOrder() {
        return 2;
    }
}
