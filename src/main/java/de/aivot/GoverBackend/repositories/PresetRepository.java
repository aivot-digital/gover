package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.models.entities.Preset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PresetRepository extends JpaRepository<Preset, Integer> {
}
