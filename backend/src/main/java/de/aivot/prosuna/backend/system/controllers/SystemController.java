package de.aivot.prosuna.backend.system.controllers;

import de.aivot.prosuna.backend.asset.services.AssetService;
import de.aivot.prosuna.backend.config.services.SystemConfigService;
import de.aivot.prosuna.backend.data.SystemConfigKey;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.system.dtos.SystemSetupDTO;
import de.aivot.prosuna.backend.system.services.SystemService;
import de.aivot.prosuna.backend.theme.dtos.ThemeResponseDTO;
import de.aivot.prosuna.backend.theme.entities.ThemeEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/public/system/")
public class SystemController {
    private final ProsunaConfig prosunaConfig;
    private final SystemConfigService systemConfigService;
    private final AssetService assetService;
    private final SystemService systemService;

    @Autowired
    public SystemController(ProsunaConfig prosunaConfig,
                            SystemConfigService systemConfigService,
                            AssetService assetService,
                            SystemService systemService) {
        this.prosunaConfig = prosunaConfig;
        this.systemConfigService = systemConfigService;
        this.assetService = assetService;
        this.systemService = systemService;
    }

    @Deprecated
    @GetMapping("file-extensions/")
    public List<String> getFileExtensions() {
        return prosunaConfig.getFileExtensions();
    }

    @Deprecated
    @GetMapping("favicon/")
    public void getFavicon(
            HttpServletResponse response
    ) throws IOException, ResponseException {
        var theme = getSystemTheme();

        String redirectUrl;
        if (theme.getFaviconKey() == null) {
            redirectUrl = prosunaConfig.getDefaultFaviconUrl();
        } else {
            redirectUrl = assetService.createUrl(theme.getFaviconKey());
        }

        response.sendRedirect(redirectUrl);
    }

    @Deprecated
    @GetMapping("logo/")
    public void getLogo(
            HttpServletResponse response
    ) throws IOException, ResponseException {
        var theme = getSystemTheme();

        String redirectUrl;
        if (theme.getLogoKey() == null) {
            redirectUrl = prosunaConfig.getDefaultLogoUrl();
        } else {
            redirectUrl = assetService.createUrl(theme.getLogoKey());
        }

        response.sendRedirect(redirectUrl);
    }

    @Deprecated
    @GetMapping("setup/")
    public SystemSetupDTO getSelectedTheme() throws ResponseException {
        var providerName = getProviderName();
        var systemTheme = getSystemTheme();
        var publicConfigs = systemConfigService.getPublicConfigsAsMap();

        return new SystemSetupDTO(
                providerName,
                ThemeResponseDTO.fromEntity(systemTheme),
                publicConfigs
        );
    }

    @Deprecated
    @Nullable
    private String getProviderName() {
        String providerName;
        try {
            providerName = systemConfigService
                    .retrieve(SystemConfigKey.PROVIDER__NAME.getKey())
                    .getValueAsString()
                    .orElse(null);
        } catch (ResponseException e) {
            return null;
        }

        return providerName;
    }

    @Deprecated
    @Nonnull
    private ThemeEntity getSystemTheme() throws ResponseException {
        return systemService
                .retrieveDefaultTheme();
    }
}
