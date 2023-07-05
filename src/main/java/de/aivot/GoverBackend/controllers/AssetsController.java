package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.permissions.IsAdmin;
import de.aivot.GoverBackend.services.AssetStorageService;
import de.aivot.GoverBackend.services.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.Stream;

@RestController
public class AssetsController {
    private final AssetStorageService assetStorageService;
    private final MailService mailService;

    @Autowired
    public AssetsController(
            AssetStorageService assetStorageService,
            MailService mailService
    ) {
        this.assetStorageService = assetStorageService;
        this.mailService = mailService;
    }

    @GetMapping("/api/public/system-assets/{assetKey}")
    public ResponseEntity<Resource> retrievePublic(@PathVariable String assetKey) {
        Optional<Path> path = assetStorageService.getAssetPath(assetKey);

        if (path.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        try {
            Resource resource = new UrlResource(path.get().toUri());
            String contentType;
            try {
                contentType = Files.probeContentType(path.get());
            } catch (IOException e) {
                contentType = MediaType.APPLICATION_OCTET_STREAM.getType();
            }
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity
                        .ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @IsAdmin
    @GetMapping("/api/system-assets")
    public Collection<String> list(
            Authentication authentication
    ) {
        File assetRoot = new File(assetStorageService.getAssetRoot());
        File[] files = assetRoot.listFiles();

        if (files == null) {
            return new LinkedList<>();
        } else {
            return Stream.of(files)
                    .filter(file -> !file.isDirectory())
                    .map(File::getName)
                    .toList();
        }
    }

    @IsAdmin
    @PostMapping("/api/system-assets")
    public ResponseEntity<HttpStatus> create(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) {
        String filename = file.getOriginalFilename();

        if (filename == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        Optional<Path> path = assetStorageService.getAssetPath(filename);

        if (path.isPresent()) {
            try {
                file.transferTo(path.get());
            } catch (IOException e) {
                mailService.sendExceptionMail(e);
                throw new RuntimeException(e);
            }
        }

        return ResponseEntity.ok(HttpStatus.OK);
    }

    @IsAdmin
    @DeleteMapping("/api/system-assets/{assetKey}")
    public ResponseEntity<HttpStatus> destroy(
            Authentication authentication,
            @PathVariable String assetKey
    ) {
        Optional<Path> path = assetStorageService.getAssetPath(assetKey);

        if (path.isPresent()) {
            File file = path.get().toFile();

            if (file.exists() && file.canWrite()) {
                if (!file.delete()) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT);
                }
            }
        }

        return ResponseEntity.ok(HttpStatus.OK);
    }
}
