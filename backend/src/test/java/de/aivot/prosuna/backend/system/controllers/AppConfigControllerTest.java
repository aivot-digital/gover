package de.aivot.prosuna.backend.system.controllers;

import de.aivot.prosuna.backend.asset.services.AssetService;
import de.aivot.prosuna.backend.config.entities.SystemConfigEntity;
import de.aivot.prosuna.backend.config.services.SystemConfigService;
import de.aivot.prosuna.backend.core.configs.ProviderNameSystemConfigDefinition;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.storage.services.KnownExtensionsService;
import de.aivot.prosuna.backend.system.services.SystemService;
import de.aivot.prosuna.backend.theme.entities.ThemeEntity;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppConfigControllerTest {
    @Test
    void getAppConfigJsExposesMissingSystemThemeMediaAsNull() throws Exception {
        var systemService = mock(SystemService.class);
        when(systemService.retrieveDefaultTheme()).thenReturn(new ThemeEntity(
                1,
                "System",
                "#111111",
                "#222222",
                null,
                null,
                null,
                null,
                null
        ));
        var systemConfigService = mock(SystemConfigService.class);
        when(systemConfigService.retrieve(ProviderNameSystemConfigDefinition.KEY)).thenReturn(
                new SystemConfigEntity()
                        .setKey(ProviderNameSystemConfigDefinition.KEY)
                        .setValue("Prosuna")
                        .setPublicConfig(true)
        );

        var controller = new AppConfigController(
                mock(ProsunaConfig.class),
                systemConfigService,
                mock(AssetService.class),
                systemService,
                mock(KnownExtensionsService.class)
        );
        ReflectionTestUtils.setField(controller, "oidcIssuerURI", "https://identity.example");
        ReflectionTestUtils.setField(controller, "oidcClientId", "prosuna");
        ReflectionTestUtils.setField(controller, "oidcRealm", "example");

        var body = controller.getAppConfigJs(null).getBody();

        assertNotNull(body);
        assertTrue(body.contains("\"faviconUrl\":null"));
        assertTrue(body.contains("\"logoUrl\":null"));
        assertTrue(body.contains("\"logoUrlDark\":null"));
    }
}
