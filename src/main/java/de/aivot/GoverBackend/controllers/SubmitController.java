package de.aivot.GoverBackend.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.GoverBackend.dtos.ApplicationDto;
import de.aivot.GoverBackend.exceptions.ScriptRequiredException;
import de.aivot.GoverBackend.models.Application;
import de.aivot.GoverBackend.models.Department;
import de.aivot.GoverBackend.models.Destination;
import de.aivot.GoverBackend.models.SendCopyRequest;
import de.aivot.GoverBackend.repositories.ApplicationRepository;
import de.aivot.GoverBackend.repositories.DepartmentRepository;
import de.aivot.GoverBackend.repositories.DestinationRepository;
import de.aivot.GoverBackend.services.*;
import org.json.JSONObject;
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
import org.springframework.web.server.ResponseStatusException;

import javax.mail.MessagingException;
import javax.script.ScriptException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin
public class SubmitController {
    private final ApplicationRepository applicationRepository;
    private final DestinationRepository destinationRepository;
    private final DepartmentRepository departmentRepository;
    private final PdfService pdfService;
    private final DestinationSubmitService destinationSubmitService;
    private final CustomerMailService customerMailService;
    private final ScriptService scriptService;
    private final SystemMailService systemMailService;
    private final BlobService blobService;

    @Autowired
    public SubmitController(ApplicationRepository applicationRepository, DestinationRepository destinationRepository, DepartmentRepository departmentRepository, PdfService pdfService, DestinationSubmitService destinationSubmitService, CustomerMailService customerMailService, ScriptService scriptService, SystemMailService systemMailService, BlobService blobService) {
        this.applicationRepository = applicationRepository;
        this.destinationRepository = destinationRepository;
        this.departmentRepository = departmentRepository;
        this.pdfService = pdfService;
        this.destinationSubmitService = destinationSubmitService;
        this.customerMailService = customerMailService;
        this.scriptService = scriptService;
        this.systemMailService = systemMailService;
        this.blobService = blobService;
    }

    @GetMapping("/api/public/prints/{uuid}")
    public ResponseEntity<Resource> getCode(@PathVariable String uuid) {
        Path path = blobService.getPrintPdfPath(uuid);
        try {
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() || resource.isReadable()) {
                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.setContentType(MediaType.APPLICATION_PDF);
                responseHeaders.set("filename", uuid + ".pdf");
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

    @PostMapping("/api/public/submit/{applicationId}")
    public String submit(@PathVariable Long applicationId, @RequestParam(value = "inputs") String inputs, @RequestParam(value = "files", required = false) MultipartFile[] files) {
        Optional<Application> application = applicationRepository.findById(applicationId);

        Map<String, Object> customerData = new JSONObject(inputs).toMap();

        if (application.isPresent()) {
            String validationError;
            try {
                validationError = scriptService.validateApplication(application.get(), customerData);
            } catch (ScriptRequiredException | ScriptException | JsonProcessingException e) {
                systemMailService.sendExceptionMail(e);
                throw new RuntimeException(e);
            }

            if (validationError != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, validationError);
            }

            ApplicationDto applicationDto;
            try {
                applicationDto = new ApplicationDto(application.get(), customerData, scriptService);
            } catch (ScriptRequiredException | ScriptException | JsonProcessingException e) {
                systemMailService.sendExceptionMail(e);
                throw new RuntimeException(e);
            }

            String pdfUuid;
            try {
                pdfUuid = pdfService.generatePdf(application.get(), applicationDto);
            } catch (IOException | InterruptedException e) {
                systemMailService.sendExceptionMail(e);
                throw new RuntimeException(e);
            }

            Integer destinationId = (Integer) application.get().getRoot().get("interface");
            if (destinationId != null) {
                Optional<Destination> destination = destinationRepository.findById(Long.valueOf(destinationId));
                if (destination.isPresent()) {
                    try {
                        destinationSubmitService.handleSubmit(destination.get(), application.get(), customerData, blobService.getPrintPdfPath(pdfUuid).toString(), files);
                    } catch (IOException | InterruptedException | MessagingException e) {
                        systemMailService.sendExceptionMail(e);
                        throw new RuntimeException(e);
                    }
                }
            }

            return "/api/public/prints/" + pdfUuid;
        }

        throw new ResourceNotFoundException();
    }

    @PostMapping("/api/public/send-copy/{applicationId}")
    public ResponseEntity<HttpStatus> sendCopy(@PathVariable Long applicationId, @RequestBody SendCopyRequest sendCopyRequest) {
        Optional<Application> application = applicationRepository.findById(applicationId);
        if (application.isPresent()) {
            Map<String, Object> introStep = (Map<String, Object>) application.get().getRoot().get("introductionStep");

            Object storedDepartmentId = introStep.get("managingDepartment");
            if (storedDepartmentId == null || (storedDepartmentId instanceof String && ((String) storedDepartmentId).isEmpty())) {
                storedDepartmentId = introStep.get("responsibleDepartment");
            }

            Long departmentId = null;
            if (storedDepartmentId != null) {
                if (storedDepartmentId instanceof String) {
                    departmentId = Long.valueOf((String) storedDepartmentId);
                } else if (storedDepartmentId instanceof Integer) {
                    departmentId = Long.valueOf((Integer) storedDepartmentId);
                } else if (storedDepartmentId instanceof Long) {
                    departmentId = (Long) storedDepartmentId;
                }
            }

            Department department = null;
            if (departmentId != null) {
                Optional<Department> _department = departmentRepository.findById(departmentId);
                if (_department.isPresent()) {
                    department = _department.get();
                }
            }

            try {
                customerMailService.sendApplicationCopyMail(sendCopyRequest.getEmail(), application.get(), department, sendCopyRequest.getPdfLink());
            } catch (IOException e) {
                systemMailService.sendExceptionMail(e);
                throw new RuntimeException(e);
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        }
        return ResponseEntity.ok(HttpStatus.OK);
    }
}
