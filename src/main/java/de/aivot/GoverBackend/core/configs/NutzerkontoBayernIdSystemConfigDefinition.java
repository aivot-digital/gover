package de.aivot.GoverBackend.core.configs;

import de.aivot.GoverBackend.config.entities.SystemConfigEntity;
import de.aivot.GoverBackend.config.enums.ConfigType;
import de.aivot.GoverBackend.config.models.SystemConfigDefinition;
import de.aivot.GoverBackend.data.SystemConfigKey;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.entities.Form;
import de.aivot.GoverBackend.form.repositories.FormRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.List;

@Component
public class NutzerkontoBayernIdSystemConfigDefinition implements SystemConfigDefinition {
    // TODO: Remove SystemConfigKey.NUTZERKONTEN__BAYERN_ID and use the key directly
    public static final String KEY = SystemConfigKey.NUTZERKONTEN__BAYERN_ID.getKey();

    private final FormRepository formRepository;

    @Autowired
    public NutzerkontoBayernIdSystemConfigDefinition(FormRepository formRepository) {
        this.formRepository = formRepository;
    }

    @NotNull
    @Override
    public String getKey() {
        return KEY;
    }

    @NotNull
    @Override
    public ConfigType getType() {
        return ConfigType.FLAG;
    }

    @NotNull
    @Override
    public String getCategory() {
        return "Nutzerkonten";
    }

    @NotNull
    @Override
    public String getLabel() {
        return "BayernID aktiviert";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Aktiviert die BayernID als verfügbares Nutzerkonto für Ihre Anträge.";
    }

    @NotNull
    @Override
    public Boolean isPublicConfig() {
        return false;
    }

    @Override
    public void validate(@Nonnull SystemConfigEntity entity) throws ResponseException {
        // Check if the flag is being disabled while published forms are using it
        var val = entity
                .getValueAsBoolean()
                .orElse(false);
        if (!val) {
            if (formRepository.existsByStatusAndBayernIdEnabledIsTrue(FormStatus.Published)) {
                throw ResponseException.conflict("Das Nutzerkonto BayernID kann nicht deaktiviert werden, solange es in veröffentlichten Formulare in Verwendung ist.");
            }
        }
    }

    @Override
    public void afterChange(SystemConfigEntity entity) {
        var val = entity
                .getValueAsBoolean()
                .orElse(false);

        if (!val) {
            formRepository
                    .findAllByStatusNotIn(List.of(FormStatus.Published, FormStatus.Revoked))
                    .stream()
                    .filter(Form::getBayernIdEnabled)
                    .forEach(form -> {
                        form.setBayernIdEnabled(false);
                        form.setBayernIdLevel(null);
                        form.getRoot().setTestProtocolSet(null);
                        formRepository.save(form);
                    });
        }
    }
}
