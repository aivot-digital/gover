package de.aivot.gover.backend.core.configs;

import de.aivot.gover.backend.config.models.SystemConfigDefinition;
import de.aivot.gover.backend.department.repositories.VDepartmentShadowedRepository;
import de.aivot.gover.backend.elements.models.elements.BaseElement;
import de.aivot.gover.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

@Component
public class ListingPageAccesibilitySystemConfigDefinition implements SystemConfigDefinition<String> {
    public static final String KEY = "ProviderListingPageAccessibilityDepartmentId";

    private final VDepartmentShadowedRepository vDepartmentShadowedRepository;

    public ListingPageAccesibilitySystemConfigDefinition(VDepartmentShadowedRepository vDepartmentShadowedRepository) {
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
        return "Erklärung der Barrierefreiheit";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Die für die Barrierefreiheit zuständige Organisationseinheit.";
    }

    @Nonnull
    @Override
    public Boolean isPublicConfig() {
        return true;
    }

    @Nullable
    @Override
    public String parseValueFromDB(@Nonnull String value) throws ResponseException {
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw ResponseException.internalServerError("Ungültiger Wert für " + getLabel());
        }
        return value;
    }
}
