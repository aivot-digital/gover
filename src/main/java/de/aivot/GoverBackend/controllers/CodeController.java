package de.aivot.GoverBackend.controllers;

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
public class CodeController {
    private final SystemMailService systemMailService;
    private final BlobService blobService;

    @Autowired
    public CodeController(SystemMailService systemMailService, BlobService blobService) {
        this.systemMailService = systemMailService;
        this.blobService = blobService;
    }

    @GetMapping("/api/public/code/{id}")
    public ResponseEntity<Resource> getCode(@PathVariable Long id) {
        Path path = blobService.getCodePath(id);
        try {
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() || resource.isReadable()) {
                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.setContentType(MediaType.valueOf("text/javascript"));
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

    @PostMapping("/api/code/{id}")
    public ResponseEntity<HttpStatus> saveCode(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        Path pathCode = blobService.getCodePath(id);
        try {
            file.transferTo(pathCode);
        } catch (IOException e) {
            systemMailService.sendExceptionMail(e);
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok(HttpStatus.OK);
    }
}
