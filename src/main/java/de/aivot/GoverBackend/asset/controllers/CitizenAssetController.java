package de.aivot.GoverBackend.asset.controllers;

import de.aivot.GoverBackend.asset.services.AssetService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.storage.services.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final AssetService assetService;

    @Autowired
    public CitizenAssetController(StorageService storageService,
                                  AssetService assetService) {
        this.storageService = storageService;
        this.assetService = assetService;
    }

    /**
     * Retrieve a single asset as an anonymous user.
     *
     * @param assetId The id of the asset to retrieve.
     */
    @GetMapping("{assetId}")
    @Operation(
            summary = "Retrieve Public Asset by ID",
            description = "Retrieves a public asset by its ID and streams the file directly."
    )
    public ResponseEntity<InputStreamResource> retrievePublicById(
            @PathVariable String assetId,
            @RequestParam(defaultValue = "false") boolean download
    ) throws ResponseException {
        var uuid = parseAssetId(assetId);
        var asset = assetService
                .retrieve(uuid)
                .orElseThrow(ResponseException::notFound);
        if (asset.getPrivate()) {
            throw ResponseException.notFound();
        }

        var assetResponse = assetService
                .retrieveResponse(uuid)
                .orElseThrow(ResponseException::notFound);

        if (!hasStorageReference(asset)) {
            throw ResponseException.notFound();
        }

        var inputStream = storageService.getDocumentContent(asset.getStorageProviderId(), asset.getStoragePathFromRoot());

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(assetResponse.contentType());
        } catch (InvalidMediaTypeException e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok().contentType(mediaType);
        var contentDispositionType = download ? "attachment" : "inline";
        var contentDisposition = ContentDisposition
                .builder(contentDispositionType)
                .filename(assetResponse.filename(), StandardCharsets.UTF_8)
                .build();
        responseBuilder.header(
                "Content-Disposition",
                contentDisposition.toString()
        );

        return responseBuilder.body(new InputStreamResource(inputStream));
    }

    private static UUID parseAssetId(String assetId) throws ResponseException {
        try {
            return UUID.fromString(assetId);
        } catch (Exception e) {
            throw ResponseException.notFound();
        }
    }

    private static boolean hasStorageReference(de.aivot.GoverBackend.asset.entities.AssetEntity asset) {
        return asset.getStorageProviderId() != null &&
                asset.getStoragePathFromRoot() != null &&
                !asset.getStoragePathFromRoot().isBlank();
    }
}
