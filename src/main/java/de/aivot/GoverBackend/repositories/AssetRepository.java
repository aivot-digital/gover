package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.models.entities.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AssetRepository extends JpaRepository<Asset, String> {
    Collection<Asset> findAllByContentTypeEqualsIgnoreCase(String contentType);
    Collection<Asset> findAllByContentTypeStartingWith(String contentType);
}
