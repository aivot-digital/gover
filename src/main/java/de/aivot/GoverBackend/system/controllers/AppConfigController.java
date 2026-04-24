package de.aivot.GoverBackend.system.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.GoverBackend.asset.services.AssetService;
import de.aivot.GoverBackend.config.services.SystemConfigService;
import de.aivot.GoverBackend.core.configs.ProviderNameSystemConfigDefinition;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.data.SystemConfigKey;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.storage.services.KnownExtensionsService;
import de.aivot.GoverBackend.system.dtos.SystemSetupDTO;
import de.aivot.GoverBackend.system.services.SystemService;
import de.aivot.GoverBackend.theme.dtos.ThemeResponseDTO;
import de.aivot.GoverBackend.theme.entities.ThemeEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/public/system/")
public class AppConfigController {
    private final GoverConfig goverConfig;
    private final SystemConfigService systemConfigService;
    private final AssetService assetService;
    private final SystemService systemService;
    private final KnownExtensionsService knownExtensionsService;

    @Value("${keycloak.oidc.hostname}")
    private String oidcIssuerURI;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String oidcClientId;

    @Value("${keycloak.oidc.realm}")
    private String oidcRealm;

    @Autowired
    public AppConfigController(GoverConfig goverConfig,
                               SystemConfigService systemConfigService,
                               AssetService assetService,
                               SystemService systemService,
                               KnownExtensionsService knownExtensionsService) {
        this.goverConfig = goverConfig;
        this.systemConfigService = systemConfigService;
        this.assetService = assetService;
        this.systemService = systemService;
        this.knownExtensionsService = knownExtensionsService;
    }

    private static final String KNOWN_EXTENSIONS_CONFIG_KEY = "knownFileExtensions";
    private static final String PROVIDER_NAME_CONFIG_KEY = "providerName";
    private static final String SYSTEM_THEME_CONFIG_KEY = "systemTheme";
    private static final String PUBLIC_SYSTEM_CONFIGS_CONFIG_KEY = "systemConfigs";
    private static final String FAVICON_URL_CONFIG_KEY = "faviconUrl";
    private static final String LOGO_URL_CONFIG_KEY = "logoUrl";
    private static final String API_HOSTNAME_CONFIG_KEY = "apiHostname";
    private static final String REGISTRY_HOSTNAME_CONFIG_KEY = "registryHostname";
    private static final String SENTRY_DSN = "sentryDsn";

    private static final String OIDC_KEY = "oidc";
    private static final String OIDC_REALM_KEY = "realm";
    private static final String OIDC_HOSTNAME_KEY = "hostname";
    private static final String OIDC_CLIENT_ID_KEY = "clientId";

    @GetMapping("app-config.js")
    public ResponseEntity<String> getAppConfigJs() throws ResponseException {
        var appConfig = new HashMap<String, Object>();

        var systemTheme = getSystemTheme();

        appConfig.put(KNOWN_EXTENSIONS_CONFIG_KEY, knownExtensionsService.getKnownExtensions());
        appConfig.put(PROVIDER_NAME_CONFIG_KEY, getProviderName());
        appConfig.put(SYSTEM_THEME_CONFIG_KEY, ThemeResponseDTO.fromEntity(systemTheme));
        appConfig.put(PUBLIC_SYSTEM_CONFIGS_CONFIG_KEY, systemConfigService.getPublicConfigsAsMap());

        String faviconUrl;
        if (systemTheme.getFaviconKey() == null) {
            faviconUrl = goverConfig.getDefaultFaviconUrl();
        } else {
            faviconUrl = assetService.createUrl(systemTheme.getFaviconKey());
        }
        appConfig.put(FAVICON_URL_CONFIG_KEY, faviconUrl);

        String logoURL;
        if (systemTheme.getLogoKey() == null) {
            logoURL = goverConfig.getDefaultLogoUrl();
        } else {
            logoURL = assetService.createUrl(systemTheme.getLogoKey());
        }
        appConfig.put(LOGO_URL_CONFIG_KEY, logoURL);

        appConfig.put(API_HOSTNAME_CONFIG_KEY, goverConfig.getGoverHostname());
        appConfig.put(REGISTRY_HOSTNAME_CONFIG_KEY, goverConfig.getRegistryHostname());
        appConfig.put(SENTRY_DSN, goverConfig.getSentryWebApp());

        var oidc = new HashMap<String, String>();
        oidc.put(OIDC_HOSTNAME_KEY, oidcIssuerURI);
        oidc.put(OIDC_REALM_KEY, oidcRealm);
        oidc.put(OIDC_CLIENT_ID_KEY, oidcClientId);
        appConfig.put(OIDC_KEY, oidc);

        String configJson;
        try {
            configJson = ObjectMapperFactory
                    .getInstance()
                    .writeValueAsString(appConfig);
        } catch (JsonProcessingException e) {
            throw ResponseException.internalServerError(e);
        }

        var content = "window.AppConfig = " + configJson + ";";

        // Do not declare `produces` on this handler. The global MVC configuration defaults
        // request negotiation to JSON, which would otherwise make this JS endpoint unroutable.
        return ResponseEntity
                .ok()
                .contentType(MediaType.parseMediaType("text/javascript"))
                .body(content);
    }

    @Nullable
    private String getProviderName() {
        String providerName;
        try {
            providerName = systemConfigService
                    .retrieve(ProviderNameSystemConfigDefinition.KEY)
                    .getValueAsString()
                    .orElse(null);
        } catch (ResponseException e) {
            return null;
        }

        return providerName;
    }

    @Nonnull
    private ThemeEntity getSystemTheme() throws ResponseException {
        return systemService
                .retrieveDefaultTheme();
    }
}
