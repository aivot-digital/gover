package de.aivot.prosuna.backend.theme.services;

import de.aivot.prosuna.backend.department.repositories.DepartmentRepository;
import de.aivot.prosuna.backend.department.repositories.VDepartmentShadowedRepository;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.system.services.SystemService;
import de.aivot.prosuna.backend.asset.repositories.AssetRepository;
import de.aivot.prosuna.backend.theme.entities.ThemeEntity;
import de.aivot.prosuna.backend.theme.repositories.ThemeRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ThemeServiceTest {
    @Test
    void performUpdateShouldAllowRemovingLogoAndFavicon() throws ResponseException {
        var themeRepository = mock(ThemeRepository.class);
        var departmentRepository = mock(DepartmentRepository.class);
        var assetRepository = mock(AssetRepository.class);

        var service = new ThemeService(
                themeRepository,
                departmentRepository,
                assetRepository,
                mock(VDepartmentShadowedRepository.class),
                mock(SystemService.class)
        );

        var existingEntity = new ThemeEntity(
                1,
                "Theme",
                "#253B5B",
                "#5F6368",
                "#8EA9D1",
                "#AEB3B8",
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        var updatedEntity = new ThemeEntity(
                1,
                "Theme aktualisiert",
                "#111111",
                "#222222",
                "#AAAAAA",
                "#BBBBBB",
                null,
                null
        );

        when(themeRepository.save(any(ThemeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var savedEntity = service.performUpdate(1, updatedEntity, existingEntity);

        assertEquals("Theme aktualisiert", savedEntity.getName());
        assertEquals("#111111", savedEntity.getPrimaryColor());
        assertEquals("#222222", savedEntity.getSecondaryColor());
        assertEquals("#AAAAAA", savedEntity.getPrimaryColorDark());
        assertEquals("#BBBBBB", savedEntity.getSecondaryColorDark());
        assertNull(savedEntity.getLogoKey());
        assertNull(savedEntity.getFaviconKey());
        verify(assetRepository, never()).existsById(any());
    }
}
