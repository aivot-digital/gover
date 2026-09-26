package de.aivot.prosuna.backend.customLink.services;

import de.aivot.prosuna.backend.customLink.entities.CustomLink;
import de.aivot.prosuna.backend.customLink.enums.CustomLinkType;
import de.aivot.prosuna.backend.customLink.repositories.CustomLinkRepository;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.lib.services.EntityService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CustomLinkService implements EntityService<CustomLink, Integer> {
    private final CustomLinkRepository repository;

    public CustomLinkService(CustomLinkRepository repository) {
        this.repository = repository;
    }

    @Nonnull
    @Override
    public CustomLink create(@Nonnull CustomLink entity) {
        entity.setId(null).setPosition(repository.getMaximumPosition(entity.getType()) + 1);
        return repository.save(entity);
    }

    @Nonnull
    @Override
    public Page<CustomLink> performList(@Nonnull Pageable pageable,
                                        @Nullable Specification<CustomLink> specification,
                                        @Nullable Filter<CustomLink> filter) {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public CustomLink performUpdate(@Nonnull Integer id,
                                    @Nonnull CustomLink entity,
                                    @Nonnull CustomLink existingEntity) throws ResponseException {
        if (existingEntity.getType() != entity.getType()) {
            throw ResponseException.badRequest("Der Typ eines Custom-Links kann nicht nachträglich geändert werden.");
        }
        existingEntity
                .setLabel(entity.getLabel())
                .setDescription(entity.getDescription())
                .setUrl(entity.getUrl())
                .setIcon(entity.getIcon())
                .setEnabled(entity.getEnabled());
        return repository.save(existingEntity);
    }

    @Nonnull
    @Override
    public Optional<CustomLink> retrieve(@Nonnull Integer id) {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<CustomLink> retrieve(@Nonnull Specification<CustomLink> specification) {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<CustomLink> specification) {
        return repository.exists(specification);
    }

    @Override
    public void performDelete(@Nonnull CustomLink entity) {
        repository.delete(entity);
    }

    @Transactional
    public List<CustomLink> reorder(@Nonnull CustomLinkType type,
                                    @Nonnull List<Integer> ids) throws ResponseException {
        var idSet = new HashSet<>(ids);
        if (idSet.size() != ids.size()) {
            throw ResponseException.badRequest("Die Reihenfolge enthält doppelte Custom-Links.");
        }

        var links = repository.findAllByType(type);
        // Require a full permutation within the type so stale clients cannot omit existing links.
        if (links.size() != ids.size() || links.stream().anyMatch(link -> !idSet.contains(link.getId()))) {
            throw ResponseException.badRequest("Die Reihenfolge muss alle Custom-Links des gewählten Typs enthalten.");
        }

        var linksById = links.stream().collect(Collectors.toMap(CustomLink::getId, link -> link));
        for (var position = 0; position < ids.size(); position++) {
            linksById.get(ids.get(position)).setPosition(position);
        }
        var orderedLinks = ids.stream().map(linksById::get).toList();
        return repository.saveAll(orderedLinks);
    }
}
