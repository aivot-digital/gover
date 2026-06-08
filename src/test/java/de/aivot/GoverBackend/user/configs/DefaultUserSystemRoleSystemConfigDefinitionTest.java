package de.aivot.GoverBackend.user.configs;

import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.userRoles.entities.SystemRoleEntity;
import de.aivot.GoverBackend.userRoles.repositories.SystemRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultUserSystemRoleSystemConfigDefinitionTest {
    @Test
    void getDefaultValueShouldReturnStandardEmployeeSystemRoleId() {
        var definition = new DefaultUserSystemRoleSystemConfigDefinition(mock(SystemRoleRepository.class));

        assertEquals("3", definition.getDefaultValue());
    }

    @Test
    void getConfigElementShouldReturnSystemRoleOptions() {
        var repository = mock(SystemRoleRepository.class);
        when(repository.findAll()).thenReturn(List.of(
                new SystemRoleEntity().setId(3).setName("Mitarbeiter:in"),
                new SystemRoleEntity().setId(4).setName("Sachbearbeiter:in")
        ));

        var definition = new DefaultUserSystemRoleSystemConfigDefinition(repository);

        var element = assertInstanceOf(SelectInputElement.class, definition.getConfigElement());
        assertEquals(DefaultUserSystemRoleSystemConfigDefinition.KEY, element.getId());
        assertEquals(definition.getLabel(), element.getLabel());
        assertEquals(definition.getDescription(), element.getHint());
        assertEquals(List.of(
                SelectInputElementOption.of("3", "Mitarbeiter:in"),
                SelectInputElementOption.of("4", "Sachbearbeiter:in")
        ), element.getOptions());
    }

    @Test
    void parseValueFromDBShouldAcceptNumericString() throws ResponseException {
        var repository = mock(SystemRoleRepository.class);
        var definition = new DefaultUserSystemRoleSystemConfigDefinition(repository);

        assertEquals("3", definition.parseValueFromDB("3"));
        verifyNoInteractions(repository);
    }

    @Test
    void parseValueFromDBShouldRejectNonNumericValue() {
        var definition = new DefaultUserSystemRoleSystemConfigDefinition(mock(SystemRoleRepository.class));

        var exception = assertThrows(ResponseException.class, () -> definition.parseValueFromDB("not-a-role-id"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        assertEquals("Ungültiger Wert für users.default_system_role: not-a-role-id", exception.getTitle());
    }

    @Test
    void validateShouldNotRejectUnknownSystemRole() {
        var repository = mock(SystemRoleRepository.class);

        var definition = new DefaultUserSystemRoleSystemConfigDefinition(repository);

        assertDoesNotThrow(() -> definition.validate(definition.parseValueFromDB("999")));
        verifyNoInteractions(repository);
    }
}
