package de.aivot.GoverBackend.theme.services;

import de.aivot.GoverBackend.department.repositories.DepartmentRepository;
import de.aivot.GoverBackend.form.repositories.FormVersionRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.theme.entities.ThemeEntity;
import de.aivot.GoverBackend.theme.repositories.ThemeRepository;
import de.aivot.GoverBackend.asset.repositories.AssetRepository;
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
        var formVersionRepository = mock(FormVersionRepository.class);
        var departmentRepository = mock(DepartmentRepository.class);
        var assetRepository = mock(AssetRepository.class);

        var service = new ThemeService(
                themeRepository,
                formVersionRepository,
                departmentRepository,
                assetRepository
        );

        var existingEntity = new ThemeEntity(
                1,
                "Theme",
                "#253B5B",
                "#102334",
                "#F8D27C",
                "#CD362D",
                "#B55E06",
                "#1F7894",
                "#378550",
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        var updatedEntity = new ThemeEntity(
                1,
                "Theme aktualisiert",
                "#111111",
                "#222222",
                "#333333",
                "#444444",
                "#555555",
                "#666666",
                "#777777",
                null,
                null
        );

        when(themeRepository.save(any(ThemeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var savedEntity = service.performUpdate(1, updatedEntity, existingEntity);

        assertEquals("Theme aktualisiert", savedEntity.getName());
        assertNull(savedEntity.getLogoKey());
        assertNull(savedEntity.getFaviconKey());
        verify(assetRepository, never()).existsById(any());
    }
}
