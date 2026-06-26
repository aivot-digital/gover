package de.aivot.GoverBackend.asset.services;

import de.aivot.GoverBackend.asset.entities.AssetEntity;
import de.aivot.GoverBackend.asset.repositories.AssetRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.ReadEntityService;
import de.aivot.GoverBackend.models.config.GoverConfig;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class AssetService implements ReadEntityService<AssetEntity, UUID> {

    private final GoverConfig goverConfig;
    private final AssetRepository assetRepository;

    public AssetService(GoverConfig goverConfig, AssetRepository assetRepository) {
        this.goverConfig = goverConfig;
        this.assetRepository = assetRepository;
    }


    @Nullable
    @Override
    public Page<AssetEntity> performList(@Nonnull Pageable pageable,
                                         @Nullable Specification<AssetEntity> specification,
                                         @Nullable Filter<AssetEntity> filter) throws ResponseException {
        return assetRepository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<AssetEntity> retrieve(@Nonnull UUID id) throws ResponseException {
        return assetRepository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<AssetEntity> retrieve(@Nonnull Specification<AssetEntity> specification) throws ResponseException {
        return assetRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull UUID id) {
        return assetRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<AssetEntity> specification) {
        return assetRepository.exists(specification);
    }

    public String createUrl(AssetEntity asset) {
        return createUrl(asset.getKey());
    }

    public String createUrl(UUID key) {
        return goverConfig.createUrl("/api/public/assets/" + key.toString() + "/");
    }
}
