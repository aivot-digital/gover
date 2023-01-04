package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.models.Preset;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "presets", path = "presets")
public interface PresetRepository extends PagingAndSortingRepository<Preset, Long> {
}