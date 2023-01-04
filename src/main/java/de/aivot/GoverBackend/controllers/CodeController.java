package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.services.BlobService;
import de.aivot.GoverBackend.services.SystemMailService;
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RestController
@CrossOrigin
public class CodeController {
    @Autowired
    BlobService blobService;
    @Autowired
    SystemMailService systemMailService;

    @GetMapping("/public/code/{id}")
    public RedirectView getCode(@PathVariable Long id) {
        String link = blobService.getCodeLink(id);
        return new RedirectView(link);
    }

    @PostMapping("/code/{id}")
    public String postCode(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            return blobService.storeData("code" ,id + ".js", file.getBytes());
        } catch (ServerException | InternalException | XmlParserException | InvalidResponseException |
                 InvalidKeyException | NoSuchAlgorithmException | IOException | ErrorResponseException |
                 InsufficientDataException e) {
            systemMailService.sendExceptionMail(e);
            throw new RuntimeException(e);
        }
    }
}
