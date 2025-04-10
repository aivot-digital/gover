package de.aivot.GoverBackend.asset.controllers;

import de.aivot.GoverBackend.asset.entities.AssetEntity;
import de.aivot.GoverBackend.asset.repositories.AssetRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.models.config.StorageConfig;
import de.aivot.GoverBackend.services.storages.AssetStorageService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/public/assets/")
public class CitizenAssetController {
    private final AssetStorageService assetStorageService;
    private final AssetRepository assetRepository;
    private final GoverConfig goverConfig;
    private final StorageConfig storageConfig;

    @Autowired
    public CitizenAssetController(
            AssetStorageService assetStorageService,
            AssetRepository assetRepository,
            GoverConfig goverConfig,
            StorageConfig storageConfig
    ) {
        this.assetStorageService = assetStorageService;
        this.assetRepository = assetRepository;
        this.goverConfig = goverConfig;
        this.storageConfig = storageConfig;
    }

    /**
     * Retrieve a single asset as an anonymous user.
     *
     * @param assetId The id of the asset to retrieve.
     */
    @GetMapping("{assetId}")
    public void retrievePublicById(
            @PathVariable String assetId,
            @RequestParam(defaultValue = "false") boolean download,
            HttpServletResponse response
    ) throws IOException, ResponseException {
        var asset = getPublicAssetById(assetId);

        var urlEncodedFilename = URLEncoder
                .encode(asset.getFilename(), StandardCharsets.UTF_8);

        var targetUrl = goverConfig.createUrl(
                "/api/public/assets/" + assetId + "/" + urlEncodedFilename + "?download=" + download
        );

        response.sendRedirect(targetUrl);
    }

    @GetMapping("{assetId}/{filename}")
    public void retrievePublicFile(
            @PathVariable String assetId,
            @PathVariable String filename,
            @RequestParam(defaultValue = "false") boolean download,
            HttpServletResponse response
    ) throws IOException, ResponseException {
        var asset = getPublicAssetById(assetId);

        if (storageConfig.localStorageEnabled()) {
            var urlEncodedFilename = URLEncoder
                    .encode(asset.getFilename(), StandardCharsets.UTF_8);
            var targetUrl = goverConfig.createUrl(
                    "/api/public/assets/local/" + assetId + "/" + urlEncodedFilename + "?download=" + download
            );
            response.sendRedirect(targetUrl);
        } else {
            var storageUrl = download
                    ? assetStorageService.getAssetDownloadUrl(asset)
                    : assetStorageService.getAssetUrl(asset);

            response.sendRedirect(storageUrl);
        }
    }


    @GetMapping("local/{assetId}/{filename}")
    public ResponseEntity<Resource> retrievePublicLocalFile(
            @PathVariable String assetId,
            @PathVariable String filename,
            @RequestParam(defaultValue = "false") boolean download
    ) throws ResponseException {
        // Check if the assetId is a valid UUID.
        var asset = getPublicAssetById(assetId);

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
            ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok().contentType(mediaType);

            if (download) {
                responseBuilder.header("Content-Disposition", "attachment; filename=\"" + asset.getFilename() + "\"");
            }

            return responseBuilder.body(resource);
        }

        throw ResponseException.notFound();
    }

    private AssetEntity getPublicAssetById(String assetId) throws ResponseException {
        var asset = getAssetById(assetId);

        if (asset.getPrivate()) {
            throw ResponseException.notFound();
        }

        return asset;
    }

    private AssetEntity getAssetById(String assetId) throws ResponseException {
        try {
            UUID.fromString(assetId);
        } catch (Exception e) {
            throw ResponseException.notFound();
        }

        return assetRepository
                .findById(assetId)
                .orElseThrow(ResponseException::notFound);
    }
}
