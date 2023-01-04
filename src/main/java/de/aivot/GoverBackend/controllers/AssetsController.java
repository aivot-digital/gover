package de.aivot.GoverBackend.controllers;

import com.sun.istack.Nullable;
import de.aivot.GoverBackend.enums.SystemAssetKey;
import de.aivot.GoverBackend.services.BlobService;
import de.aivot.GoverBackend.services.SystemMailService;
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@RestController
@CrossOrigin
public class AssetsController {
    @Autowired
    BlobService blobService;
    @Autowired
    SystemMailService systemMailService;

    @GetMapping("/public/system-assets/{assetKey}")
    public RedirectView getAsset(@PathVariable String assetKey) {
        SystemAssetKey key = getSystemAssetKey(assetKey);
        if (key == null) {
            throw new ResourceNotFoundException();
        }

        String link = blobService.getAssetLink(key) + "?ts=" + LocalDateTime.now();
        return new RedirectView(link);
    }

    @PostMapping("/system-assets/{assetKey}")
    public String postAsset(@PathVariable String assetKey, @RequestParam("file") MultipartFile file) {
        SystemAssetKey key = getSystemAssetKey(assetKey);
        if (key == null) {
            throw new ResourceNotFoundException();
        }

        try {
            return blobService.storeData("assets", key.getKey(), file.getBytes());
        } catch (ServerException | InternalException | InsufficientDataException | ErrorResponseException |
                 IOException | NoSuchAlgorithmException | InvalidKeyException | InvalidResponseException |
                 XmlParserException e) {
            systemMailService.sendExceptionMail(e);
            throw new RuntimeException(e);
        }
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
