package de.aivot.GoverBackend.plugins.form.v1.nodes;

import de.aivot.GoverBackend.department.entities.VDepartmentShadowedEntity;
import de.aivot.GoverBackend.department.services.VDepartmentShadowedService;
import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.pdf.models.PrintableFormPdfData;
import de.aivot.GoverBackend.permissions.services.PermissionService;
import de.aivot.GoverBackend.process.entities.ProcessEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.permissions.ProcessPermissionProvider;
import de.aivot.GoverBackend.process.services.ProcessNodeDefinitionService;
import de.aivot.GoverBackend.process.services.ProcessNodeService;
import de.aivot.GoverBackend.process.services.ProcessService;
import de.aivot.GoverBackend.process.services.PublicUrlService;
import de.aivot.GoverBackend.services.PdfService;
import de.aivot.GoverBackend.system.services.SystemService;
import de.aivot.GoverBackend.theme.entities.ThemeEntity;
import de.aivot.GoverBackend.theme.services.ThemeService;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.services.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FormTriggerUtilsControllerV1Test {
    @Test
    void printPdfShouldBuildProcessBasedUrlAndPreferResponsibleDepartment() throws Exception {
        var formTheme = createTheme(11, "Form Theme");
        var responsibleDepartment = new VDepartmentShadowedEntity()
                .setId(200)
                .setName("Responsible Department");
        var managingDepartment = new VDepartmentShadowedEntity()
                .setId(300)
                .setName("Managing Department");
        var formLayout = baseFormLayout()
                .setThemeId(formTheme.getId())
                .setResponsibleDepartmentId(responsibleDepartment.getId())
                .setManagingDepartmentId(managingDepartment.getId());
        var fixture = createFixture(formLayout);

        when(fixture.themeService().retrieve(formTheme.getId())).thenReturn(Optional.of(formTheme));
        when(fixture.departmentService().retrieve(responsibleDepartment.getId())).thenReturn(Optional.of(responsibleDepartment));
        when(fixture.departmentService().retrieve(managingDepartment.getId())).thenReturn(Optional.of(managingDepartment));
        when(fixture.pdfService().generatePrintableForm(
                any(PrintableFormPdfData.class),
                eq(formTheme),
                eq(responsibleDepartment),
                eq(responsibleDepartment),
                eq(managingDepartment)
        ))
                .thenReturn(new byte[]{1, 2, 3});

        var response = fixture.controller().printPdf(null, fixture.node().getId());

        verify(fixture.permissionService()).testDepartmentPermission(
                fixture.user().getId(),
                fixture.process().getDepartmentId(),
                ProcessPermissionProvider.PROCESS_DEFINITION_READ
        );
        var printableFormCaptor = ArgumentCaptor.forClass(PrintableFormPdfData.class);
        verify(fixture.pdfService()).generatePrintableForm(
                printableFormCaptor.capture(),
                eq(formTheme),
                eq(responsibleDepartment),
                eq(responsibleDepartment),
                eq(managingDepartment)
        );
        var printableForm = printableFormCaptor.getValue();
        assertEquals("/form/%s/%s/".formatted(
                fixture.process().getSlug(),
                fixture.formSlug()
        ), printableForm.getSlug());
        assertEquals(fixture.node().getName(), printableForm.getInternalTitle());
        assertEquals(fixture.node().getProcessVersion(), printableForm.getVersion());
        assertEquals(formLayout.getPublicTitle(), printableForm.getPublicTitle());
        assertEquals(formLayout, printableForm.getRootElement());
        assertEquals(formLayout.getPdfTemplateKey(), printableForm.getPdfTemplateKey());

        assertEquals("example-form-7.pdf", ContentDisposition.parse(response.getHeaders().getFirst("Content-Disposition")).getFilename());
        assertArrayEquals(new byte[]{1, 2, 3}, ((ByteArrayResource) response.getBody()).getByteArray());
    }

    @Test
    void printPdfShouldFallbackToManagingDepartmentForThemeAndLetterhead() throws Exception {
        var managingTheme = createTheme(21, "Managing Theme");
        var managingDepartment = new VDepartmentShadowedEntity()
                .setId(300)
                .setThemeId(managingTheme.getId())
                .setName("Managing Department");
        var formLayout = baseFormLayout()
                .setManagingDepartmentId(managingDepartment.getId());
        var fixture = createFixture(formLayout);

        when(fixture.departmentService().retrieve(managingDepartment.getId())).thenReturn(Optional.of(managingDepartment));
        when(fixture.themeService().retrieve(managingTheme.getId())).thenReturn(Optional.of(managingTheme));
        when(fixture.pdfService().generatePrintableForm(
                any(PrintableFormPdfData.class),
                eq(managingTheme),
                eq(managingDepartment),
                isNull(),
                eq(managingDepartment)
        ))
                .thenReturn(new byte[]{4, 5, 6});

        var response = fixture.controller().printPdf(null, fixture.node().getId());

        assertEquals("example-form-7.pdf", ContentDisposition.parse(response.getHeaders().getFirst("Content-Disposition")).getFilename());
        assertArrayEquals(new byte[]{4, 5, 6}, ((ByteArrayResource) response.getBody()).getByteArray());
    }

    @Test
    void printPdfShouldFallbackToProcessDepartmentAndSystemTheme() throws Exception {
        var processDepartment = new VDepartmentShadowedEntity()
                .setId(400)
                .setName("Process Department");
        var systemTheme = createTheme(31, "System Theme");
        var fixture = createFixture(baseFormLayout());
        fixture.process().setDepartmentId(processDepartment.getId());

        when(fixture.departmentService().retrieve(processDepartment.getId())).thenReturn(Optional.of(processDepartment));
        when(fixture.systemService().retrieveDefaultTheme()).thenReturn(systemTheme);
        when(fixture.pdfService().generatePrintableForm(
                any(PrintableFormPdfData.class),
                eq(systemTheme),
                eq(processDepartment),
                isNull(),
                isNull()
        ))
                .thenReturn(new byte[]{7, 8, 9});

        var response = fixture.controller().printPdf(null, fixture.node().getId());

        assertEquals("example-form-7.pdf", ContentDisposition.parse(response.getHeaders().getFirst("Content-Disposition")).getFilename());
        assertArrayEquals(new byte[]{7, 8, 9}, ((ByteArrayResource) response.getBody()).getByteArray());
    }

    @Test
    void printPdfShouldRejectNonFormTriggerNodes() throws Exception {
        var fixture = createFixture(baseFormLayout());
        fixture.node()
                .setProcessNodeDefinitionKey("core.http")
                .setProcessNodeDefinitionVersion(1);

        var error = assertThrows(ResponseException.class, () -> fixture.controller().printPdf(null, fixture.node().getId()));

        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, error.getStatus());
        verify(fixture.pdfService(), never()).generatePrintableForm(
                any(PrintableFormPdfData.class),
                any(ThemeEntity.class),
                any(VDepartmentShadowedEntity.class),
                any(),
                any()
        );
    }

    private TestFixture createFixture(FormLayoutElement formLayout) throws Exception {
        var processAccessKey = UUID.randomUUID();
        var processSlug = "example-process";
        var formSlug = "example-form";

        var publicUrlService = mock(PublicUrlService.class);
        when(publicUrlService.createPublicFormUrl(any(ProcessEntity.class), eq(formSlug)))
                .thenReturn("https://gover.example/form/%s/%s/".formatted(processSlug, formSlug));

        var user = new UserEntity()
                .setId("user-1");
        var userService = mock(UserService.class);
        when(userService.fromJWT(isNull())).thenReturn(Optional.of(user));

        var permissionService = mock(PermissionService.class);

        var process = new ProcessEntity()
                .setId(100)
                .setDepartmentId(400)
                .setAccessKey(processAccessKey)
                .setSlug(processSlug)
                .setInternalTitle("Process")
                .setVersionCount(1)
                .setPublishedVersion(1);
        var processService = mock(ProcessService.class);
        when(processService.retrieve(process.getId())).thenReturn(Optional.of(process));

        var node = new ProcessNodeEntity()
                .setId(500)
                .setProcessId(process.getId())
                .setProcessVersion(7)
                .setName("Trigger Form")
                .setDataKey("form")
                .setProcessNodeDefinitionKey("form.form")
                .setProcessNodeDefinitionVersion(1);
        var processNodeService = mock(ProcessNodeService.class);
        when(processNodeService.retrieve(node.getId())).thenReturn(Optional.of(node));

        var provider = mock(FormTriggerNodeV1.class);
        when(provider.getKey()).thenReturn("form.form");
        when(provider.getMajorVersion()).thenReturn(1);

        var config = new FormTriggerConfigV1();
        config.formSlug = formSlug;
        config.formLayout = formLayout;

        when(processNodeService.deriveConfiguration(eq(node), eq(provider), eq(user), eq(true)))
                .thenReturn(new ProcessNodeService.ProcessConfigurationDetails<>(
                        config,
                        new DerivedRuntimeElementData()
                ));

        var processNodeDefinitionService = mock(ProcessNodeDefinitionService.class);
        when(processNodeDefinitionService.getProcessNodeDefinition(eq(node), eq(FormTriggerNodeV1.class)))
                .thenReturn(Optional.of(provider));

        var departmentService = mock(VDepartmentShadowedService.class);
        var themeService = mock(ThemeService.class);
        var systemService = mock(SystemService.class);
        when(systemService.retrieveDefaultTheme()).thenReturn(createTheme(1, "System Theme"));

        var pdfService = mock(PdfService.class);
        when(pdfService.generatePrintableForm(
                any(PrintableFormPdfData.class),
                any(ThemeEntity.class),
                any(VDepartmentShadowedEntity.class),
                any(),
                any()
        ))
                .thenReturn(new byte[]{0});

        var controller = new FormTriggerUtilsControllerV1(
                publicUrlService,
                userService,
                permissionService,
                processService,
                processNodeService,
                processNodeDefinitionService,
                departmentService,
                themeService,
                systemService,
                pdfService,
                provider
        );

        return new TestFixture(
                controller,
                userService,
                permissionService,
                processService,
                processNodeService,
                processNodeDefinitionService,
                departmentService,
                themeService,
                systemService,
                pdfService,
                provider,
                user,
                process,
                node,
                formSlug
        );
    }

    private FormLayoutElement baseFormLayout() {
        return new FormLayoutElement()
                .setPublicTitle("Citizen Form")
                .setPdfTemplateKey(UUID.randomUUID());
    }

    private ThemeEntity createTheme(int id, String name) {
        return new ThemeEntity()
                .setId(id)
                .setName(name);
    }

    private record TestFixture(
            FormTriggerUtilsControllerV1 controller,
            UserService userService,
            PermissionService permissionService,
            ProcessService processService,
            ProcessNodeService processNodeService,
            ProcessNodeDefinitionService processNodeDefinitionService,
            VDepartmentShadowedService departmentService,
            ThemeService themeService,
            SystemService systemService,
            PdfService pdfService,
            FormTriggerNodeV1 provider,
            UserEntity user,
            ProcessEntity process,
            ProcessNodeEntity node,
            String formSlug
    ) {
    }
}
