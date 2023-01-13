package de.aivot.GoverBackend.controllers;

import com.sun.istack.Nullable;
import de.aivot.GoverBackend.enums.SystemAssetKey;
import de.aivot.GoverBackend.services.BlobService;
import de.aivot.GoverBackend.services.SystemMailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;

@RestController
@CrossOrigin
public class AssetsController {
    private final BlobService blobService;
    private final SystemMailService systemMailService;

    @Autowired
    public AssetsController(BlobService blobService, SystemMailService systemMailService) {
        this.blobService = blobService;
        this.systemMailService = systemMailService;
    }

    @GetMapping("/api/public/system-assets/{assetKey}")
    public ResponseEntity<Resource> getAsset(@PathVariable String assetKey) {
        SystemAssetKey key = getSystemAssetKey(assetKey);
        if (key == null) {
            throw new ResourceNotFoundException();
        }

        Path path = blobService.getAssetPath(key);
        try {
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() || resource.isReadable()) {
                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.setContentType(MediaType.IMAGE_PNG);
                return ResponseEntity
                        .ok()
                        .headers(responseHeaders)
                        .body(resource);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        throw new ResourceNotFoundException();
    }

    @PostMapping("/api/system-assets/{assetKey}")
    public ResponseEntity<HttpStatus> postAsset(@PathVariable String assetKey, @RequestParam("file") MultipartFile file) {
        SystemAssetKey key = getSystemAssetKey(assetKey);
        if (key == null) {
            throw new ResourceNotFoundException();
        }

        Path path = blobService.getAssetPath(key);
        try {
            file.transferTo(path);
        } catch (IOException e) {
            systemMailService.sendExceptionMail(e);
            throw new RuntimeException(e);
        }

        return ResponseEntity.ok(HttpStatus.OK);
    }

    @Nullable
    private SystemAssetKey getSystemAssetKey(String assetKey) {
        for (SystemAssetKey key : SystemAssetKey.values()) {
            if (key.getKey().matches(assetKey)) {
                return key;
            }
        }
        return null;
    }
}
