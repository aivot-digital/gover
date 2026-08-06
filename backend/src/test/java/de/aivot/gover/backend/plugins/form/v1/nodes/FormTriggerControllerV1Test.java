package de.aivot.gover.backend.plugins.form.v1.nodes;

import de.aivot.gover.backend.asset.services.AssetService;
import de.aivot.gover.backend.captcha.services.CaptchaReplayGuard;
import de.aivot.gover.backend.config.services.SystemConfigService;
import de.aivot.gover.backend.department.entities.VDepartmentShadowedEntity;
import de.aivot.gover.backend.department.services.VDepartmentShadowedService;
import de.aivot.gover.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.gover.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.gover.backend.elements.services.ElementDerivationService;
import de.aivot.gover.backend.identity.services.IdentityProviderService;
import de.aivot.gover.backend.identity.services.IdentityService;
import de.aivot.gover.backend.models.config.GoverConfig;
import de.aivot.gover.backend.payment.repositories.PaymentProviderRepository;
import de.aivot.gover.backend.payment.services.PaymentRequestCreationService;
import de.aivot.gover.backend.process.entities.ProcessEntity;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.entities.ProcessTestClaimEntity;
import de.aivot.gover.backend.process.entities.ProcessVersionEntity;
import de.aivot.gover.backend.process.enums.ProcessVersionStatus;
import de.aivot.gover.backend.process.filters.ProcessVersionFilter;
import de.aivot.gover.backend.process.filters.ProcessNodeFilter;
import de.aivot.gover.backend.process.services.*;
import de.aivot.gover.backend.storage.services.StorageProviderService;
import de.aivot.gover.backend.submission.services.ElementDataTransformService;
import de.aivot.gover.backend.system.services.SystemService;
import de.aivot.gover.backend.theme.entities.ThemeEntity;
import de.aivot.gover.backend.theme.services.ThemeService;
import de.aivot.gover.backend.user.entities.UserEntity;
import de.aivot.gover.backend.user.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.Jwt;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FormTriggerControllerV1Test {
    @Test
    void getThemeShouldUseFormThemeFromDerivedConfiguration() throws Exception {
        var formTheme = createTheme(11, "Form Theme", UUID.randomUUID(), UUID.randomUUID());
        var fixture = createFixture(baseFormLayout().setThemeId(formTheme.getId()));

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
                baseFormLayout().setThemeId(formTheme.getId()),
                testClaimAccessKey,
                requestedProcessVersion
        );

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
                        .setThemeId(formTheme.getId())
                        .setResponsibleDepartmentId(200),
                testClaimAccessKey
        );

        when(fixture.themeService().retrieve(formTheme.getId())).thenReturn(Optional.of(formTheme));
        when(fixture.departmentService().retrieve(200)).thenReturn(Optional.of(new VDepartmentShadowedEntity().setId(200).setThemeId(responsibleTheme.getId())));
        when(fixture.themeService().retrieve(responsibleTheme.getId())).thenReturn(Optional.of(responsibleTheme));

        var response = new MockHttpServletResponse();
        fixture.controller().getLogo(null, fixture.processSlug(), fixture.formSlug(), testClaimAccessKey, null, response);

        assertEquals("https://assets.example/" + responsibleTheme.getLogoKey(), response.getRedirectedUrl());
        verify(fixture.processTestClaimService()).retrieveByAccessKey(fixture.process().getId(), testClaimAccessKey);
    }

    @Test
    void getLogoShouldNotFallbackToDefaultLogoWhenCustomThemeChainProvidesNone() throws Exception {
        var formTheme = createTheme(11, "Form Theme", null, null);
        var fixture = createFixture(baseFormLayout().setThemeId(formTheme.getId()));

        when(fixture.themeService().retrieve(formTheme.getId())).thenReturn(Optional.of(formTheme));

        var response = new MockHttpServletResponse();
        fixture.controller().getLogo(null, fixture.processSlug(), fixture.formSlug(), null, null, response);

        assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());
    }

    @Test
    void getLogoShouldFallbackToDefaultLogoWhenNoCustomThemeIsResolved() throws Exception {
        var fixture = createFixture(baseFormLayout());

        var response = new MockHttpServletResponse();
        fixture.controller().getLogo(null, fixture.processSlug(), fixture.formSlug(), null, null, response);

        assertEquals("https://gover.example/assets/default-logo.png", response.getRedirectedUrl());
    }

    @Test
    void getFaviconShouldFallbackToManagingDepartmentTheme() throws Exception {
        var formTheme = createTheme(11, "Form Theme", null, null);
        var managingTheme = createTheme(31, "Managing Theme", null, UUID.randomUUID());
        var fixture = createFixture(
                baseFormLayout()
                        .setThemeId(formTheme.getId())
                        .setManagingDepartmentId(300)
        );

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

        assertEquals("https://gover.example/assets/default-favicon.ico", response.getRedirectedUrl());
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

        var goverConfig = mock(GoverConfig.class);
        when(goverConfig.getDefaultLogoUrl()).thenReturn("https://gover.example/assets/default-logo.png");
        when(goverConfig.getDefaultFaviconUrl()).thenReturn("https://gover.example/assets/default-favicon.ico");

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
                goverConfig,
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
                mock(FileUploadMultipartInputService.class),
                mock(ElementDataTransformService.class),
                mock(ProcessNodeExecutionLoggerFactory.class),
                provider,
                mock(IdentityService.class),
                mock(PaymentRequestCreationService.class),
                mock(PaymentProviderRepository.class)
        );

        return new TestFixture(
                controller,
                processSlug,
                formSlug,
                process,
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
                "#333333",
                "#444444",
                "#555555",
                "#666666",
                "#777777",
                logoKey,
                faviconKey
        );
    }

    private record TestFixture(
            FormTriggerControllerV1 controller,
            String processSlug,
            String formSlug,
            ProcessEntity process,
            ProcessTestClaimService processTestClaimService,
            ProcessVersionService processVersionService,
            ThemeService themeService,
            VDepartmentShadowedService departmentService
    ) {
    }
}
