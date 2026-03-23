package de.aivot.GoverBackend.preset.filters;

import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.preset.entities.PresetVersionEntity;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import jakarta.annotation.Nonnull;
import java.util.UUID;

public class PresetVersionFilter implements Filter<PresetVersionEntity> {
    private UUID presetKey;
    private Integer version;
    private FormStatus status;

    public static PresetVersionFilter create() {
        return new PresetVersionFilter();
    }

    @Nonnull
    @Override
    public Specification<PresetVersionEntity> build() {
        return SpecificationBuilder
                .create(PresetVersionEntity.class)
                .withEquals("presetKey", presetKey)
                .withEquals("version", version)
                .withEquals("status", status)
                .build();
    }

    public UUID getPresetKey() {
        return presetKey;
    }

    public PresetVersionFilter setPresetKey(UUID presetKey) {
        this.presetKey = presetKey;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public PresetVersionFilter setVersion(Integer version) {
        this.version = version;
        return this;
    }

    public FormStatus getStatus() {
        return status;
    }

    public PresetVersionFilter setStatus(FormStatus status) {
        this.status = status;
        return this;
    }
}
