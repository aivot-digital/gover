package de.aivot.GoverBackend.theme.repositories;

import de.aivot.GoverBackend.theme.entities.ThemeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ThemeRepository extends JpaRepository<ThemeEntity, Integer>, JpaSpecificationExecutor<ThemeEntity> {
    @Query(
            value = "SELECT *, similarity(name, :search) as sml FROM themes ORDER BY sml DESC",
            nativeQuery = true
    )
    Page<ThemeEntity> search(@Param("search") String search, Pageable pageable);
}
