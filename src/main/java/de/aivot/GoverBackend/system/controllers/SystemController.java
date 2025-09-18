package de.aivot.GoverBackend.system.controllers;

import de.aivot.GoverBackend.config.services.SystemConfigService;
import de.aivot.GoverBackend.data.SystemConfigKey;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.system.dtos.SystemSetupDTO;
import de.aivot.GoverBackend.theme.entities.Theme;
import de.aivot.GoverBackend.theme.services.ThemeService;
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

    @Autowired
    public SystemController(
            GoverConfig goverConfig,
            SystemConfigService systemConfigService, ThemeService themeService) {
        this.goverConfig = goverConfig;
        this.systemConfigService = systemConfigService;
        this.themeService = themeService;
    }

    @GetMapping("file-extensions/")
    public List<String> getFileExtensions() {
        return goverConfig.getFileExtensions();
    }

    @GetMapping("favicon/")
    public void getFavicon(
            HttpServletResponse response
    ) throws IOException {
        String faviconAssetKey;
        try {
            faviconAssetKey = systemConfigService
                    .retrieve(SystemConfigKey.SYSTEM__FAVICON.getKey())
                    .getValueAsString()
                    .orElse(null);
        } catch (ResponseException e) {
            return;
        }

        if (faviconAssetKey == null) {
            return;
        }

        var targetUrl = goverConfig.createUrl("/api/public/assets/" + faviconAssetKey);

        response.sendRedirect(targetUrl);
    }

    @GetMapping("logo/")
    public void getLogo(
            HttpServletResponse response
    ) throws IOException {
        String faviconAssetKey;
        try {
            faviconAssetKey = systemConfigService
                    .retrieve(SystemConfigKey.SYSTEM__LOGO.getKey())
                    .getValueAsString()
                    .orElse(null);
        } catch (ResponseException e) {
            return;
        }

        if (faviconAssetKey == null) {
            return;
        }

        var targetUrl = goverConfig.createUrl("/api/public/assets/" + faviconAssetKey);

        response.sendRedirect(targetUrl);
    }

    @GetMapping("setup/")
    public SystemSetupDTO getSelectedTheme() {
        var providerName = getProviderName();
        var systemTheme = getSystemTheme();

        return new SystemSetupDTO(
                providerName,
                systemTheme
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

    @Nullable
    private Theme getSystemTheme() {
        Integer themeId;
        try {
            themeId = systemConfigService
                    .retrieve(SystemConfigKey.SYSTEM__THEME.getKey())
                    .getValueAsInteger()
                    .orElse(null);
        } catch (ResponseException e) {
            return null;
        }

        if (themeId == null) {
            return null;
        }

        return themeService
                .retrieve(themeId)
                .orElse(null);
    }
}
