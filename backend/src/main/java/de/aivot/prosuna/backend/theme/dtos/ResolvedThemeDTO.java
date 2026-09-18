package de.aivot.prosuna.backend.theme.dtos;

import de.aivot.prosuna.backend.asset.services.AssetService;
import de.aivot.prosuna.backend.theme.entities.ThemeEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record ResolvedThemeDTO(
        @Nonnull String primaryColor,
        @Nonnull String secondaryColor,
        @Nullable String primaryColorDark,
        @Nullable String secondaryColorDark,
        @Nullable String logoUrl,
        @Nullable String logoUrlDark,
        @Nullable String faviconUrl
) {
    @Nonnull
    public static ResolvedThemeDTO fromResolvedTheme(@Nonnull ThemeEntity theme,
                                                     @Nonnull AssetService assetService) {
        var logoUrl = theme.getLogoKey() == null
                ? null
                : assetService.createUrl(theme.getLogoKey());
        var logoUrlDark = theme.getLogoKeyDark() == null
                ? logoUrl
                : assetService.createUrl(theme.getLogoKeyDark());
        var faviconUrl = theme.getFaviconKey() == null
                ? null
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
