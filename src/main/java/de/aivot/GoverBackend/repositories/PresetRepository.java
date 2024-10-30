package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.models.entities.Preset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface PresetRepository extends JpaRepository<Preset, String> {
    Collection<Preset> findAllByCurrentPublishedVersionIsNotNull();

    boolean existsByTitle(String title);
}
