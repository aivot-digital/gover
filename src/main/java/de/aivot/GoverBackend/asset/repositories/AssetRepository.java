package de.aivot.GoverBackend.asset.repositories;

import de.aivot.GoverBackend.asset.entities.AssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AssetRepository extends JpaRepository<AssetEntity, String>, JpaSpecificationExecutor<AssetEntity> {
}
