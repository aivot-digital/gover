package de.aivot.GoverBackend.asset.services;

import de.aivot.GoverBackend.asset.entities.AssetEntity;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
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
public class AssetService implements EntityService<AssetEntity, UUID> {

    private final GoverConfig goverConfig;

    public AssetService(GoverConfig goverConfig) {
        this.goverConfig = goverConfig;
    }

    @Nonnull
    @Override
    public AssetEntity create(@Nonnull AssetEntity entity) throws ResponseException {
        return null;
    }

    @Override
    public void performDelete(@Nonnull AssetEntity entity) throws ResponseException {

    }

    @Nullable
    @Override
    public Page<AssetEntity> performList(@Nonnull Pageable pageable,
                                         @Nullable Specification<AssetEntity> specification,
                                         @Nullable Filter<AssetEntity> filter) throws ResponseException {
        return null;
    }

    @Nonnull
    @Override
    public AssetEntity performUpdate(@Nonnull UUID id,
                                     @Nonnull AssetEntity entity,
                                     @Nonnull AssetEntity existingEntity) throws ResponseException {
        return null;
    }

    @Nonnull
    @Override
    public Optional<AssetEntity> retrieve(@Nonnull UUID id) throws ResponseException {
        return Optional.empty();
    }

    @Nonnull
    @Override
    public Optional<AssetEntity> retrieve(@Nonnull Specification<AssetEntity> specification) throws ResponseException {
        return Optional.empty();
    }

    @Override
    public boolean exists(@Nonnull UUID id) {
        return false;
    }

    @Override
    public boolean exists(@Nonnull Specification<AssetEntity> specification) {
        return false;
    }

    public String createUrl(AssetEntity asset) {
        return createUrl(asset.getKey());
    }

    public String createUrl(UUID key) {
        return goverConfig.createUrl("/api/public/assets/" + key.toString() + "/");
    }
}
