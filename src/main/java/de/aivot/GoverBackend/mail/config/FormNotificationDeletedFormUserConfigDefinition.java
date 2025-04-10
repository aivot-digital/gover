package de.aivot.GoverBackend.mail.config;

import de.aivot.GoverBackend.config.enums.ConfigType;
import de.aivot.GoverBackend.config.models.ConfigDefinitionOption;
import de.aivot.GoverBackend.config.models.UserConfigDefinition;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

@Component
public class FormNotificationDeletedFormUserConfigDefinition implements UserConfigDefinition {
    public static final String KEY = "mail.notification.form.deleted";

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
        return "Formulare";
    }

    @Nonnull
    @Override
    public String getLabel() {
        return "Formular gelöscht";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Sie werden informiert, wenn in einem entwickelnden Fachbereich, dem Sie angehören, ein Formular gelöscht wird.";
    }

    @Override
    public List<String> getDefaultValue() {
        return List.of("mail", "app");
    }

    @Nonnull
    @Override
    public String serializeValueToDB(@Nullable Object value) throws ResponseException {
        if (value == null) {
            return "";
        }
        if (value instanceof List) {
            return String.join(",", (List<String>) value);
        }
        throw ResponseException.badRequest("Der Wert ist vom falschen Typ");
    }

    @Nullable
    @Override
    public Object parseValueFromDB(@Nonnull String value) throws ResponseException {
        if (value.isBlank()) {
            return null;
        }
        return Arrays.asList(value.split(","));
    }

    @Nullable
    @Override
    public List<ConfigDefinitionOption> getOptions() {
        return List.of(
                new ConfigDefinitionOption("E-Mail", "mail"),
                new ConfigDefinitionOption("In-App", "app")
        );
    }

    @Override
    public int getSubCategoryOrder() {
        return 2;
    }

    @Override
    public int getDefinitionOrder() {
        return 30;
    }
}
