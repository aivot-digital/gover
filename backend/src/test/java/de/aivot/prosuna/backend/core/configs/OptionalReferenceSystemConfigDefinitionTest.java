package de.aivot.prosuna.backend.core.configs;

import de.aivot.prosuna.backend.config.entities.SystemConfigEntity;
import de.aivot.prosuna.backend.config.models.SystemConfigDefinition;
import de.aivot.prosuna.backend.config.repositories.SystemConfigRepository;
import de.aivot.prosuna.backend.config.services.SystemConfigService;
import de.aivot.prosuna.backend.department.repositories.VDepartmentShadowedRepository;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.theme.repositories.ThemeRepository;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OptionalReferenceSystemConfigDefinitionTest {
    private static Stream<Named<SystemConfigDefinition<String>>> optionalReferenceDefinitions() {
        var departmentRepository = mock(VDepartmentShadowedRepository.class);

        return Stream.of(
                Named.of("accessibility department", new ListingPageAccesibilitySystemConfigDefinition(departmentRepository)),
                Named.of("imprint department", new ListingPageImprintSystemConfigDefinition(departmentRepository)),
                Named.of("privacy department", new ListingPagePrivacySystemConfigDefinition(departmentRepository)),
                Named.of("global theme", new GlobalThemeSystemConfigDefinition(mock(ThemeRepository.class)))
        );
    }

    @ParameterizedTest
    @MethodSource("optionalReferenceDefinitions")
    void shouldPreserveEmptyValue(SystemConfigDefinition<String> definition) throws ResponseException {
        assertEquals("", definition.parseValueFromDB(""));
    }

    @ParameterizedTest
    @MethodSource("optionalReferenceDefinitions")
    void shouldParseNumericReference(SystemConfigDefinition<String> definition) throws ResponseException {
        assertEquals("42", definition.parseValueFromDB("42"));
    }

    @ParameterizedTest
    @MethodSource("optionalReferenceDefinitions")
    void shouldRejectNonNumericReference(SystemConfigDefinition<String> definition) {
        assertThrows(ResponseException.class, () -> definition.parseValueFromDB("invalid"));
    }

    @ParameterizedTest
    @MethodSource("optionalReferenceDefinitions")
    void shouldSaveFirstSelection(SystemConfigDefinition<String> definition) throws ResponseException {
        var repository = mock(SystemConfigRepository.class);
        var service = new SystemConfigService(repository, List.of(definition));
        var entity = new SystemConfigEntity().setValue("42");

        when(repository.findById(definition.getKey())).thenReturn(Optional.empty());

        service.save(definition.getKey(), entity, true);

        verify(repository).save(entity);
    }

    @ParameterizedTest
    @MethodSource("optionalReferenceDefinitions")
    void shouldClearSelection(SystemConfigDefinition<String> definition) throws ResponseException {
        var repository = mock(SystemConfigRepository.class);
        var service = new SystemConfigService(repository, List.of(definition));
        var entity = new SystemConfigEntity().setValue("");

        when(repository.findById(definition.getKey())).thenReturn(Optional.of(
                new SystemConfigEntity()
                        .setKey(definition.getKey())
                        .setValue("42")
                        .setPublicConfig(definition.isPublicConfig())
        ));

        service.save(definition.getKey(), entity, true);

        verify(repository).save(entity);
    }
}
