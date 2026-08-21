package de.aivot.prosuna.backend.plugins.form.v1.nodes;

import de.aivot.prosuna.backend.asset.services.AssetService;
import de.aivot.prosuna.backend.av.services.AVService;
import de.aivot.prosuna.backend.captcha.services.CaptchaReplayGuard;
import de.aivot.prosuna.backend.config.services.SystemConfigService;
import de.aivot.prosuna.backend.department.entities.VDepartmentShadowedEntity;
import de.aivot.prosuna.backend.department.services.VDepartmentShadowedService;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.prosuna.backend.elements.services.ElementDerivationService;
import de.aivot.prosuna.backend.enums.XBezahldienstStatus;
import de.aivot.prosuna.backend.identity.cache.repositories.IdentityCacheRepository;
import de.aivot.prosuna.backend.identity.services.IdentityProviderService;
import de.aivot.prosuna.backend.identity.services.IdentityService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentInformation;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentRequest;
import de.aivot.prosuna.backend.payment.repositories.PaymentProviderRepository;
import de.aivot.prosuna.backend.payment.services.PaymentPayloadCreationService;
import de.aivot.prosuna.backend.payment.services.PaymentProviderDefinitionsService;
import de.aivot.prosuna.backend.payment.services.PaymentTransactionService;
import de.aivot.prosuna.backend.plugins.form.v1.services.FormPaymentService;
import de.aivot.prosuna.backend.process.entities.*;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.filters.ProcessVersionFilter;
import de.aivot.prosuna.backend.process.filters.ProcessNodeFilter;
import de.aivot.prosuna.backend.process.services.*;
import de.aivot.prosuna.backend.services.PdfService;
import de.aivot.prosuna.backend.storage.services.StorageProviderService;
import de.aivot.prosuna.backend.payment.services.PaymentProviderService;
import de.aivot.prosuna.backend.storage.services.StorageService;
import de.aivot.prosuna.backend.submission.services.ElementDataTransformService;
import de.aivot.prosuna.backend.system.services.SystemService;
import de.aivot.prosuna.backend.theme.entities.ThemeEntity;
import de.aivot.prosuna.backend.theme.services.ThemeService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.Jwt;

