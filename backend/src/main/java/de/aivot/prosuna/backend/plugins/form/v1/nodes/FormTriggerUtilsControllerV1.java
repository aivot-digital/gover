package de.aivot.prosuna.backend.plugins.form.v1.nodes;

import de.aivot.prosuna.backend.department.entities.VDepartmentShadowedEntity;
import de.aivot.prosuna.backend.department.services.VDepartmentShadowedService;
import de.aivot.prosuna.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.pdf.models.PrintableFormPdfData;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntityId;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.services.ProcessNodeDefinitionService;
import de.aivot.prosuna.backend.process.services.ProcessNodeService;
import de.aivot.prosuna.backend.process.services.ProcessService;
import de.aivot.prosuna.backend.process.services.ProcessVersionService;
import de.aivot.prosuna.backend.process.services.PublicUrlService;
import de.aivot.prosuna.backend.services.PdfService;
import de.aivot.prosuna.backend.theme.services.ThemeService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import de.aivot.prosuna.backend.utils.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.exceptions.TemplateProcessingException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@RestController
@RequestMapping("/api/forms/v1/{nodeId}/")
@Tag(
        name = "Forms",
        description = "Forms are built for collecting data from users. " +
                      "They can be designed with various elements and configurations to suit different data collection needs. " +
                      "Forms can be published, managed, and analyzed within the system."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class FormTriggerUtilsControllerV1 {
    private final PublicUrlService publicUrlService;
    private final UserService userService;
    private final PermissionService permissionService;
    private final ProcessService processService;
    private final ProcessNodeService processNodeService;
    private final ProcessVersionService processVersionService;
    private final ProcessNodeDefinitionService processNodeDefinitionService;
    private final VDepartmentShadowedService vDepartmentShadowedService;
    private final ThemeService themeService;
    private final PdfService pdfService;
    private final FormTriggerNodeV1 formTriggerNodeV1;

    @Autowired
    public FormTriggerUtilsControllerV1(PublicUrlService publicUrlService,
                                        UserService userService,
                                        PermissionService permissionService,
                                        ProcessService processService,
                                        ProcessNodeService processNodeService,
                                        ProcessVersionService processVersionService,
                                        ProcessNodeDefinitionService processNodeDefinitionService,
                                        VDepartmentShadowedService vDepartmentShadowedService,
                                        ThemeService themeService,
                                        PdfService pdfService,
                                        FormTriggerNodeV1 formTriggerNodeV1) {
        this.publicUrlService = publicUrlService;
        this.userService = userService;
        this.permissionService = permissionService;
        this.processService = processService;
        this.processNodeService = processNodeService;
        this.processVersionService = processVersionService;
        this.processNodeDefinitionService = processNodeDefinitionService;
        this.vDepartmentShadowedService = vDepartmentShadowedService;
        this.themeService = themeService;
        this.pdfService = pdfService;
        this.formTriggerNodeV1 = formTriggerNodeV1;
    }

    @GetMapping("print-pdf/")
    @Operation(
            summary = "Print a form trigger as PDF",
            description = "Generate and retrieve a printable PDF version of the configured form layout for a form trigger node."
    )
    public ResponseEntity<Resource> printPdf(@Nullable @AuthenticationPrincipal Jwt jwt,
                                             @Nonnull @PathVariable Integer nodeId) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var node = processNodeService
                .retrieve(nodeId)
                .orElseThrow(ResponseException::notFound);
        ensureFormTriggerNode(node);

        var process = processService
                .retrieve(node.getProcessId())
                .orElseThrow(ResponseException::badRequest);
        var processVersion = processVersionService
                .retrieve(ProcessVersionEntityId.of(node.getProcessId(), node.getProcessVersion()))
                .orElseThrow(ResponseException::notFound);

        permissionService.requireDepartmentPermission(
                execUser.getId(),
                process.getDepartmentId(),
                ProcessPermissionProvider.PROCESS_DEFINITION_READ
        );

        var config = resolveFormTriggerConfiguration(node, execUser);
        var printableForm = buildPrintableForm(process, node, config);
        var theme = themeService
                .getFormThemesInOrderOfImportance(processVersion, config.formLayout)
                .getFirst();
        var responsibleDepartment = getDepartment(config.formLayout.getResponsibleDepartmentId()).orElse(null);
        var managingDepartment = getDepartment(config.formLayout.getManagingDepartmentId()).orElse(null);
        var department = getPrintableDepartment(process, responsibleDepartment, managingDepartment);

        byte[] bytes;
        try {
            bytes = pdfService.generatePrintableForm(
                    printableForm,
                    theme,
                    department,
                    responsibleDepartment,
                    managingDepartment
            );
        } catch (IOException | URISyntaxException | InterruptedException | TemplateProcessingException e) {
            throw ResponseException.internalServerError("Fehler beim Erzeugen der PDF-Datei. Bitte versuchen Sie es später erneut.", e);
        }

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(bytes.length)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition
                                .attachment()
                                .filename(config.formSlug + "-" + node.getProcessVersion() + ".pdf", StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(new ByteArrayResource(bytes));
    }

    private void ensureFormTriggerNode(@Nonnull ProcessNodeEntity node) throws ResponseException {
        if (!formTriggerNodeV1.getKey().equals(node.getProcessNodeDefinitionKey()) ||
                !formTriggerNodeV1.getMajorVersion().equals(node.getProcessNodeDefinitionVersion())) {
            throw ResponseException.badRequest("Das angegebene Prozesselement ist kein Formulareingang.");
        }
    }

    @Nonnull
    private ResolvedFormTriggerConfiguration resolveFormTriggerConfiguration(@Nonnull ProcessNodeEntity node,
                                                                             @Nonnull UserEntity execUser) throws ResponseException {
        var provider = processNodeDefinitionService
                .getProcessNodeDefinition(node, FormTriggerNodeV1.class)
                .orElseThrow(ResponseException::notFound);

        var config = processNodeService
                .deriveConfiguration(node, provider, execUser, true)
                .configuration();

        if (config.formLayout == null) {
            throw ResponseException.internalServerError("Die Konfiguration des Formulareingangs enthält kein Formular.");
        }

        if (StringUtils.isNullOrEmpty(config.formSlug)) {
            throw ResponseException.internalServerError("Die Konfiguration des Formulareingangs enthält keinen Formular-Slug.");
        }

        return new ResolvedFormTriggerConfiguration(
                config.formSlug,
                config.formLayout
        );
    }

    @Nonnull
    private PrintableFormPdfData buildPrintableForm(@Nonnull ProcessEntity process,
                                                    @Nonnull ProcessNodeEntity node,
                                                    @Nonnull ResolvedFormTriggerConfiguration config) {
        return new PrintableFormPdfData()
                .setSlug(createPublicFormPath(process, config.formSlug))
                .setInternalTitle(node.resolveName(formTriggerNodeV1))
                .setVersion(node.getProcessVersion())
                .setPublicTitle(config.formLayout.getPublicTitle())
                .setRootElement(config.formLayout)
                .setPdfTemplateKey(config.formLayout.getPdfTemplateKey());
    }

    @Nonnull
    private String createPublicFormPath(@Nonnull ProcessEntity process,
                                        @Nonnull String formSlug) {
        var publicUrl = publicUrlService.createPublicFormUrl(process, formSlug);
        return URI.create(publicUrl).getRawPath();
    }

    @Nonnull
    private VDepartmentShadowedEntity getPrintableDepartment(@Nonnull ProcessEntity process,
                                                             @Nullable VDepartmentShadowedEntity responsibleDepartment,
                                                             @Nullable VDepartmentShadowedEntity managingDepartment) throws ResponseException {
        if (responsibleDepartment != null) {
            return responsibleDepartment;
        }

        if (managingDepartment != null) {
            return managingDepartment;
        }

        return getDepartment(process.getDepartmentId())
                .orElseThrow(() -> ResponseException.internalServerError("Keine zuständige Organisationseinheit für den PDF-Druck gefunden."));
    }

    @Nonnull
    private Optional<VDepartmentShadowedEntity> getDepartment(@Nullable Integer departmentId) {
        if (departmentId == null) {
            return Optional.empty();
        }

        return vDepartmentShadowedService.retrieve(departmentId);
    }

    private record ResolvedFormTriggerConfiguration(
            @Nonnull
            String formSlug,
            @Nonnull
            FormLayoutElement formLayout
    ) {
    }
}
