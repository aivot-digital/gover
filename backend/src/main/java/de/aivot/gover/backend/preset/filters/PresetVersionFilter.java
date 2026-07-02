package de.aivot.gover.backend.preset.filters;

import de.aivot.gover.backend.preset.enums.PresetStatus;
import de.aivot.gover.backend.lib.models.Filter;
import de.aivot.gover.backend.preset.entities.PresetVersionEntity;
import de.aivot.gover.backend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import jakarta.annotation.Nonnull;
import java.util.UUID;

public class PresetVersionFilter implements Filter<PresetVersionEntity> {
    private UUID presetKey;
    private Integer version;
    private PresetStatus status;

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

    public PresetStatus getStatus() {
        return status;
    }

    public PresetVersionFilter setStatus(PresetStatus status) {
        this.status = status;
        return this;
    }
}
