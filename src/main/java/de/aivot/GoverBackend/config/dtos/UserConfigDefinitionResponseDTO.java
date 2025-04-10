package de.aivot.GoverBackend.config.dtos;

import de.aivot.GoverBackend.config.enums.ConfigType;
import de.aivot.GoverBackend.config.models.ConfigDefinitionOption;
import de.aivot.GoverBackend.config.models.UserConfigDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public record UserConfigDefinitionResponseDTO(
        @Nonnull String key,
        @Nonnull ConfigType type,
        @Nonnull String category,
        @Nullable String subCategory,
        @Nonnull String label,
        @Nonnull String description,
        @Nonnull Boolean isPublicConfig,
        @Nullable List<ConfigDefinitionOption> options,
        @Nullable Object defaultValue
) {
    @Nonnull
    public static UserConfigDefinitionResponseDTO fromDefinition(
            @Nonnull UserConfigDefinition configDefinition
    ) {
        return new UserConfigDefinitionResponseDTO(
                configDefinition.getKey(),
                configDefinition.getType(),
                configDefinition.getCategory(),
                configDefinition.getSubCategory(),
                configDefinition.getLabel(),
                configDefinition.getDescription(),
                configDefinition.isPublicConfig(),
                configDefinition.getOptions(),
                configDefinition.getDefaultValue()
        );
    }
}
