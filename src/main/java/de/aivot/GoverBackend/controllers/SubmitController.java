package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.pdf.ApplicationPdfDto;
import de.aivot.GoverBackend.models.entities.Application;
import de.aivot.GoverBackend.models.entities.Department;
import de.aivot.GoverBackend.models.entities.Destination;
import de.aivot.GoverBackend.models.SendCopyRequest;
import de.aivot.GoverBackend.models.elements.steps.IntroductionStepElement;
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
import javax.script.ScriptEngine;
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
    private final SystemMailService systemMailService;
    private final BlobService blobService;

    @Autowired
    public SubmitController(
            ApplicationRepository applicationRepository,
            DestinationRepository destinationRepository,
            DepartmentRepository departmentRepository,
            PdfService pdfService,
            DestinationSubmitService destinationSubmitService,
            CustomerMailService customerMailService,
            SystemMailService systemMailService,
            BlobService blobService) {
        this.applicationRepository = applicationRepository;
        this.destinationRepository = destinationRepository;
        this.departmentRepository = departmentRepository;
        this.pdfService = pdfService;
        this.destinationSubmitService = destinationSubmitService;
        this.customerMailService = customerMailService;
        this.systemMailService = systemMailService;
        this.blobService = blobService;
    }

    @GetMapping("/api/public/prints/{uuid}")
    public ResponseEntity<Resource> getPrint(@PathVariable String uuid) {
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
    public String submit(@PathVariable Long applicationId, @RequestParam(value = "inputs", required = true) String inputs, @RequestParam(value = "files", required = false) MultipartFile[] files) {
        Optional<Application> fetchedApplication = applicationRepository.findById(applicationId);
        ScriptEngine scriptEngine = ScriptService.getEngine();
        Map<String, Object> customerData = new JSONObject(inputs).toMap();

        if (fetchedApplication.isPresent()) {
            Application application = fetchedApplication.get();
            RootElement root = application.getRoot();
            try {
                root.validate(root, customerData, null, scriptEngine);
            } catch (ValidationException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Field " + ex.getElement().getId() + ": " + ex.getMessage());
            }

            Integer destinationId = application.getRoot().getDestination();
            Optional<Destination> destination = Optional.empty();
            if (destinationId != null) {
                destination = destinationRepository.findById(Long.valueOf(destinationId));
            }

            if (files != null && files.length > 0 && destination.isPresent() && destination.get().getMaxAttachmentMegaBytes() != null) {
                long filesTotalBytes = 0;
                for (MultipartFile file : files) {
                    filesTotalBytes += file.getSize();
                }
                long allowedTotalBytes = destination.get().getMaxAttachmentMegaBytes() * 1000 * 1000;
                if (filesTotalBytes > allowedTotalBytes) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Exceeded max allowed file size of destination");
                }
            }

            ApplicationPdfDto applicationDto = new ApplicationPdfDto(application, customerData, scriptEngine);

            String pdfUuid;
            try {
                pdfUuid = pdfService.generatePdf(application, applicationDto);
            } catch (IOException | InterruptedException e) {
                systemMailService.sendExceptionMail(e);
                throw new RuntimeException(e);
            }

            if (destination.isPresent()) {
                try {
                    destinationSubmitService.handleSubmit(destination.get(), application, customerData, blobService.getPrintPdfPath(pdfUuid).toString(), files != null ? files : new MultipartFile[]{});
                } catch (IOException | InterruptedException | MessagingException e) {
                    systemMailService.sendExceptionMail(e);
                    throw new RuntimeException(e);
                }
            }

            return "/api/public/prints/" + pdfUuid;
        }

        throw new ResourceNotFoundException();
    }

    @PostMapping("/api/public/send-copy/{applicationId}")
    public ResponseEntity<HttpStatus> sendCopy(@PathVariable Long applicationId, @RequestBody SendCopyRequest sendCopyRequest) {
        Optional<Application> fetchedApplication = applicationRepository.findById(applicationId);
        if (fetchedApplication.isPresent()) {
            Application application = fetchedApplication.get();

            IntroductionStepElement introStep = application.getRoot().getIntroductionStep();

            Integer departmentId = introStep.getManagingDepartment();
            if (departmentId == null) {
                departmentId = introStep.getResponsibleDepartment();
            }

            Department department = null;
            if (departmentId != null) {
                Optional<Department> _department = departmentRepository.findById(Long.valueOf(departmentId));
                if (_department.isPresent()) {
                    department = _department.get();
                }
            }

            try {
                customerMailService.sendApplicationCopyMail(sendCopyRequest.getEmail(), application, department, sendCopyRequest.getPdfLink());
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
