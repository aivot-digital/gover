package de.aivot.prosuna.backend.customLink.repositories;

import de.aivot.prosuna.backend.customLink.entities.CustomLink;
import de.aivot.prosuna.backend.customLink.enums.CustomLinkType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomLinkRepository extends JpaRepository<CustomLink, Integer>, JpaSpecificationExecutor<CustomLink> {
    @Query("SELECT COALESCE(MAX(link.position), -1) FROM CustomLink link WHERE link.type = :type")
    int getMaximumPosition(@Param("type") CustomLinkType type);

    List<CustomLink> findAllByType(CustomLinkType type);
}
