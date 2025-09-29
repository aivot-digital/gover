package de.aivot.GoverBackend.system.services;

import de.aivot.GoverBackend.config.repositories.SystemConfigRepository;
import de.aivot.GoverBackend.data.SystemConfigKey;
import de.aivot.GoverBackend.theme.entities.ThemeEntity;
import de.aivot.GoverBackend.theme.repositories.ThemeRepository;
import de.aivot.GoverBackend.theme.services.ThemeService;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SystemService {
    private final SystemConfigRepository systemConfigRepository;
    private final ThemeRepository themeRepository;

    @Autowired
    public SystemService(SystemConfigRepository systemConfigRepository,
                         ThemeRepository themeRepository) {
        this.systemConfigRepository = systemConfigRepository;
        this.themeRepository = themeRepository;
    }

    @Nonnull
    public ThemeEntity retrieveDefaultTheme() {
        var systemConfigThemeId = systemConfigRepository
                .findById(SystemConfigKey.SYSTEM__THEME.getKey())
                .orElse(null);

        if (systemConfigThemeId != null) {
            Integer themeId = null;
            try {
                themeId = Integer.parseInt(systemConfigThemeId.getValue());
            } catch (Exception ignored) {

            }
            if (themeId != null) {
                var systemTheme = themeRepository
                        .findById(themeId)
                        .orElse(null);

                if (systemTheme != null) {
                    return systemTheme;
                }
            }
        }

        return new ThemeEntity(
                0,
                "Standard",
                "#253B5B",
                "#102334",
                "#F8D27C",
                "#CD362D",
                "#B55E06",
                "#1F7894",
                "#378550",
                null,
                null
        );
    }
}
