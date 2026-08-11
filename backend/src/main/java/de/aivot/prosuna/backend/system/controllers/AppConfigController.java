package de.aivot.prosuna.backend.system.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.prosuna.backend.asset.services.AssetService;
import de.aivot.prosuna.backend.config.services.SystemConfigService;
import de.aivot.prosuna.backend.core.configs.ProviderNameSystemConfigDefinition;
import de.aivot.prosuna.backend.core.services.ObjectMapperFactory;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.storage.services.KnownExtensionsService;
import de.aivot.prosuna.backend.system.services.SystemService;
import de.aivot.prosuna.backend.theme.dtos.ThemeResponseDTO;
import de.aivot.prosuna.backend.theme.entities.ThemeEntity;
import de.aivot.prosuna.backend.utils.ApplicationTimeZone;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@RestController
@RequestMapping("/api/public/system/")
public class AppConfigController {
    private final ProsunaConfig prosunaConfig;
    private final SystemConfigService systemConfigService;
    private final AssetService assetService;
    private final SystemService systemService;
    private final KnownExtensionsService knownExtensionsService;

    @Value("${keycloak.hostname}")
    private String oidcIssuerURI;

    @Value("${keycloak.frontendClientId}")
    private String oidcClientId;

    @Value("${keycloak.realm}")
    private String oidcRealm;

    @Autowired
    public AppConfigController(ProsunaConfig prosunaConfig,
                               SystemConfigService systemConfigService,
                               AssetService assetService,
                               SystemService systemService,
                               KnownExtensionsService knownExtensionsService) {
        this.prosunaConfig = prosunaConfig;
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
    private static final String SUPPORT_URL_CONFIG_KEY = "supportUrl";
    private static final String SENTRY_DSN = "sentryDsn";
    private static final String APPLICATION_TIMEZONE_CONFIG_KEY = "applicationTimeZone";
    private static final String DEPARTMENT_LEVEL_LABELS_CONFIG_KEY = "departmentLevelLabels";
    private static final String MODULE_FLAGS_KEY = "moduleFlags";
    private static final String PROCESS_NODE_LIMITS_KEY = "processNodeLimits";

    private static final String OIDC_KEY = "oidc";
    private static final String OIDC_REALM_KEY = "realm";
    private static final String OIDC_HOSTNAME_KEY = "hostname";
    private static final String OIDC_CLIENT_ID_KEY = "clientId";

    @GetMapping("app-config.js")
    public ResponseEntity<String> getAppConfigJs(
            @Nullable @AuthenticationPrincipal Jwt jwt
    ) throws ResponseException {
        var appConfig = new HashMap<String, Object>();

        var systemTheme = getSystemTheme();

        appConfig.put(KNOWN_EXTENSIONS_CONFIG_KEY, knownExtensionsService.getKnownExtensions());
        appConfig.put(PROVIDER_NAME_CONFIG_KEY, getProviderName());
        appConfig.put(SYSTEM_THEME_CONFIG_KEY, ThemeResponseDTO.fromEntity(systemTheme));

        if (jwt != null) {
            appConfig.put(PUBLIC_SYSTEM_CONFIGS_CONFIG_KEY, systemConfigService.getAllConfigsAsMap());
        } else {
            appConfig.put(PUBLIC_SYSTEM_CONFIGS_CONFIG_KEY, systemConfigService.getPublicConfigsAsMap());
        }


        String faviconUrl;
        if (systemTheme.getFaviconKey() == null) {
            faviconUrl = prosunaConfig.getDefaultFaviconUrl();
        } else {
            faviconUrl = assetService.createUrl(systemTheme.getFaviconKey());
        }
        appConfig.put(FAVICON_URL_CONFIG_KEY, faviconUrl);

        String logoURL;
        if (systemTheme.getLogoKey() == null) {
            logoURL = prosunaConfig.getDefaultLogoUrl();
        } else {
            logoURL = assetService.createUrl(systemTheme.getLogoKey());
        }
        appConfig.put(LOGO_URL_CONFIG_KEY, logoURL);

        appConfig.put(API_HOSTNAME_CONFIG_KEY, prosunaConfig.getProsunaHostname());
        appConfig.put(REGISTRY_HOSTNAME_CONFIG_KEY, prosunaConfig.getRegistryHostname());
        appConfig.put(SUPPORT_URL_CONFIG_KEY, prosunaConfig.getSupportUrl());
        appConfig.put(SENTRY_DSN, prosunaConfig.getSentryWebApp());
        appConfig.put(APPLICATION_TIMEZONE_CONFIG_KEY, ApplicationTimeZone.getZoneIdValue());
        appConfig.put(DEPARTMENT_LEVEL_LABELS_CONFIG_KEY, prosunaConfig.getDepartmentLevelLabels());
        appConfig.put(MODULE_FLAGS_KEY, prosunaConfig.getModuleFlags());
        appConfig.put(PROCESS_NODE_LIMITS_KEY, prosunaConfig.getProcessNodeLimits());

        // TODO: This data should not be required in the fronted because the backend handles the authentication flow
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
