package de.aivot.prosuna.backend.theme.services;

import de.aivot.prosuna.backend.asset.repositories.VStorageIndexItemWithAssetRepository;
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
import java.util.UUID;

@Service
public class ThemeService implements EntityService<ThemeEntity, Integer> {
    private final ThemeRepository themeRepository;
    private final DepartmentRepository departmentRepository;
    private final VStorageIndexItemWithAssetRepository storageItemWithAssetRepository;
    private final VDepartmentShadowedRepository vDepartmentShadowedRepository;
    private final SystemService systemService;

    @Autowired
    public ThemeService(ThemeRepository themeRepository,
                        DepartmentRepository departmentRepository,
                        VStorageIndexItemWithAssetRepository storageItemWithAssetRepository,
                        VDepartmentShadowedRepository vDepartmentShadowedRepository,
                        SystemService systemService) {
        this.themeRepository = themeRepository;
        this.departmentRepository = departmentRepository;
        this.storageItemWithAssetRepository = storageItemWithAssetRepository;
        this.vDepartmentShadowedRepository = vDepartmentShadowedRepository;
        this.systemService = systemService;
    }

    @Nonnull
    @Override
    public ThemeEntity create(@Nonnull ThemeEntity entity) throws ResponseException {
        entity.setId(null);
        validateMediaAssets(entity);
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
        validateMediaAssets(entity);

        existingEntity.setName(entity.getName());
        existingEntity.setPrimaryColor(entity.getPrimaryColor());
        existingEntity.setSecondaryColor(entity.getSecondaryColor());
        existingEntity.setPrimaryColorDark(entity.getPrimaryColorDark());
        existingEntity.setSecondaryColorDark(entity.getSecondaryColorDark());

        existingEntity.setLogoKey(entity.getLogoKey());
        existingEntity.setLogoKeyDark(entity.getLogoKeyDark());
        existingEntity.setFaviconKey(entity.getFaviconKey());

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
                                                              @Nonnull FormLayoutElement form,
                                                              @Nullable Integer processDepartmentId) {
        var themes = new LinkedList<ThemeEntity>();

        addTheme(themes, processVersion.getThemeId());
        addDepartmentTheme(themes, form.getResponsibleDepartmentId());
        addDepartmentTheme(themes, form.getManagingDepartmentId());
        addDepartmentTheme(themes, processDepartmentId);

        themes.add(systemService.retrieveDefaultTheme());

        return themes;
    }

    @Nonnull
    public ThemeEntity resolveFormTheme(@Nonnull ProcessVersionEntity processVersion,
                                        @Nonnull FormLayoutElement form,
                                        @Nullable Integer processDepartmentId) {
        return resolveThemeChain(getFormThemesInOrderOfImportance(processVersion, form, processDepartmentId));
    }

    @Nonnull
    public ThemeEntity resolveProcessTheme(@Nonnull ProcessVersionEntity processVersion,
                                           @Nullable Integer processDepartmentId) {
        var themes = new LinkedList<ThemeEntity>();
        addTheme(themes, processVersion.getThemeId());
        addDepartmentTheme(themes, processDepartmentId);
        themes.add(systemService.retrieveDefaultTheme());
        return resolveThemeChain(themes);
    }

    @Nonnull
    public ThemeEntity resolveDepartmentTheme(@Nullable Integer departmentId) {
        var themes = new LinkedList<ThemeEntity>();
        addDepartmentTheme(themes, departmentId);
        themes.add(systemService.retrieveDefaultTheme());
        return resolveThemeChain(themes);
    }

    @Nonnull
    public ThemeEntity resolveThemeWithSystemFallback(@Nonnull ThemeEntity theme) {
        return resolveThemeChain(List.of(theme, systemService.retrieveDefaultTheme()));
    }

    @Nonnull
    public ThemeEntity resolveThemeChain(@Nonnull List<ThemeEntity> themes) {
        if (themes.isEmpty()) {
            throw new IllegalArgumentException("A theme chain must contain at least one theme.");
        }

        var appearanceTheme = themes.getFirst();
        return new ThemeEntity(
                appearanceTheme.getId(),
                appearanceTheme.getName(),
                appearanceTheme.getPrimaryColor(),
                appearanceTheme.getSecondaryColor(),
                appearanceTheme.getPrimaryColorDark(),
                appearanceTheme.getSecondaryColorDark(),
                resolveLightLogoKey(themes),
                resolveDarkLogoKey(themes),
                resolveFaviconKey(themes)
        );
    }

    private void addTheme(@Nonnull List<ThemeEntity> themes, @Nullable Integer themeId) {
        if (themeId == null) {
            return;
        }
        themeRepository.findById(themeId).ifPresent(themes::add);
    }

    private void addDepartmentTheme(@Nonnull List<ThemeEntity> themes, @Nullable Integer departmentId) {
        if (departmentId == null) {
            return;
        }
        vDepartmentShadowedRepository
                .findById(departmentId)
                .map(department -> department.getThemeId())
                .ifPresent(themeId -> addTheme(themes, themeId));
    }

    @Nullable
    private static UUID resolveLightLogoKey(@Nonnull List<ThemeEntity> themes) {
        return themes.stream()
                .map(ThemeEntity::getLogoKey)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Nullable
    private static UUID resolveDarkLogoKey(@Nonnull List<ThemeEntity> themes) {
        for (var theme : themes) {
            if (theme.getLogoKeyDark() != null) {
                return theme.getLogoKeyDark();
            }
            if (theme.getLogoKey() != null) {
                return theme.getLogoKey();
            }
        }
        return null;
    }

    @Nullable
    private static UUID resolveFaviconKey(@Nonnull List<ThemeEntity> themes) {
        return themes.stream()
                .map(ThemeEntity::getFaviconKey)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private void validateMediaAssets(@Nonnull ThemeEntity theme) throws ResponseException {
        validateImageAsset(theme.getLogoKey(), "Das Logo für helle Hintergründe");
        validateImageAsset(theme.getLogoKeyDark(), "Das Logo für dunkle Hintergründe");
        validateImageAsset(theme.getFaviconKey(), "Das Favicon");
    }

    private void validateImageAsset(@Nullable UUID assetKey, @Nonnull String label) throws ResponseException {
        if (assetKey == null) {
            return;
        }

        var asset = storageItemWithAssetRepository
                .findByAssetKey(assetKey)
                .orElseThrow(() -> ResponseException.badRequest("%s wurde nicht gefunden.", label));

        if (Boolean.TRUE.equals(asset.getDirectory()) || Boolean.TRUE.equals(asset.getMissing())) {
            throw ResponseException.badRequest("%s verweist nicht auf eine verfügbare Datei.", label);
        }
        if (!Boolean.FALSE.equals(asset.getAssetIsPrivate())) {
            throw ResponseException.badRequest("%s muss öffentlich zugänglich sein.", label);
        }
        if (asset.getMimeType() == null || !asset.getMimeType().toLowerCase().startsWith("image/")) {
            throw ResponseException.badRequest("%s muss eine Bilddatei sein.", label);
        }
    }
}
