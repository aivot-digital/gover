package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.models.entities.Theme;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThemeRepository extends JpaRepository<Theme, Integer> {
}