import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FormTriggerControllerV1Test {
    @Test
    void getThemeShouldUseProcessVersionTheme() throws Exception {
        var formTheme = createTheme(11, "Form Theme", UUID.randomUUID(), UUID.randomUUID());
        var fixture = createFixture(baseFormLayout());
        fixture.processVersion().setThemeId(formTheme.getId());

        when(fixture.themeService().retrieve(formTheme.getId())).thenReturn(Optional.of(formTheme));

        var result = fixture.controller().getTheme(null, fixture.processSlug(), fixture.formSlug(), null, null);

        assertEquals(formTheme.getId(), result.id());
        assertEquals(formTheme.getName(), result.name());
        assertEquals(formTheme.getLogoKey(), result.logoKey());
        assertEquals(formTheme.getFaviconKey(), result.faviconKey());
    }

    @Test
    void getThemeShouldUseExplicitVersionBeforeTestClaim() throws Exception {
        var requestedProcessVersion = 7;
        var testClaimAccessKey = "claim-123";
        var formTheme = createTheme(11, "Form Theme", UUID.randomUUID(), UUID.randomUUID());
        var fixture = createFixture(
                baseFormLayout(),
                testClaimAccessKey,
                requestedProcessVersion
        );
        fixture.processVersion().setThemeId(formTheme.getId());

        when(fixture.themeService().retrieve(formTheme.getId())).thenReturn(Optional.of(formTheme));

        var result = fixture.controller().getTheme(
                mock(Jwt.class),
                fixture.processSlug(),
                fixture.formSlug(),
                testClaimAccessKey,
                requestedProcessVersion
        );

        assertEquals(formTheme.getId(), result.id());
        verify(fixture.processVersionService()).retrieve(argThat((ProcessVersionFilter filter) ->
                filter.getProcessId().equals(fixture.process().getId()) &&
                        filter.getProcessVersion().equals(requestedProcessVersion) &&
                        filter.getStatus() == null
        ));
        verify(fixture.processTestClaimService(), never()).retrieveByAccessKey(fixture.process().getId(), testClaimAccessKey);
    }

    @Test
    void getLogoShouldFallbackToResponsibleDepartmentThemeAndUseTestClaimVersion() throws Exception {
        var testClaimAccessKey = "claim-123";
        var formTheme = createTheme(11, "Form Theme", null, null);
        var responsibleTheme = createTheme(21, "Responsible Theme", UUID.randomUUID(), null);
        var fixture = createFixture(
                baseFormLayout()
                        .setResponsibleDepartmentId(200),
                testClaimAccessKey
        );
        fixture.processVersion().setThemeId(formTheme.getId());

        when(fixture.themeService().retrieve(formTheme.getId())).thenReturn(Optional.of(formTheme));
        when(fixture.departmentService().retrieve(200)).thenReturn(Optional.of(new VDepartmentShadowedEntity().setId(200).setThemeId(responsibleTheme.getId())));
        when(fixture.themeService().retrieve(responsibleTheme.getId())).thenReturn(Optional.of(responsibleTheme));

        var response = new MockHttpServletResponse();
        fixture.controller().getLogo(null, fixture.processSlug(), fixture.formSlug(), testClaimAccessKey, null, null, response);

        assertEquals("https://assets.example/" + responsibleTheme.getLogoKey(), response.getRedirectedUrl());
        verify(fixture.processTestClaimService()).retrieveByAccessKey(fixture.process().getId(), testClaimAccessKey);
    }

    @Test
    void getLogoShouldNotFallbackToDefaultLogoWhenCustomThemeChainProvidesNone() throws Exception {
        var formTheme = createTheme(11, "Form Theme", null, null);
        var fixture = createFixture(baseFormLayout());
        fixture.processVersion().setThemeId(formTheme.getId());

        when(fixture.themeService().retrieve(formTheme.getId())).thenReturn(Optional.of(formTheme));

        var response = new MockHttpServletResponse();
        fixture.controller().getLogo(null, fixture.processSlug(), fixture.formSlug(), null, null, null, response);

        assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());
    }

    @Test
    void getLogoShouldFallbackToDefaultLogoWhenNoCustomThemeIsResolved() throws Exception {
        var fixture = createFixture(baseFormLayout());

        var response = new MockHttpServletResponse();
        fixture.controller().getLogo(null, fixture.processSlug(), fixture.formSlug(), null, null, null, response);

        assertEquals("https://prosuna.example/assets/default-logo.png", response.getRedirectedUrl());
    }

    @Test
    void getLogoShouldPreferDarkLogoAndFallbackWithinTheSameTheme() throws Exception {
        var lightLogoKey = UUID.randomUUID();
        var darkLogoKey = UUID.randomUUID();
        var formTheme = createTheme(11, "Form Theme", lightLogoKey, null).setLogoKeyDark(darkLogoKey);
        var fixture = createFixture(baseFormLayout());
        fixture.processVersion().setThemeId(formTheme.getId());
        when(fixture.themeService().retrieve(formTheme.getId())).thenReturn(Optional.of(formTheme));

        var darkResponse = new MockHttpServletResponse();
        fixture.controller().getLogo(
                null, fixture.processSlug(), fixture.formSlug(), null, null, "dark", darkResponse
        );
        assertEquals("https://assets.example/" + darkLogoKey, darkResponse.getRedirectedUrl());

        formTheme.setLogoKeyDark(null);
        var fallbackResponse = new MockHttpServletResponse();
        fixture.controller().getLogo(
                null, fixture.processSlug(), fixture.formSlug(), null, null, "dark", fallbackResponse
        );
        assertEquals("https://assets.example/" + lightLogoKey, fallbackResponse.getRedirectedUrl());
    }

    @Test
    void getFaviconShouldFallbackToManagingDepartmentTheme() throws Exception {
        var formTheme = createTheme(11, "Form Theme", null, null);
        var managingTheme = createTheme(31, "Managing Theme", null, UUID.randomUUID());
        var fixture = createFixture(
                baseFormLayout()
                        .setManagingDepartmentId(300)
        );
        fixture.processVersion().setThemeId(formTheme.getId());

        when(fixture.themeService().retrieve(formTheme.getId())).thenReturn(Optional.of(formTheme));
        when(fixture.departmentService().retrieve(300)).thenReturn(Optional.of(new VDepartmentShadowedEntity().setId(300).setThemeId(managingTheme.getId())));
        when(fixture.themeService().retrieve(managingTheme.getId())).thenReturn(Optional.of(managingTheme));

        var response = new MockHttpServletResponse();
        fixture.controller().getFavicon(null, fixture.processSlug(), fixture.formSlug(), null, null, response);

        assertEquals("https://assets.example/" + managingTheme.getFaviconKey(), response.getRedirectedUrl());
    }

    @Test
    void getFaviconShouldFallbackToDefaultFaviconWhenNoThemeProvidesOne() throws Exception {
        var fixture = createFixture(baseFormLayout());

        var response = new MockHttpServletResponse();
        fixture.controller().getFavicon(null, fixture.processSlug(), fixture.formSlug(), null, null, response);

        assertEquals("https://prosuna.example/assets/default-favicon.ico", response.getRedirectedUrl());
    }

    @Test
    void getPrintShouldStreamSubmittedSummaryPdf() throws Exception {
        var fixture = createPrintFixture(true);
        var pdfBytes = new byte[]{37, 80, 68, 70};
        when(fixture.storageService().getDocumentContent(7, "/summary.pdf"))
                .thenReturn(new ByteArrayInputStream(pdfBytes));

        var response = new MockHttpServletResponse();
        fixture.controller().getPrint(
                null,
                fixture.processSlug(),
                fixture.formSlug(),
                fixture.instanceAccessKey(),
                fixture.taskAccessKey(),
                null,
                response
        );

        assertEquals("application/pdf", response.getContentType());
        assertArrayEquals(pdfBytes, response.getContentAsByteArray());
        assertTrue(response.getHeader("Content-Disposition").contains("summary.pdf"));
    }

    @Test
    void getPrintShouldRejectWrongFormSlug() throws Exception {
        var fixture = createPrintFixture(true);
        var response = new MockHttpServletResponse();

        var error = assertThrows(ResponseException.class, () -> fixture.controller().getPrint(
                null,
                fixture.processSlug(),
                "other-form",
                fixture.instanceAccessKey(),
                fixture.taskAccessKey(),
                null,
                response
        ));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
        verify(fixture.storageService(), never()).getDocumentContent(anyInt(), anyString());
    }

    @Test
    void getPrintShouldReturnNotFoundWhenSummaryAttachmentIsMissing() throws Exception {
        var fixture = createPrintFixture(false);
        var response = new MockHttpServletResponse();

        var error = assertThrows(ResponseException.class, () -> fixture.controller().getPrint(
                null,
                fixture.processSlug(),
                fixture.formSlug(),
                fixture.instanceAccessKey(),
                fixture.taskAccessKey(),
                null,
                response
        ));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
        verify(fixture.storageService(), never()).getDocumentContent(anyInt(), anyString());
    }

    @Test
    void getPaymentConfirmationShouldStreamPdfForPaidTransactionResolvedByRedirectUrl() throws Exception {
        var transaction = createPaymentTransaction(
                XBezahldienstStatus.PAYED,
                "https://gover.example/process/instance-access-key/tasks/task-access-key"
        );
        var fixture = createPrintFixture(false, transaction, false);
        var pdfBytes = new byte[]{37, 80, 68, 70};
        when(fixture.pdfService().generatePaymentConfirmation(
                same(transaction),
                eq("CASE-1"),
                eq("https://gover.example/assets/default-logo.png"),
                any(VDepartmentShadowedEntity.class)
        )).thenReturn(pdfBytes);

        var response = new MockHttpServletResponse();
        fixture.controller().getPaymentConfirmation(
                null,
                fixture.processSlug(),
                fixture.formSlug(),
                fixture.instanceAccessKey(),
                fixture.taskAccessKey(),
                null,
                response
        );

        assertEquals("application/pdf", response.getContentType());
        assertArrayEquals(pdfBytes, response.getContentAsByteArray());
        assertTrue(response.getHeader("Content-Disposition").contains("Zahlungsbestaetigung-CASE-1.pdf"));
        verify(fixture.paymentTransactionService()).retrieveByRedirectUrl(fixture.paymentRedirectUrl());
    }

    @Test
    void getPaymentConfirmationShouldUseRuntimeTransactionKey() throws Exception {
        var transaction = createPaymentTransaction(
                XBezahldienstStatus.PAYED,
                "https://gover.example/process/instance-access-key/tasks/task-access-key"
        );
        var fixture = createPrintFixture(false, transaction, true);
        when(fixture.pdfService().generatePaymentConfirmation(
                any(PaymentTransactionEntity.class),
                anyString(),
                any(),
                any(VDepartmentShadowedEntity.class)
        )).thenReturn(new byte[]{37, 80, 68, 70});

        fixture.controller().getPaymentConfirmation(
                null,
                fixture.processSlug(),
                fixture.formSlug(),
                fixture.instanceAccessKey(),
                fixture.taskAccessKey(),
                null,
                new MockHttpServletResponse()
        );

        verify(fixture.paymentTransactionService()).retrieve(transaction.getKey());
        verify(fixture.paymentTransactionService(), never()).retrieveByRedirectUrl(anyString());
    }

    @Test
    void getPaymentConfirmationShouldRejectUnpaidTransaction() throws Exception {
        var transaction = createPaymentTransaction(
                XBezahldienstStatus.INITIAL,
                "https://gover.example/process/instance-access-key/tasks/task-access-key"
        );
        var fixture = createPrintFixture(false, transaction, false);

        var error = assertThrows(ResponseException.class, () -> fixture.controller().getPaymentConfirmation(
                null,
                fixture.processSlug(),
                fixture.formSlug(),
                fixture.instanceAccessKey(),
                fixture.taskAccessKey(),
                null,
                new MockHttpServletResponse()
        ));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
        verify(fixture.pdfService(), never()).generatePaymentConfirmation(any(), anyString(), any(), any());
    }

    private PrintFixture createPrintFixture(boolean withSummaryAttachment) throws ResponseException {
        return createPrintFixture(withSummaryAttachment, null, false);
    }

    private PrintFixture createPrintFixture(boolean withSummaryAttachment,
                                            PaymentTransactionEntity paymentTransaction,
                                            boolean runtimeDataContainsTransaction) throws ResponseException {
        var processSlug = "example-process";
        var formSlug = "example-form";
        var instanceAccessKey = "instance-access-key";
        var taskAccessKey = "task-access-key";
        var paymentRedirectUrl = "https://gover.example/process/instance-access-key/tasks/task-access-key";

        var process = new ProcessEntity()
                .setId(100)
                .setInternalTitle("Process")
                .setDepartmentId(10)
                .setAccessKey(UUID.randomUUID())
                .setSlug(processSlug)
                .setVersionCount(1)
                .setPublishedVersion(1);

        var instance = new ProcessInstanceEntity()
                .setId(1L)
                .setCaseNumber("CASE-1")
                .setAccessKey(instanceAccessKey)
                .setProcessId(process.getId())
                .setInitialProcessVersion(1);

        var task = new ProcessInstanceTaskEntity()
                .setId(2L)
                .setAccessKey(taskAccessKey)
                .setProcessInstanceId(instance.getId())
                .setProcessId(process.getId())
                .setProcessVersion(1)
                .setProcessNodeId(500)
                .setStatus(ProcessTaskStatus.Completed)
                .setRuntimeData(runtimeDataContainsTransaction && paymentTransaction != null
                        ? Map.of(FormTriggerNodeV1.DATA_KEY_PAYMENT_TRANSACTION_KEY, paymentTransaction.getKey())
                        : Map.of());

        var node = new ProcessNodeEntity()
                .setId(task.getProcessNodeId())
                .setProcessId(process.getId())
                .setProcessVersion(task.getProcessVersion())
                .setDataKey("formSummary")
                .setProcessNodeDefinitionKey("form.form")
                .setProcessNodeDefinitionVersion(1);

        var processService = mock(ProcessService.class);
        when(processService.retrieveBySlugOrHistory(processSlug)).thenReturn(Optional.of(process));

        var processInstanceService = mock(ProcessInstanceService.class);
        when(processInstanceService.retrieveByAccessKey(instanceAccessKey)).thenReturn(Optional.of(instance));

        var processInstanceTaskService = mock(ProcessInstanceTaskService.class);
        when(processInstanceTaskService.retrieveByProcessInstanceIdAndAccessKey(instance.getId(), taskAccessKey))
                .thenReturn(Optional.of(task));

        var processNodeService = mock(ProcessNodeService.class);
        when(processNodeService.retrieve(task.getProcessNodeId())).thenReturn(Optional.of(node));

        var provider = mock(FormTriggerNodeV1.class);
        when(provider.getKey()).thenReturn("form.form");

        var triggerConfig = new FormTriggerConfigV1();
        triggerConfig.formSlug = formSlug;
        triggerConfig.formLayout = baseFormLayout();
        when(processNodeService.deriveConfiguration(eq(node), eq(provider), nullable(UserEntity.class), eq(true)))
                .thenReturn(new ProcessNodeService.ProcessConfigurationDetails<>(
                        triggerConfig,
                        new DerivedRuntimeElementData()
                ));

        var processNodeDefinitionService = mock(ProcessNodeDefinitionService.class);
        when(processNodeDefinitionService.getProcessNodeDefinition(eq(node), eq(FormTriggerNodeV1.class)))
                .thenReturn(Optional.of(provider));

        var userService = mock(UserService.class);
        when(userService.fromJWT(isNull())).thenReturn(Optional.empty());

        var processVersion = new ProcessVersionEntity()
                .setProcessId(node.getProcessId())
                .setProcessVersion(node.getProcessVersion())
                .setStatus(ProcessVersionStatus.Published);
        var processVersionService = mock(ProcessVersionService.class);
        when(processVersionService.retrieve(ProcessVersionEntityId.of(node.getProcessId(), node.getProcessVersion())))
                .thenReturn(Optional.of(processVersion));

        var attachmentSetService = mock(ProcessInstanceAttachmentSetService.class);
        var attachmentService = mock(ProcessInstanceAttachmentService.class);
        if (withSummaryAttachment) {
            var attachmentSet = new ProcessInstanceAttachmentSetEntity()
                    .setId(3)
                    .setName("Formularzusammenfassung.pdf")
                    .setDataKey(node.getDataKey())
                    .setProcessInstanceId(instance.getId())
                    .setProcessInstanceTaskId(task.getId());
            when(attachmentSetService.retrieveLatestByProcessInstanceIdAndTaskIdAndDataKey(
                    instance.getId(),
                    task.getId(),
                    node.getDataKey()
            )).thenReturn(Optional.of(attachmentSet));

            var attachment = new ProcessInstanceAttachmentEntity()
                    .setKey(UUID.randomUUID())
                    .setFileName("summary.pdf")
                    .setOriginalFileName("summary.pdf")
                    .setPosition(1)
                    .setAttachmentSetId(attachmentSet.getId())
                    .setProcessInstanceId(instance.getId())
                    .setProcessInstanceTaskId(task.getId())
                    .setStorageProviderId(7)
                    .setStoragePathFromRoot("/summary.pdf");
            when(attachmentService.findAllByAttachmentSetId(attachmentSet.getId()))
                    .thenReturn(List.of(attachment));
        } else {
            when(attachmentSetService.retrieveLatestByProcessInstanceIdAndTaskIdAndDataKey(
                    instance.getId(),
                    task.getId(),
                    node.getDataKey()
            )).thenReturn(Optional.empty());
        }

        var storageService = mock(StorageService.class);
        var prosunaConfig = mock(ProsunaConfig.class);
        when(prosunaConfig.createUrl(eq("/process/"), eq(instanceAccessKey), eq("tasks"), eq(taskAccessKey)))
                .thenReturn(paymentRedirectUrl);
        when(prosunaConfig.getDefaultLogoUrl()).thenReturn("https://gover.example/assets/default-logo.png");

        var assetService = mock(AssetService.class);
        when(assetService.createUrl(any(UUID.class))).thenAnswer(invocation -> "https://assets.example/" + invocation.getArgument(0, UUID.class));

        var systemService = mock(SystemService.class);
        when(systemService.retrieveDefaultTheme()).thenReturn(createTheme(1, "System Theme", null, null));

        var departmentService = mock(VDepartmentShadowedService.class);
        when(departmentService.retrieve(process.getDepartmentId()))
                .thenReturn(Optional.of(new VDepartmentShadowedEntity()
                        .setId(process.getDepartmentId())
                        .setName("Department")
                        .setPostalAddress("Example street 1")));

        var paymentTransactionService = mock(PaymentTransactionService.class);
        when(paymentTransactionService.retrieveByRedirectUrl(paymentRedirectUrl))
                .thenReturn(Optional.ofNullable(paymentTransaction));
        if (paymentTransaction != null) {
            when(paymentTransactionService.retrieve(paymentTransaction.getKey()))
                    .thenReturn(Optional.of(paymentTransaction));
        }

        var pdfService = mock(PdfService.class);

        var controller = new FormTriggerControllerV1(
                prosunaConfig,
                mock(IdentityProviderService.class),
                mock(ElementDerivationService.class),
                assetService,
                mock(ThemeService.class),
                departmentService,
                systemService,
                userService,
                processService,
                processNodeService,
                mock(ProcessTestClaimService.class),
                processVersionService,
                processNodeDefinitionService,
                mock(SystemConfigService.class),
                mock(StorageProviderService.class),
                mock(CaptchaReplayGuard.class),
                processInstanceService,
                processInstanceTaskService,
                attachmentSetService,
                attachmentService,
                storageService,
                mock(FileUploadMultipartInputService.class),
                mock(ElementDataTransformService.class),
                mock(ProcessNodeExecutionLoggerFactory.class),
                provider,
                mock(IdentityService.class),
                mock(PaymentPayloadCreationService.class),
                paymentTransactionService,
                mock(PaymentProviderRepository.class),
                pdfService,
                mock(PaymentProviderDefinitionsService.class)
        );

        return new PrintFixture(
                controller,
                processSlug,
                formSlug,
                instanceAccessKey,
                taskAccessKey,
                storageService,
                paymentTransactionService,
                pdfService,
                paymentRedirectUrl
        );
    }

    private TestFixture createFixture(FormLayoutElement formLayout) throws Exception {
        return createFixture(formLayout, null);
    }

    private TestFixture createFixture(FormLayoutElement formLayout,
                                      String testClaimAccessKey) throws Exception {
        return createFixture(formLayout, testClaimAccessKey, testClaimAccessKey != null ? 2 : 1);
    }

    private TestFixture createFixture(FormLayoutElement formLayout,
                                      String testClaimAccessKey,
                                      int resolvedProcessVersion) throws Exception {
        var processAccessKey = UUID.randomUUID();
        var processSlug = "example-process";
        var formSlug = "example-form";

        var prosunaConfig = mock(ProsunaConfig.class);
        when(prosunaConfig.getDefaultLogoUrl()).thenReturn("https://prosuna.example/assets/default-logo.png");
        when(prosunaConfig.getDefaultFaviconUrl()).thenReturn("https://prosuna.example/assets/default-favicon.ico");

        var assetService = mock(AssetService.class);
        when(assetService.createUrl(any(UUID.class))).thenAnswer(invocation -> "https://assets.example/" + invocation.getArgument(0, UUID.class));

        var themeService = mock(ThemeService.class);
        var departmentService = mock(VDepartmentShadowedService.class);
        var systemService = mock(SystemService.class);
        when(systemService.retrieveDefaultTheme()).thenReturn(createTheme(1, "System Theme", null, null));

        var userService = mock(UserService.class);
        when(userService.fromJWT(isNull())).thenReturn(Optional.empty());
        when(userService.fromJWT(any(Jwt.class))).thenReturn(Optional.of(new UserEntity().setId("staff-user")));

        var process = new ProcessEntity()
                .setId(100)
                .setInternalTitle("Process")
                .setDepartmentId(10)
                .setAccessKey(processAccessKey)
                .setSlug(processSlug)
                .setVersionCount(1)
                .setPublishedVersion(1);

        var processVersion = new ProcessVersionEntity()
                .setProcessId(process.getId())
                .setProcessVersion(resolvedProcessVersion)
                .setStatus(ProcessVersionStatus.Published);

        var node = new ProcessNodeEntity()
                .setId(500)
                .setProcessId(process.getId())
                .setProcessVersion(processVersion.getProcessVersion())
                .setProcessNodeDefinitionKey("form.form")
                .setProcessNodeDefinitionVersion(1);

        var processService = mock(ProcessService.class);
        when(processService.retrieveBySlugOrHistory(processSlug)).thenReturn(Optional.of(process));

        var processTestClaimService = mock(ProcessTestClaimService.class);
        if (testClaimAccessKey != null) {
            when(processTestClaimService.retrieveByAccessKey(process.getId(), testClaimAccessKey))
                    .thenReturn(Optional.of(new ProcessTestClaimEntity()
                            .setProcessId(process.getId())
                            .setProcessVersion(processVersion.getProcessVersion())
                            .setAccessKey(testClaimAccessKey)));
        }

        var processVersionService = mock(ProcessVersionService.class);
        when(processVersionService.retrieve(any(ProcessVersionFilter.class)))
                .thenReturn(Optional.of(processVersion));

        var processNodeService = mock(ProcessNodeService.class);
        when(processNodeService.retrieve(any(ProcessNodeFilter.class)))
                .thenReturn(Optional.of(node));

        var provider = mock(FormTriggerNodeV1.class);
        when(provider.getKey()).thenReturn("form.form");

        var triggerConfig = new FormTriggerConfigV1();
        triggerConfig.formSlug = formSlug;
        triggerConfig.formLayout = formLayout;

        when(processNodeService.deriveConfiguration(eq(node), eq(provider), nullable(UserEntity.class), eq(true)))
                .thenReturn(new ProcessNodeService.ProcessConfigurationDetails<>(
                        triggerConfig,
                        new DerivedRuntimeElementData()
                ));

        var processNodeDefinitionService = mock(ProcessNodeDefinitionService.class);
        when(processNodeDefinitionService.getProcessNodeDefinition(eq(node), eq(FormTriggerNodeV1.class)))
                .thenReturn(Optional.of(provider));

        var controller = new FormTriggerControllerV1(
                prosunaConfig,
                mock(IdentityProviderService.class),
                mock(ElementDerivationService.class),
                assetService,
                themeService,
                departmentService,
                systemService,
                userService,
                processService,
                processNodeService,
                processTestClaimService,
                processVersionService,
                processNodeDefinitionService,
                mock(SystemConfigService.class),
                mock(StorageProviderService.class),
                mock(CaptchaReplayGuard.class),
                mock(ProcessInstanceService.class),
                mock(ProcessInstanceTaskService.class),
                mock(ProcessInstanceAttachmentSetService.class),
                mock(ProcessInstanceAttachmentService.class),
                mock(StorageService.class),
                mock(FileUploadMultipartInputService.class),
                mock(ElementDataTransformService.class),
                mock(ProcessNodeExecutionLoggerFactory.class),
                provider,
                mock(IdentityService.class),
                mock(PaymentPayloadCreationService.class),
                mock(PaymentTransactionService.class),
                mock(PaymentProviderRepository.class),
                mock(PdfService.class),
                mock(PaymentProviderDefinitionsService.class)
        );

        return new TestFixture(
                controller,
                processSlug,
                formSlug,
                process,
                processVersion,
                processTestClaimService,
                processVersionService,
                themeService,
                departmentService
        );
    }

    private FormLayoutElement baseFormLayout() {
        return new FormLayoutElement()
                .setPublicTitle("Example form");
    }

    private ThemeEntity createTheme(Integer id,
                                    String name,
                                    UUID logoKey,
                                    UUID faviconKey) {
        return new ThemeEntity(
                id,
                name,
                "#111111",
                "#222222",
                null,
                null,
                logoKey,
                null,
                faviconKey
        );
    }

    private PaymentTransactionEntity createPaymentTransaction(XBezahldienstStatus status,
                                                              String redirectUrl) {
        var request = new XBezahldienstePaymentRequest();
        request.setGrosAmount(BigDecimal.valueOf(12.34));

        var information = new XBezahldienstePaymentInformation();
        information.setStatus(status);
        information.setTransactionId("TX-123");
        information.setTransactionTimestamp("2026-08-17T10:00:00.000Z");

        return new PaymentTransactionEntity()
                .setKey("tx-key")
                .setPaymentProviderKey(UUID.randomUUID())
                .setPaymentRequest(request)
                .setPaymentInformation(information)
                .setRedirectUrl(redirectUrl);
    }

    private record TestFixture(
            FormTriggerControllerV1 controller,
            String processSlug,
            String formSlug,
            ProcessEntity process,
            ProcessVersionEntity processVersion,
            ProcessTestClaimService processTestClaimService,
            ProcessVersionService processVersionService,
            ThemeService themeService,
            VDepartmentShadowedService departmentService
    ) {
    }

    private record PrintFixture(
            FormTriggerControllerV1 controller,
            String processSlug,
            String formSlug,
            String instanceAccessKey,
            String taskAccessKey,
            StorageService storageService,
            PaymentTransactionService paymentTransactionService,
            PdfService pdfService,
            String paymentRedirectUrl
    ) {
    }
}
