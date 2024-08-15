package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.exceptions.BadRequestException;
import de.aivot.GoverBackend.exceptions.ConflictException;
import de.aivot.GoverBackend.exceptions.NotFoundException;
import de.aivot.GoverBackend.models.auth.KeyCloakDetailsUser;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.models.config.StorageConfig;
import de.aivot.GoverBackend.models.entities.Asset;
import de.aivot.GoverBackend.repositories.AssetRepository;
import de.aivot.GoverBackend.services.AVService;
import de.aivot.GoverBackend.services.storages.AssetStorageService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;

@RestController
public class AssetController {
    private final AssetStorageService assetStorageService;
    private final AssetRepository assetRepository;
    private final AVService avService;
    private final GoverConfig goverConfig;
    private final StorageConfig storageConfig;

    @Autowired
    public AssetController(
            AssetStorageService assetStorageService,
            AssetRepository assetRepository,
            AVService avService,
            GoverConfig goverConfig,
            StorageConfig storageConfig
    ) {
        this.assetStorageService = assetStorageService;
        this.assetRepository = assetRepository;
        this.avService = avService;
        this.goverConfig = goverConfig;
        this.storageConfig = storageConfig;
    }

    /**
     * Retrieve a single asset as an anonymous user.
     *
     * @param assetId The id of the asset to retrieve.
     */
    @GetMapping("/api/public/assets/{assetId}")
    public void retrievePublicById(
            @PathVariable String assetId,
            HttpServletResponse response
    ) throws IOException {
        var asset = getAssetById(assetId);

        var urlEncodedFilename = URLEncoder
                .encode(asset.getFilename(), StandardCharsets.UTF_8);

        var targetUrl = goverConfig.createUrl(
                "/api/public/assets/" + assetId + "/" + urlEncodedFilename
        );

        response.sendRedirect(targetUrl);
    }

    @GetMapping("/api/public/assets/{assetId}/{filename}")
    public void retrievePublicFile(
            @PathVariable String assetId,
            @PathVariable String filename,
            HttpServletResponse response
    ) throws IOException {
        var asset = getAssetById(assetId);

        if (storageConfig.localStorageEnabled()) {
            var urlEncodedFilename = URLEncoder
                    .encode(asset.getFilename(), StandardCharsets.UTF_8);
            var targetUrl = goverConfig.createUrl(
                    "/api/public/assets/local/" + assetId + "/" + urlEncodedFilename
            );
            response.sendRedirect(targetUrl);
        } else {
            var storageUrl = assetStorageService
                    .getAssetUrl(asset);
            response.sendRedirect(storageUrl);
        }
    }

    @GetMapping("/api/public/assets/local/{assetId}/{filename}")
    public ResponseEntity<Resource> retrievePublicLocalFile(
            @PathVariable String assetId,
            @PathVariable String filename
    ) {
        // Check if the assetId is a valid UUID.
        var asset = getAssetById(assetId);

        var assetBytes = assetStorageService
                .getAssetData(asset);

        Resource resource = new ByteArrayResource(assetBytes);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(asset.getContentType());
        } catch (InvalidMediaTypeException e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        if (resource.exists() && resource.isReadable()) {
            return ResponseEntity
                    .ok()
                    .contentType(mediaType)
                    .body(resource);
        }

        throw new NotFoundException();
    }

    /**
     * List all assets as an authenticated user.
     *
     * @return The list of the assets.
     */
    @GetMapping("/api/assets")
    public Collection<Asset> list(
            @RequestParam(required = false) String mimetype
    ) {
        if (mimetype != null) {
            if (mimetype.endsWith("/*")) {
                return assetRepository
                        .findAllByContentTypeStartingWith(mimetype.substring(0, mimetype.length() - 2));
            } else {
                return assetRepository
                        .findAllByContentTypeEqualsIgnoreCase(mimetype);
            }
        }

        return assetRepository
                .findAll();
    }

    /**
     * Upload a new asset as an authenticated user.
     *
     * @param file The new asset
     * @return The created asset object.
     */
    @PostMapping("/api/assets")
    public Asset create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file
    ) {
        boolean isClean;
        try {
            isClean = avService.testFile(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (!isClean) {
            throw new ConflictException();
        }

        String filename = file.getOriginalFilename();

        if (filename == null) {
            throw new BadRequestException();
        }

        var asset = new Asset();
        asset.setKey(UUID.randomUUID().toString());
        asset.setFilename(filename);
        asset.setCreated(LocalDateTime.now());
        asset.setUploaderId(KeyCloakDetailsUser.fromJwt(jwt).getId());
        asset.setContentType(file.getContentType());

        try {
            assetStorageService.saveAsset(asset, file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return assetRepository.save(asset);
    }

    /**
     * Retrieve a single asset as an authenticated user.
     *
     * @return The retrieved asset.
     */
    @GetMapping("/api/assets/{assetId}")
    public Asset retrieve(
            @PathVariable String assetId
    ) {
        return assetRepository
                .findById(assetId)
                .orElseThrow(NotFoundException::new);
    }

    /**
     * Update an asset as an authenticated user.
     *
     * @return The updated asset.
     */
    @PutMapping("/api/assets/{assetId}")
    public Asset update(
            @PathVariable String assetId,
            @RequestBody @Valid Asset updatedAsset
    ) {
        var existingAsset = assetRepository
                .findById(assetId)
                .orElseThrow(NotFoundException::new);

        existingAsset.setFilename(updatedAsset.getFilename());

        return assetRepository.save(existingAsset);
    }

    /**
     * Delete an asset as an authenticated user.
     *
     * @param assetId The filename of the asset.
     */
    @DeleteMapping("/api/assets/{assetId}")
    public void destroy(
            @PathVariable String assetId
    ) {
        var asset = assetRepository
                .findById(assetId)
                .orElseThrow(NotFoundException::new);

        assetStorageService.deleteAsset(asset);

        assetRepository.delete(asset);
    }

    private Asset getAssetById(String assetId) {
        try {
            UUID.fromString(assetId);
        } catch (Exception e) {
            throw new NotFoundException();
        }

        return assetRepository
                .findById(assetId)
                .orElseThrow(NotFoundException::new);
    }
}
