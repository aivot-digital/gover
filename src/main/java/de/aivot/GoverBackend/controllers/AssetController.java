package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.exceptions.BadRequestException;
import de.aivot.GoverBackend.exceptions.ConflictException;
import de.aivot.GoverBackend.exceptions.NotAcceptableException;
import de.aivot.GoverBackend.exceptions.NotFoundException;
import de.aivot.GoverBackend.models.auth.KeyCloakDetailsUser;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.models.entities.Asset;
import de.aivot.GoverBackend.repositories.AssetRepository;
import de.aivot.GoverBackend.services.AVService;
import de.aivot.GoverBackend.services.AssetStorageService;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
public class AssetController {
    private final AssetStorageService assetStorageService;
    private final AssetRepository assetRepository;
    private final AVService avService;
    private final GoverConfig goverConfig;

    @Autowired
    public AssetController(
            AssetStorageService assetStorageService,
            AssetRepository assetRepository,
            AVService avService, GoverConfig goverConfig
    ) {
        this.assetStorageService = assetStorageService;
        this.assetRepository = assetRepository;
        this.avService = avService;
        this.goverConfig = goverConfig;
    }

    @GetMapping("/api/public/assets/{assetId}/{filename}")
    public ResponseEntity<Resource> retrieveFile(
            @PathVariable String assetId,
            @PathVariable String filename
    ) {
        // Check if the assetId is a valid UUID.
        try {
            UUID.fromString(assetId);
        } catch (Exception e) {
            throw new NotFoundException();
        }

        var asset = assetRepository
                .findById(assetId)
                .orElseThrow(NotFoundException::new);

        if (!asset.getFilename().equalsIgnoreCase(filename)) {
            throw new NotFoundException();
        }

        var path = assetStorageService
                .getAssetPath(asset.getKey())
                .orElseThrow(NotFoundException::new);

        Resource resource;
        try {
            resource = new UrlResource(path.toUri());
        } catch (MalformedURLException e) {
            throw new NotFoundException();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(asset.getContentType());
        } catch (InvalidMediaTypeException e) {
            try {
                mediaType = MediaType.parseMediaType(Files.probeContentType(path));
            } catch (IOException | InvalidMediaTypeException inE) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }
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
     * Retrieve a single asset as an anonymous user.
     *
     * @param assetId The id of the asset to retrieve.
     * @return The asset.
     */
    @GetMapping("/api/public/assets/{assetId}")
    public void retrievePublic(
            @PathVariable String assetId,
            HttpServletResponse response
    ) throws IOException {
        // Check if the assetId is a valid UUID.
        try {
            UUID.fromString(assetId);
        } catch (Exception e) {
            throw new NotFoundException();
        }

        var asset = assetRepository
                .findById(assetId)
                .orElseThrow(NotFoundException::new);

        response.sendRedirect(goverConfig.createUrl("/api/public/assets/" + assetId + "/" + asset.getFilename()));
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

        var path = assetStorageService
                .getAssetPath(asset.getKey())
                .orElseThrow(() -> new RuntimeException("Could not get asset path."));

        try {
            file.transferTo(path);
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

        var path = assetStorageService
                .getAssetPath(asset.getKey())
                .orElseThrow(NotFoundException::new);

        File file = path.toFile();

        if (file.exists() && file.canWrite()) {
            if (!file.delete()) {
                throw new ConflictException();
            }
        }

        assetRepository.delete(asset);
    }
}
