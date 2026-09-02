package de.aivot.prosuna.backend.theme.services;

import de.aivot.prosuna.backend.department.repositories.DepartmentRepository;
import de.aivot.prosuna.backend.department.repositories.VDepartmentShadowedRepository;
import de.aivot.prosuna.backend.department.entities.VDepartmentShadowedEntity;
import de.aivot.prosuna.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.system.services.SystemService;
import de.aivot.prosuna.backend.asset.repositories.AssetRepository;
import de.aivot.prosuna.backend.theme.entities.ThemeEntity;
import de.aivot.prosuna.backend.theme.repositories.ThemeRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ThemeServiceTest {
    @Test
    void getFormThemesShouldPreferProcessVersionThenFormDepartmentsThenSystem() throws ResponseException {
        var themeRepository = mock(ThemeRepository.class);
        var departmentRepository = mock(VDepartmentShadowedRepository.class);
        var systemService = mock(SystemService.class);
        var processVersionTheme = new ThemeEntity().setId(1);
        var responsibleTheme = new ThemeEntity().setId(2);
        var managingTheme = new ThemeEntity().setId(3);
        var systemTheme = new ThemeEntity().setId(4);

        when(themeRepository.findById(1)).thenReturn(Optional.of(processVersionTheme));
        when(themeRepository.findById(2)).thenReturn(Optional.of(responsibleTheme));
        when(themeRepository.findById(3)).thenReturn(Optional.of(managingTheme));
        when(departmentRepository.findById(20)).thenReturn(Optional.of(
                new VDepartmentShadowedEntity().setId(20).setThemeId(2)
        ));
        when(departmentRepository.findById(30)).thenReturn(Optional.of(
                new VDepartmentShadowedEntity().setId(30).setThemeId(3)
        ));
        when(systemService.retrieveDefaultTheme()).thenReturn(systemTheme);

        var service = new ThemeService(
                themeRepository,
                mock(DepartmentRepository.class),
                mock(AssetRepository.class),
                departmentRepository,
                systemService
        );

        var result = service.getFormThemesInOrderOfImportance(
                new ProcessVersionEntity().setThemeId(1),
                new FormLayoutElement()
                        .setResponsibleDepartmentId(20)
                        .setManagingDepartmentId(30)
        );

        assertEquals(List.of(processVersionTheme, responsibleTheme, managingTheme, systemTheme), result);
    }

    @Test
    void performDeleteShouldRejectDefaultTheme() {
        var themeRepository = mock(ThemeRepository.class);
        var systemService = mock(SystemService.class);
        var theme = new ThemeEntity().setId(1);

        when(systemService.retrieveDefaultTheme()).thenReturn(new ThemeEntity().setId(1));

        var service = new ThemeService(
                themeRepository,
                mock(DepartmentRepository.class),
                mock(AssetRepository.class),
                mock(VDepartmentShadowedRepository.class),
                systemService
        );

        assertThrows(ResponseException.class, () -> service.performDelete(theme));
        verify(themeRepository, never()).delete(theme);
    }

    @Test
    void performDeleteShouldDeleteUnassignedNonDefaultTheme() throws ResponseException {
        var themeRepository = mock(ThemeRepository.class);
        var departmentRepository = mock(DepartmentRepository.class);
        var systemService = mock(SystemService.class);
        var theme = new ThemeEntity().setId(1);

        when(systemService.retrieveDefaultTheme()).thenReturn(new ThemeEntity().setId(2));

        var service = new ThemeService(
                themeRepository,
                departmentRepository,
                mock(AssetRepository.class),
                mock(VDepartmentShadowedRepository.class),
                systemService
        );

        service.performDelete(theme);

        verify(themeRepository).delete(theme);
    }

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
        assertNull(savedEntity.getLogoKeyDark());
        assertNull(savedEntity.getFaviconKey());
        verify(assetRepository, never()).existsById(any());
    }
}
