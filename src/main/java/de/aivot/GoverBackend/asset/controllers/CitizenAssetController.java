package de.aivot.GoverBackend.asset.controllers;

import de.aivot.GoverBackend.asset.entities.AssetEntity;
import de.aivot.GoverBackend.asset.repositories.AssetRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.storage.services.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/public/assets/")
@Tag(
        name = OpenApiConstants.Tags.AssetsName,
        description = OpenApiConstants.Tags.AssetsDescription
)
public class CitizenAssetController {
    private final StorageService storageService;
    private final AssetRepository assetRepository;

    @Autowired
    public CitizenAssetController(StorageService storageService,
                                  AssetRepository assetRepository) {
        this.storageService = storageService;
        this.assetRepository = assetRepository;
    }

    /**
     * Retrieve a single asset as an anonymous user.
     *
     * @param assetId The id of the asset to retrieve.
     */
    @GetMapping("{assetId}")
    @Operation(
            summary = "Retrieve Public Asset by ID",
            description = "Retrieves a public asset by its ID. Redirects to the actual file location."
    )
    public void retrievePublicById(
            @PathVariable String assetId,
            @RequestParam(defaultValue = "false") boolean download,
            HttpServletResponse response
    ) throws IOException, ResponseException {
        var asset = getPublicAssetById(assetId);

        var urlEncodedFilename = URLEncoder
                .encode(asset.getFilename(), StandardCharsets.UTF_8);

        var targetUrl = "/api/public/assets/" + assetId + "/" + urlEncodedFilename + "?download=" + download;

        response.sendRedirect(targetUrl);
    }

    @GetMapping("{assetId}/{filename}")
    @Operation(
            summary = "Retrieve Public Asset File",
            description = "Retrieves the actual file of a public asset by its ID and filename. Redirects to the storage location or serves the file directly if local storage is enabled."
    )
    public void retrievePublicFile(
            @PathVariable String assetId,
            @PathVariable String filename,
            @RequestParam(defaultValue = "false") boolean download,
            HttpServletResponse response
    ) throws IOException, ResponseException {
        var asset = getPublicAssetById(assetId);

        var urlEncodedFilename = URLEncoder
                .encode(asset.getFilename(), StandardCharsets.UTF_8);
        var targetUrl = "/api/public/assets/local/" + assetId + "/" + urlEncodedFilename + "?download=" + download;
        response.sendRedirect(targetUrl);
    }


    @GetMapping("local/{assetId}/{filename}")
    @Operation(
            summary = "Retrieve Public Asset File from Local Storage",
            description = "Retrieves the actual file of a public asset stored in local storage by its ID and filename."
    )
    public ResponseEntity<InputStreamResource> retrievePublicLocalFile(
            @PathVariable String assetId,
            @PathVariable String filename,
            @RequestParam(defaultValue = "false") boolean download
    ) throws ResponseException {
        var asset = getPublicAssetById(assetId);

        if (asset.getStorageProviderId() == null || asset.getStoragePathFromRoot() == null) {
            throw ResponseException.notFound();
        }

        var inputStream = storageService
                .getDocumentContent(asset.getStorageProviderId(), asset.getStoragePathFromRoot());

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(asset.getContentType());
        } catch (InvalidMediaTypeException e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok().contentType(mediaType);

        if (download) {
            responseBuilder.header("Content-Disposition", "attachment; filename=\"" + asset.getFilename() + "\"");
        }

        return responseBuilder.body(new InputStreamResource(inputStream));
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
            var uuid = UUID.fromString(assetId);
            return getAssetById(uuid);
        } catch (Exception e) {
            throw ResponseException.notFound();
        }
    }

    private AssetEntity getAssetById(UUID assetId) throws ResponseException {
        return assetRepository
                .findById(assetId)
                .orElseThrow(ResponseException::notFound);
    }
}
