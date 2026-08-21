package de.aivot.prosuna.backend.theme.services;

import de.aivot.prosuna.backend.asset.repositories.AssetRepository;
import de.aivot.prosuna.backend.department.filters.DepartmentFilter;
import de.aivot.prosuna.backend.department.repositories.DepartmentRepository;
import de.aivot.prosuna.backend.department.repositories.VDepartmentShadowedRepository;
import de.aivot.prosuna.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.lib.services.EntityService;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.system.services.SystemService;
import de.aivot.prosuna.backend.theme.entities.ThemeEntity;
import de.aivot.prosuna.backend.theme.repositories.ThemeRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Service
public class ThemeService implements EntityService<ThemeEntity, Integer> {
    private final ThemeRepository themeRepository;
    private final DepartmentRepository departmentRepository;
    private final AssetRepository assetRepository;
    private final VDepartmentShadowedRepository vDepartmentShadowedRepository;
    private final SystemService systemService;

    @Autowired
    public ThemeService(ThemeRepository themeRepository,
                        DepartmentRepository departmentRepository,
                        AssetRepository assetRepository,
                        VDepartmentShadowedRepository vDepartmentShadowedRepository,
                        SystemService systemService) {
        this.themeRepository = themeRepository;
        this.departmentRepository = departmentRepository;
        this.assetRepository = assetRepository;
        this.vDepartmentShadowedRepository = vDepartmentShadowedRepository;
        this.systemService = systemService;
    }

    @Nonnull
    @Override
    public ThemeEntity create(@Nonnull ThemeEntity entity) throws ResponseException {
        entity.setId(null);
        return themeRepository.save(entity);
    }

    @Override
    public void performDelete(@Nonnull ThemeEntity entity) throws ResponseException {
        var defaultTheme = systemService.retrieveDefaultTheme();
        if (defaultTheme.getId().equals(entity.getId())) {
            throw ResponseException.conflict("Das Standard-Erscheinungsbild der Prosuna-Instanz kann nicht gelöscht werden.");
        }

        var depSpec = DepartmentFilter
                .create()
                .setThemeId(entity.getId())
                .build();

        if (departmentRepository.exists(depSpec)) {
            throw ResponseException.conflict("Das Erscheinungsbild wird noch von einer oder mehreren Organisationseinheiten verwendet.");
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
        existingEntity.setPrimaryColor(entity.getPrimaryColor());
        existingEntity.setSecondaryColor(entity.getSecondaryColor());
        existingEntity.setPrimaryColorDark(entity.getPrimaryColorDark());
        existingEntity.setSecondaryColorDark(entity.getSecondaryColorDark());

        var logoKey = entity.getLogoKey();
        if (logoKey == null) {
            existingEntity.setLogoKey(null);
        } else {
            var logoExists = assetRepository
                    .existsById(logoKey);
            if (logoExists) {
                existingEntity.setLogoKey(logoKey);
            } else {
                existingEntity.setLogoKey(null);
            }
        }

        var logoKeyDark = entity.getLogoKeyDark();
        if (logoKeyDark == null) {
            existingEntity.setLogoKeyDark(null);
        } else if (assetRepository.existsById(logoKeyDark)) {
            existingEntity.setLogoKeyDark(logoKeyDark);
        } else {
            existingEntity.setLogoKeyDark(null);
        }

        var faviconKey = entity.getFaviconKey();
        if (faviconKey == null) {
            existingEntity.setFaviconKey(null);
        } else {
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


    @Nonnull
    public List<ThemeEntity> getFormThemesInOrderOfImportance(@Nonnull ProcessVersionEntity processVersion,
                                                              @Nonnull FormLayoutElement form) throws ResponseException {
        // TODO: Need to respect department of owning process
        var themes = new LinkedList<ThemeEntity>();

        if (processVersion.getThemeId() != null) {
            themeRepository
                    .findById(processVersion.getThemeId())
                    .ifPresent(themes::add);
        }

        Consumer<Integer> getDepartmentTheme = (departmentId) -> {
            if (departmentId == null) {
                return;
            }
            vDepartmentShadowedRepository
                    .findById(departmentId)
                    .ifPresent(department -> {
                        if (department.getThemeId() != null) {
                            themeRepository
                                    .findById(department.getThemeId())
                                    .ifPresent(themes::add);
                        }
                    });
        };

        getDepartmentTheme.accept(form.getResponsibleDepartmentId());
        getDepartmentTheme.accept(form.getManagingDepartmentId());

        themes.add(systemService.retrieveDefaultTheme());

        return themes;
    }
}
