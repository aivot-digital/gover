package de.aivot.prosuna.backend.userRoles.configs;

import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.userRoles.entities.SystemRoleEntity;
import de.aivot.prosuna.backend.userRoles.repositories.SystemRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MostPrivilegedSystemRoleSystemConfigDefinitionTest {
    @Test
    void getDefaultValueShouldReturnSuperAdministratorSystemRoleId() {
        var definition = new MostPrivilegedSystemRoleSystemConfigDefinition(mock(SystemRoleRepository.class));

        assertEquals("1", definition.getDefaultValue());
    }

    @Test
    void getConfigElementShouldReturnSystemRoleOptions() {
        var repository = mock(SystemRoleRepository.class);
        when(repository.findAll()).thenReturn(List.of(
                new SystemRoleEntity().setId(1).setName("Superadministrator:in"),
                new SystemRoleEntity().setId(4).setName("Sachbearbeiter:in")
        ));

        var definition = new MostPrivilegedSystemRoleSystemConfigDefinition(repository);

        var element = assertInstanceOf(SelectInputElement.class, definition.getConfigElement());
        assertEquals(MostPrivilegedSystemRoleSystemConfigDefinition.KEY, element.getId());
        assertEquals(definition.getLabel(), element.getLabel());
        assertEquals(definition.getDescription(), element.getHint());
        assertEquals(List.of(
                SelectInputElementOption.of("1", "Superadministrator:in"),
                SelectInputElementOption.of("4", "Sachbearbeiter:in")
        ), element.getOptions());
    }

    @Test
    void parseValueFromDBShouldRejectNonNumericValue() {
        var definition = new MostPrivilegedSystemRoleSystemConfigDefinition(mock(SystemRoleRepository.class));

        var exception = assertThrows(ResponseException.class, () -> definition.parseValueFromDB("not-a-role-id"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        assertEquals("Ungültiger Wert für system_roles.most_privileged_role: not-a-role-id", exception.getTitle());
    }

    @Test
    void validateShouldAcceptExistingSystemRole() {
        var repository = mock(SystemRoleRepository.class);
        when(repository.existsById(1)).thenReturn(true);
        var definition = new MostPrivilegedSystemRoleSystemConfigDefinition(repository);

        assertDoesNotThrow(() -> definition.validate("1"));
    }

    @Test
    void validateShouldRejectUnknownSystemRole() {
        var repository = mock(SystemRoleRepository.class);
        when(repository.existsById(999)).thenReturn(false);
        var definition = new MostPrivilegedSystemRoleSystemConfigDefinition(repository);

        var exception = assertThrows(ResponseException.class, () -> definition.validate("999"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Die ausgewählte Systemrolle existiert nicht.", exception.getTitle());
    }
}
