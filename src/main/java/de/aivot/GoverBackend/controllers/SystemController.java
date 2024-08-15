package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.exceptions.ForbiddenException;
import de.aivot.GoverBackend.exceptions.NotFoundException;
import de.aivot.GoverBackend.models.auth.KeyCloakDetailsUser;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.models.config.StorageConfig;
import de.aivot.GoverBackend.models.dtos.PaymentProviderInfo;
import de.aivot.GoverBackend.models.dtos.SmtpResultDto;
import de.aivot.GoverBackend.models.dtos.TelemetryDto;
import de.aivot.GoverBackend.models.dtos.TestSmtpDto;
import de.aivot.GoverBackend.repositories.DepartmentRepository;
import de.aivot.GoverBackend.repositories.FormRepository;
import de.aivot.GoverBackend.repositories.SubmissionRepository;
import de.aivot.GoverBackend.repositories.SystemConfigRepository;
import de.aivot.GoverBackend.utils.StringUtils;
import de.aivot.GoverBackend.services.mail.SmtpTestMailService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@RestController
public class SystemController {
    private final SmtpTestMailService mailService;
    private final GoverConfig goverConfig;
    private final StorageConfig storageConfig;
    private final SystemConfigRepository systemConfigRepository;
    private final FormRepository formRepository;
    private final SubmissionRepository submissionRepository;
    private final DepartmentRepository departmentRepository;

    @Autowired
    public SystemController(
            SmtpTestMailService mailService,
            GoverConfig goverConfig,
            StorageConfig storageConfig,
            SystemConfigRepository systemConfigRepository,
            FormRepository formRepository,
            SubmissionRepository submissionRepository,
            DepartmentRepository departmentRepository
    ) {
        this.mailService = mailService;
        this.goverConfig = goverConfig;
        this.storageConfig = storageConfig;
        this.systemConfigRepository = systemConfigRepository;
        this.formRepository = formRepository;
        this.submissionRepository = submissionRepository;
        this.departmentRepository = departmentRepository;
    }

    @PostMapping("/api/system/test-smtp")
    public SmtpResultDto testSmtp(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody TestSmtpDto payload
    ) {
        var user = KeyCloakDetailsUser
                .fromJwt(jwt);

        user.ifNotAdminThrow(ForbiddenException::new);

        var result = new SmtpResultDto();

        try {
            mailService.send(user, payload.getTargetMail());
        } catch (MessagingException | IOException e) {
            result.setResult(e.getMessage());
        }

        return result;
    }

    @GetMapping("/api/public/system/sentry-dsn")
    public List<String> getSentryDns() {
        return List.of(goverConfig.getSentryWebApp());
    }

    @GetMapping("/api/system/file-extensions")
    public List<String> getFileExtensions() {
        return goverConfig.getFileExtensions();
    }

    @GetMapping("/api/system/payment-providers")
    public List<PaymentProviderInfo> getPaymentProviders() {
        return goverConfig
                .getPaymentProvider()
                .entrySet()
                .stream()
                .filter(entry -> StringUtils.isNotNullOrEmpty(entry.getValue().get("url")))
                .map(entry -> new PaymentProviderInfo(entry.getKey(), entry.getValue().get("label")))
                .sorted(Comparator.comparing(PaymentProviderInfo::id))
                .toList();
    }

    @GetMapping("/api/public/system/telemetry")
    public TelemetryDto getTelemetry(
            @RequestParam(value = "key", required = true) String key
    ) {
        if (StringUtils.isNullOrEmpty(goverConfig.getTelemetryKey())) {
            throw new NotFoundException();
        }

        if (!goverConfig.getTelemetryKey().equals(key)) {
            throw new ForbiddenException();
        }

        return new TelemetryDto(
                goverConfig.toMap(),
                storageConfig.toMap(),
                systemConfigRepository.findAll(),
                formRepository.count(),
                submissionRepository.count(),
                departmentRepository.count()
        );
    }
}
