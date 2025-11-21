package de.aivot.GoverBackend.theme.services;

import de.aivot.GoverBackend.asset.repositories.AssetRepository;
import de.aivot.GoverBackend.department.filters.OrganizationalUnitFilter;
import de.aivot.GoverBackend.department.repositories.OrganizationalUnitRepository;
import de.aivot.GoverBackend.form.filters.FormVersionFilter;
import de.aivot.GoverBackend.form.repositories.FormVersionRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.theme.entities.ThemeEntity;
import de.aivot.GoverBackend.theme.repositories.ThemeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

@Service
public class ThemeService implements EntityService<ThemeEntity, Integer> {
    private final ThemeRepository themeRepository;
    private final FormVersionRepository formVersionRepository;
    private final OrganizationalUnitRepository organizationalUnitRepository;
    private final AssetRepository assetRepository;

    @Autowired
    public ThemeService(ThemeRepository themeRepository,
                        FormVersionRepository formVersionRepository,
                        OrganizationalUnitRepository organizationalUnitRepository, AssetRepository assetRepository) {
        this.themeRepository = themeRepository;
        this.formVersionRepository = formVersionRepository;
        this.organizationalUnitRepository = organizationalUnitRepository;
        this.assetRepository = assetRepository;
    }

    @Nonnull
    @Override
    public ThemeEntity create(@Nonnull ThemeEntity entity) throws ResponseException {
        entity.setId(null);
        return themeRepository.save(entity);
    }

    @Override
    public void performDelete(@Nonnull ThemeEntity entity) throws ResponseException {
        var formSpec = FormVersionFilter
                .create()
                .setThemeId(entity.getId())
                .build();

        if (formVersionRepository.exists(formSpec)) {
            throw ResponseException.conflict("Das Farbschema wird noch von einem oder mehreren Formularen verwendet.");
        }

        var depSpec = OrganizationalUnitFilter
                .create()
                .setThemeId(entity.getId())
                .build();

        if (organizationalUnitRepository.exists(depSpec)) {
            throw ResponseException.conflict("Das Farbschema wird noch von einer oder mehreren Fachbereichen verwendet.");
        }

        themeRepository.delete(entity);
    }

    @Nonnull
    @Override
    public Page<ThemeEntity> performList(@Nonnull Pageable pageable, @Nullable Specification<ThemeEntity> specification, Filter<ThemeEntity> filter) {
        return themeRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public ThemeEntity performUpdate(@Nonnull Integer id, @Nonnull ThemeEntity entity, @Nonnull ThemeEntity existingEntity) throws ResponseException {
        existingEntity.setName(entity.getName());
        existingEntity.setMain(entity.getMain());
        existingEntity.setMainDark(entity.getMainDark());
        existingEntity.setAccent(entity.getAccent());
        existingEntity.setError(entity.getError());
        existingEntity.setWarning(entity.getWarning());
        existingEntity.setInfo(entity.getInfo());
        existingEntity.setSuccess(entity.getSuccess());

        var logoKey = entity.getLogoKey();
        if (logoKey != null) {
            var logoExists = assetRepository
                    .existsById(logoKey);
            if (logoExists) {
                existingEntity.setLogoKey(logoKey);
            } else {
                existingEntity.setLogoKey(null);
            }
        }

        var faviconKey = entity.getFaviconKey();
        if (faviconKey != null) {
            var faviconExists = assetRepository
                    .existsById(faviconKey);
            if (faviconExists) {
                existingEntity.setFaviconKey(faviconKey);
            } else {
                existingEntity.setFaviconKey(null);
            }
        }

        return themeRepository.save(existingEntity);
    }

    @Nonnull
    @Override
    public Optional<ThemeEntity> retrieve(@Nonnull Integer id) {
        return themeRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<ThemeEntity> retrieve(@Nonnull Specification<ThemeEntity> specification) {
        return themeRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return themeRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<ThemeEntity> specification) {
        return themeRepository.exists(specification);
    }
}
