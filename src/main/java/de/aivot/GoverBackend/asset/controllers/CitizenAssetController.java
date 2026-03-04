package de.aivot.GoverBackend.asset.controllers;

import de.aivot.GoverBackend.asset.repositories.VStorageIndexItemWithAssetRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.storage.services.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private final VStorageIndexItemWithAssetRepository vStorageIndexItemWithAssetRepository;

    @Autowired
    public CitizenAssetController(StorageService storageService,
                                  VStorageIndexItemWithAssetRepository vStorageIndexItemWithAssetRepository) {
        this.storageService = storageService;
        this.vStorageIndexItemWithAssetRepository = vStorageIndexItemWithAssetRepository;
    }

    /**
     * Retrieve a single asset as an anonymous user.
     *
     * @param assetId The id of the asset to retrieve.
     */
    @GetMapping("{assetId}/")
    @Operation(
            summary = "Retrieve Public Asset by key",
            description = "Retrieves a public asset by its key and streams the file directly."
    )
    public ResponseEntity<InputStreamResource> retrievePublicById(
            @PathVariable String assetId,
            @RequestParam(defaultValue = "false") boolean download
    ) throws ResponseException {
        UUID uuid;
        try {
            uuid = UUID.fromString(assetId);
        } catch (Exception e) {
            throw ResponseException.notFound();
        }

        var asset = vStorageIndexItemWithAssetRepository
                .findByAssetKey(uuid)
                .orElseThrow(ResponseException::notFound);

        if (!Boolean.FALSE.equals(asset.getAssetIsPrivate())) {
            throw ResponseException.notFound();
        }

        var inputStream = storageService
                .getDocumentContent(asset.getStorageProviderId(), asset.getPathFromRoot());

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(asset.getMimeType());
        } catch (InvalidMediaTypeException e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok().contentType(mediaType);
        var contentDispositionType = download ? "attachment" : "inline";
        var contentDisposition = ContentDisposition
                .builder(contentDispositionType)
                .filename(asset.getFilename(), StandardCharsets.UTF_8)
                .build();
        responseBuilder.header(
                "Content-Disposition",
                contentDisposition.toString()
        );

        return responseBuilder.body(new InputStreamResource(inputStream));
    }
}
