package de.aivot.GoverBackend.core.configs;

import de.aivot.GoverBackend.config.models.SystemConfigDefinition;
import de.aivot.GoverBackend.department.repositories.VDepartmentShadowedRepository;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

@Component
public class ListingPagePrivacySystemConfigDefinition implements SystemConfigDefinition<String> {
    public static final String KEY = "ProviderListingPagePrivacyDepartmentId";
    private final VDepartmentShadowedRepository vDepartmentShadowedRepository;

    public ListingPagePrivacySystemConfigDefinition(VDepartmentShadowedRepository vDepartmentShadowedRepository) {
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
        return "Datenschutzerklärung";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Die für den Datenschutz zuständige Organisationseinheit.";
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
