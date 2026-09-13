package de.aivot.prosuna.backend.theme.dtos;

import de.aivot.prosuna.backend.asset.services.AssetService;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.theme.entities.ThemeEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record ResolvedThemeDTO(
        @Nonnull String primaryColor,
        @Nonnull String secondaryColor,
        @Nullable String primaryColorDark,
        @Nullable String secondaryColorDark,
        @Nonnull String logoUrl,
        @Nonnull String logoUrlDark,
        @Nonnull String faviconUrl
) {
    @Nonnull
    public static ResolvedThemeDTO fromResolvedTheme(@Nonnull ThemeEntity theme,
                                                     @Nonnull AssetService assetService,
                                                     @Nonnull ProsunaConfig prosunaConfig) {
        var logoUrl = theme.getLogoKey() == null
                ? prosunaConfig.getDefaultLogoUrl()
                : assetService.createUrl(theme.getLogoKey());
        var logoUrlDark = theme.getLogoKeyDark() == null
                ? logoUrl
                : assetService.createUrl(theme.getLogoKeyDark());
        var faviconUrl = theme.getFaviconKey() == null
                ? prosunaConfig.getDefaultFaviconUrl()
                : assetService.createUrl(theme.getFaviconKey());

        return new ResolvedThemeDTO(
                theme.getPrimaryColor(),
                theme.getSecondaryColor(),
                theme.getPrimaryColorDark(),
                theme.getSecondaryColorDark(),
                logoUrl,
                logoUrlDark,
                faviconUrl
        );
    }
}
