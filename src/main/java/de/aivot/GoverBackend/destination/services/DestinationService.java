package de.aivot.GoverBackend.destination.services;

import de.aivot.GoverBackend.destination.entities.Destination;
import de.aivot.GoverBackend.destination.repositories.DestinationRepository;
import de.aivot.GoverBackend.form.filters.FormFilter;
import de.aivot.GoverBackend.form.repositories.FormRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
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
public class DestinationService implements EntityService<Destination, Integer> {
    private final DestinationRepository destinationRepository;
    private final FormRepository formRepository;

    @Autowired
    public DestinationService(
            DestinationRepository destinationRepository,
            FormRepository formRepository
    ) {
        this.destinationRepository = destinationRepository;
        this.formRepository = formRepository;
    }

    @Nonnull
    @Override
    public Destination create(@Nonnull Destination entity) throws ResponseException {
        entity.setId(null);
        return destinationRepository.save(entity);
    }

    @Nonnull
    @Override
    public Page<Destination> performList(@Nonnull Pageable pageable, @Nullable Specification<Destination> specification, Filter<Destination> filter) {
        return destinationRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Destination performUpdate(@Nonnull Integer id, @Nonnull Destination entity, @Nonnull Destination existingEntity) throws ResponseException {
        existingEntity.setType(entity.getType());
        existingEntity.setName(entity.getName());

        existingEntity.setApiAddress(entity.getApiAddress());
        existingEntity.setAuthorizationHeader(entity.getAuthorizationHeader());

        existingEntity.setMailTo(entity.getMailTo());
        existingEntity.setMailCC(entity.getMailCC());
        existingEntity.setMailBCC(entity.getMailBCC());

        existingEntity.setMaxAttachmentMegaBytes(entity.getMaxAttachmentMegaBytes());

        return destinationRepository.save(existingEntity);
    }

    @Nonnull
    @Override
    public Optional<Destination> retrieve(@Nonnull Integer id) {
        return destinationRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<Destination> retrieve(@Nonnull Specification<Destination> specification) {
        return destinationRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return destinationRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<Destination> specification) {
        return destinationRepository.exists(specification);
    }

    @Override
    public void performDelete(@Nonnull Destination entity) throws ResponseException {
        var spec = new FormFilter()
                .setDestinationId(entity.getId())
                .build();

        if (formRepository.exists(spec)) {
            throw new ResponseException(HttpStatus.CONFLICT, "Die Schnittstelle wird noch von einem oder mehreren Formularen verwendet.");
        }

        destinationRepository.delete(entity);
    }
}
