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
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.mail.MessagingException;
import javax.script.ScriptException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin
public class SubmitController {
    @Autowired
    ApplicationRepository applicationRepository;
    @Autowired
    DestinationRepository destinationRepository;
    @Autowired
    DepartmentRepository departmentRepository;
    @Autowired
    PdfService pdfService;
    @Autowired
    DestinationSubmitService destinationSubmitService;
    @Autowired
    CustomerMailService customerMailService;
    @Autowired
    ScriptService scriptService;
    @Autowired
    SystemMailService systemMailService;

    @PostMapping("/public/submit/{applicationId}")
    public String submit(@PathVariable Long applicationId, @RequestBody Map<String, Object> customerData) {
        Optional<Application> application = applicationRepository.findById(applicationId);

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

            String pdfLink;
            try {
                pdfLink = pdfService.generatePdf(application.get(), applicationDto);
            } catch (IOException | InterruptedException | ServerException | InsufficientDataException |
                     ErrorResponseException | NoSuchAlgorithmException | InvalidKeyException |
                     InvalidResponseException | XmlParserException | InternalException e) {
                systemMailService.sendExceptionMail(e);
                throw new RuntimeException(e);
            }

            Integer destinationId = (Integer) application.get().getRoot().get("interface");
            if (destinationId != null) {
                Optional<Destination> destination = destinationRepository.findById(Long.valueOf(destinationId));
                if (destination.isPresent()) {
                    try {
                        destinationSubmitService.handleSubmit(destination.get(), application.get(), customerData, pdfLink);
                    } catch (MessagingException | IOException | InterruptedException e) {
                        systemMailService.sendExceptionMail(e);
                        throw new RuntimeException(e);
                    }
                }
            }

            return pdfLink;
        }

        throw new ResourceNotFoundException();
    }

    @PostMapping("/public/send-copy/{applicationId}")
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
            } catch (MessagingException | IOException e) {
                systemMailService.sendExceptionMail(e);
                throw new RuntimeException(e);
            }
        }
        return ResponseEntity.ok(HttpStatus.OK);
    }
}
