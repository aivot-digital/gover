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
public class SubmissionNotificationAssignedUserConfigDefinition implements UserConfigDefinition {
    public static final String KEY = "mail.notification.submission.assigned";

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
        return "Anträge";
    }

    @Nonnull
    @Override
    public String getLabel() {
        return "Antrag zugewiesen";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Erhalten Sie eine Benachrichtigung, wenn Ihnen ein Antrag zur Bearbeitung zugewiesen wird – oder wenn ein Ihnen bereits zugewiesener Antrag neu zugeordnet wird.";
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
        return List.of(
                new ConfigDefinitionOption("E-Mail", "mail"),
                new ConfigDefinitionOption("In-App", "app")
        );
    }

    @Override
    public int getSubCategoryOrder() {
        return 3;
    }

    @Override
    public int getDefinitionOrder() {
        return 10;
    }
}
