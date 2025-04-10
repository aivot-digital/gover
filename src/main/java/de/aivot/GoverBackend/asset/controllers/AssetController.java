package de.aivot.GoverBackend.asset.controllers;

import de.aivot.GoverBackend.asset.dtos.AssetRequestDTO;
import de.aivot.GoverBackend.asset.dtos.AssetResponseDTO;
import de.aivot.GoverBackend.asset.entities.AssetEntity;
import de.aivot.GoverBackend.asset.filters.AssetFilter;
import de.aivot.GoverBackend.asset.repositories.AssetRepository;
import de.aivot.GoverBackend.asset.services.AssetService;
import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.services.AVService;
import de.aivot.GoverBackend.services.storages.AssetStorageService;
import de.aivot.GoverBackend.user.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/assets/")
public class AssetController {
    private final ScopedAuditService auditService;

    private final AssetService assetService;
    private final AssetStorageService assetStorageService;
    private final AssetRepository assetRepository;
    private final AVService avService;

    @Autowired
    public AssetController(
            AuditService auditService,
            AssetService assetService,
            AssetStorageService assetStorageService,
            AssetRepository assetRepository,
            AVService avService
    ) {
        this.auditService = auditService.createScopedAuditService(AssetController.class);

        this.assetService = assetService;
        this.assetStorageService = assetStorageService;
        this.assetRepository = assetRepository;
        this.avService = avService;
    }

    @GetMapping("")
    public Page<AssetResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid AssetFilter filter
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return assetService
                .list(pageable, filter)
                .map(AssetResponseDTO::fromEntity);
    }

    @PostMapping("")
    public AssetResponseDTO create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nullable @RequestParam("file") MultipartFile file
    ) throws ResponseException {
        // TODO: Refactor with new AssetService.create method
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (file == null) {
            throw ResponseException.badRequest("Es wurde keine Datei hochgeladen.");
        }

        boolean isClean;
        try {
            isClean = avService.testFile(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (!isClean) {
            throw ResponseException.badRequest("Die hochgeladene Datei enthält Schadsoftware.");
        }

        String filename = file.getOriginalFilename();

        if (filename == null) {
            throw ResponseException.badRequest("Der Dateiname konnte nicht ermittelt werden.");
        }

        var asset = new AssetEntity();
        asset.setKey(UUID.randomUUID().toString());
        asset.setFilename(filename);
        asset.setCreated(LocalDateTime.now());
        asset.setUploaderId(user.getId());
        asset.setContentType(file.getContentType());
        asset.setPrivate(true);

        try {
            assetStorageService.saveAsset(asset, file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var createdAsset = assetRepository.save(asset);

        auditService.logAction(user, AuditAction.Create, AssetEntity.class, Map.of(
                "key", createdAsset.getKey(),
                "filename", createdAsset.getFilename()
        ));

        return AssetResponseDTO.fromEntity(createdAsset);
    }

    @GetMapping("{assetId}/")
    public AssetResponseDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String assetId
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return assetService
                .retrieve(assetId)
                .map(AssetResponseDTO::fromEntity)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{assetId}/")
    public AssetResponseDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String assetId,
            @Nonnull @RequestBody @Valid AssetRequestDTO requestDTO
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var updatedAsset = assetService
                .update(assetId, requestDTO.toEntity());

        auditService.logAction(user, AuditAction.Update, AssetEntity.class, Map.of(
                "key", updatedAsset.getKey(),
                "filename", updatedAsset.getFilename(),
                "isPrivate", updatedAsset.getPrivate()
        ));

        return AssetResponseDTO
                .fromEntity(updatedAsset);
    }

    @DeleteMapping("{assetId}/")
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String assetId
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var asset = assetRepository
                .findById(assetId)
                .orElseThrow(ResponseException::notFound);

        auditService.logAction(user, AuditAction.Delete, AssetEntity.class, Map.of(
                "key", asset.getKey(),
                "filename", asset.getFilename()
        ));

        assetService.delete(assetId);

        assetStorageService.deleteAsset(asset);
    }
}
