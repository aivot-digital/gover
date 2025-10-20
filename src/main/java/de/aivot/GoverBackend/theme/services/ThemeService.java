package de.aivot.GoverBackend.theme.services;

import de.aivot.GoverBackend.form.filters.FormVersionFilter;
import de.aivot.GoverBackend.form.repositories.FormRepository;
import de.aivot.GoverBackend.form.repositories.FormVersionRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.theme.entities.Theme;
import de.aivot.GoverBackend.theme.repositories.ThemeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

@Service
public class ThemeService implements EntityService<Theme, Integer> {
    private final ThemeRepository themeRepository;
    private final FormRepository formRepository;
    private final FormVersionRepository formVersionRepository;

    @Autowired
    public ThemeService(
            ThemeRepository themeRepository,
            FormRepository formRepository,
            FormVersionRepository formVersionRepository) {
        this.themeRepository = themeRepository;
        this.formRepository = formRepository;
        this.formVersionRepository = formVersionRepository;
    }

    @Nonnull
    @Override
    public Theme create(@Nonnull Theme entity) throws ResponseException {
        entity.setId(null);
        return themeRepository.save(entity);
    }

    @Override
    public void performDelete(@Nonnull Theme entity) throws ResponseException {
        var formSpec = FormVersionFilter
                .create()
                .setThemeId(entity.getId())
                .build();

        if (formVersionRepository.exists(formSpec)) {
            throw new ResponseException(HttpStatus.CONFLICT, "Das Farbschema wird noch von einem oder mehreren Formularen verwendet.");
        }

        themeRepository.delete(entity);
    }

    @Nonnull
    @Override
    public Page<Theme> performList(@Nonnull Pageable pageable, @Nullable Specification<Theme> specification, Filter<Theme> filter) {
        return themeRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Theme performUpdate(@Nonnull Integer id, @Nonnull Theme entity, @Nonnull Theme existingEntity) throws ResponseException {
        existingEntity.setName(entity.getName());
        existingEntity.setMain(entity.getMain());
        existingEntity.setMainDark(entity.getMainDark());
        existingEntity.setAccent(entity.getAccent());
        existingEntity.setError(entity.getError());
        existingEntity.setWarning(entity.getWarning());
        existingEntity.setInfo(entity.getInfo());
        existingEntity.setSuccess(entity.getSuccess());

        return themeRepository.save(existingEntity);
    }

    @Nonnull
    @Override
    public Optional<Theme> retrieve(@Nonnull Integer id) {
        return themeRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<Theme> retrieve(@Nonnull Specification<Theme> specification) {
        return themeRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return themeRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<Theme> specification) {
        return themeRepository.exists(specification);
    }
}
