package de.aivot.prosuna.backend.system.services;

import de.aivot.prosuna.backend.config.repositories.SystemConfigRepository;
import de.aivot.prosuna.backend.theme.repositories.ThemeRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemServiceTest {
    @Test
    void retrieveDefaultThemeShouldUseModeSpecificDefaultColors() {
        var systemConfigRepository = mock(SystemConfigRepository.class);
        var themeRepository = mock(ThemeRepository.class);
        when(systemConfigRepository.findById(anyString())).thenReturn(Optional.empty());

        var service = new SystemService(systemConfigRepository, themeRepository);
        var theme = service.retrieveDefaultTheme();

        assertEquals("#733635", theme.getPrimaryColor());
        assertEquals("#A0C9CB", theme.getSecondaryColor());
        assertEquals("#FF613A", theme.getPrimaryColorDark());
        assertEquals("#A0C9CB", theme.getSecondaryColorDark());
    }
}
