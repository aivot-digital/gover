package de.aivot.GoverBackend.asset.controllers;

import de.aivot.GoverBackend.asset.entities.VStorageIndexItemWithAssetEntity;
import de.aivot.GoverBackend.asset.entities.VStorageIndexItemWithAssetEntityId;
import de.aivot.GoverBackend.asset.repositories.VStorageIndexItemWithAssetRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.storage.services.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
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

        ensurePublicFile(asset);

        return createFileContentResponse(asset, download);
    }

    /**
     * Retrieve a single asset as an anonymous user by its storage provider id and path from root.
     *
     * @param storageProviderId The id of the storage provider containing the asset.
     * @param request           The request containing the asset path after the files marker.
     */
    @GetMapping("{storageProviderId}/files/**")
    @Operation(
            summary = "Retrieve Public Asset by storage provider id and path",
            description = "Retrieves a public asset by its storage provider id and path from root and streams the file directly."
    )
    public ResponseEntity<InputStreamResource> retrievePublicByPath(
            @Nonnull @PathVariable Integer storageProviderId,
            @RequestParam(defaultValue = "false") boolean download,
            @Nonnull HttpServletRequest request
    ) throws ResponseException {
        var filePath = getNormalizedFileContentPath(request);

        var asset = vStorageIndexItemWithAssetRepository
                .findById(VStorageIndexItemWithAssetEntityId.of(storageProviderId, filePath))
                .orElseThrow(ResponseException::notFound);

        ensurePublicFile(asset);

        return createFileContentResponse(asset, download);
    }

    private void ensurePublicFile(@Nonnull VStorageIndexItemWithAssetEntity asset) throws ResponseException {
        // Private assets are not public.
        // Directories are not public.
        // Missing assets are not public.
        if (
                Boolean.FALSE.equals(asset.getAssetIsPrivate())
                        && Boolean.FALSE.equals(asset.getDirectory())
                        && Boolean.FALSE.equals(asset.getMissing())
        ) {
            return;
        }
        throw ResponseException.notFound();
    }

    @Nonnull
    private ResponseEntity<InputStreamResource> createFileContentResponse(@Nonnull VStorageIndexItemWithAssetEntity asset,
                                                                          boolean download) throws ResponseException {
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

    private static String getNormalizedFileContentPath(@Nonnull HttpServletRequest request) throws ResponseException {
        var requestUrl = request
                .getRequestURL()
                .toString();

        var marker = "/files/";
        var markerIndex = requestUrl.indexOf(marker);
        if (markerIndex < 0) {
            throw ResponseException.notAcceptable("Der Root eines Speichers kann keine Datei sein.");
        }

        var normalizedPath = requestUrl.substring(markerIndex + marker.length());
        if (normalizedPath.isBlank()) {
            throw ResponseException.notAcceptable("Der Root eines Speichers kann keine Datei sein.");
        }

        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }

        if (normalizedPath.endsWith("/")) {
            throw ResponseException.notAcceptable("Der Pfad einer Datei darf nicht mit einem Schrägstrich (/) enden.");
        }

        return URLDecoder.decode(normalizedPath, StandardCharsets.UTF_8);
    }
}
