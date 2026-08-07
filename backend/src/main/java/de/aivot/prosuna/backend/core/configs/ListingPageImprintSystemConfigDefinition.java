package de.aivot.prosuna.backend.core.configs;

import de.aivot.prosuna.backend.config.models.SystemConfigDefinition;
import de.aivot.prosuna.backend.data.SystemConfigKey;
import de.aivot.prosuna.backend.department.repositories.VDepartmentShadowedRepository;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

@Component
public class ListingPageImprintSystemConfigDefinition implements SystemConfigDefinition<String> {
    // TODO: Remove SystemConfigKey.PROVIDER__LISTINGPAGE__IMPRINTDEPARTMENTID and use the key directly
    public static final String KEY = SystemConfigKey.PROVIDER__LISTINGPAGE__IMPRINTDEPARTMENTID.getKey();

    private final VDepartmentShadowedRepository vDepartmentShadowedRepository;

    public ListingPageImprintSystemConfigDefinition(VDepartmentShadowedRepository vDepartmentShadowedRepository) {
        this.vDepartmentShadowedRepository = vDepartmentShadowedRepository;
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
                        vDepartmentShadowedRepository
                                .findAll()
                                .stream()
                                .map(dep -> SelectInputElementOption.of(dep.getId().toString(), dep.getName()))
                                .toList()
                )
                .setLabel(getLabel())
                .setHint(getDescription())
                .setId(getKey());
    }

    @Nonnull
    @Override
    public String getCategory() {
        return "Öffentliche Auflistung";
    }

    @Nonnull
    @Override
    public String getLabel() {
        return "Impressum";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Die für das Impressum zuständige Organisationseinheit.";
    }

    @Nonnull
    @Override
    public Boolean isPublicConfig() {
        return true;
    }

    @Nullable
    @Override
    public String parseValueFromDB(@Nonnull String value) throws ResponseException {
        // Optional references use an empty string to represent "not configured" in system_configs.
        if (value.isEmpty()) {
            return value;
        }

        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw ResponseException.internalServerError("Ungültiger Wert für " + getLabel());
        }
        return value;
    }
}
