package de.aivot.GoverBackend.system.controllers;

import de.aivot.GoverBackend.asset.services.AssetService;
import de.aivot.GoverBackend.config.services.SystemConfigService;
import de.aivot.GoverBackend.data.SystemConfigKey;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.system.dtos.SystemSetupDTO;
import de.aivot.GoverBackend.system.services.SystemService;
import de.aivot.GoverBackend.theme.dtos.ThemeResponseDTO;
import de.aivot.GoverBackend.theme.entities.ThemeEntity;
import de.aivot.GoverBackend.theme.services.ThemeService;
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
    private final GoverConfig goverConfig;
    private final SystemConfigService systemConfigService;
    private final ThemeService themeService;
    private final AssetService assetService;
    private final SystemService systemService;

    @Autowired
    public SystemController(
            GoverConfig goverConfig,
            SystemConfigService systemConfigService, ThemeService themeService, AssetService assetService, SystemService systemService) {
        this.goverConfig = goverConfig;
        this.systemConfigService = systemConfigService;
        this.themeService = themeService;
        this.assetService = assetService;
        this.systemService = systemService;
    }

    @GetMapping("file-extensions/")
    public List<String> getFileExtensions() {
        return goverConfig.getFileExtensions();
    }

    @GetMapping("favicon/")
    public void getFavicon(
            HttpServletResponse response
    ) throws IOException, ResponseException {
        var theme = getSystemTheme();

        String redirectUrl;
        if (theme.getFaviconKey() != null) {
            redirectUrl = goverConfig.getDefaultFaviconUrl();
        } else {
            redirectUrl = assetService.createUrl(theme.getFaviconKey());
        }

        response.sendRedirect(redirectUrl);
    }

    @GetMapping("logo/")
    public void getLogo(
            HttpServletResponse response
    ) throws IOException, ResponseException {
        var theme = getSystemTheme();

        String redirectUrl;
        if (theme.getLogoKey() != null) {
            redirectUrl = goverConfig.getDefaultLogoUrl();
        } else {
            redirectUrl = assetService.createUrl(theme.getLogoKey());
        }

        response.sendRedirect(redirectUrl);
    }

    @GetMapping("setup/")
    public SystemSetupDTO getSelectedTheme() throws ResponseException {
        var providerName = getProviderName();
        var systemTheme = getSystemTheme();

        return new SystemSetupDTO(
                providerName,
                ThemeResponseDTO.fromEntity(systemTheme)
        );
    }

    @Nullable
    public String getProviderName() {
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

    @Nonnull
    private ThemeEntity getSystemTheme() throws ResponseException {
        return systemService
                .retrieveDefaultTheme();
    }
}
