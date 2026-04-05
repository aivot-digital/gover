package de.aivot.GoverBackend.user.configs;

import de.aivot.GoverBackend.config.entities.SystemConfigEntity;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.userRoles.repositories.SystemRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultUserSystemRoleSystemConfigDefinitionTest {
    @Test
    void getDefaultValueShouldReturnStandardEmployeeSystemRoleId() {
        var definition = new DefaultUserSystemRoleSystemConfigDefinition(mock(SystemRoleRepository.class));

        assertEquals(3, definition.getDefaultValue());
    }

    @Test
    void validateShouldAcceptExistingSystemRole() {
        var repository = mock(SystemRoleRepository.class);
        when(repository.existsById(3)).thenReturn(true);

        var definition = new DefaultUserSystemRoleSystemConfigDefinition(repository);
        var entity = new SystemConfigEntity()
                .setKey(DefaultUserSystemRoleSystemConfigDefinition.KEY)
                .setValue("3")
                .setPublicConfig(false);

        assertDoesNotThrow(() -> definition.validate(entity));
    }

    @Test
    void validateShouldRejectNonNumericValue() {
        var definition = new DefaultUserSystemRoleSystemConfigDefinition(mock(SystemRoleRepository.class));
        var entity = new SystemConfigEntity()
                .setKey(DefaultUserSystemRoleSystemConfigDefinition.KEY)
                .setValue("not-a-role-id")
                .setPublicConfig(false);

        var exception = assertThrows(ResponseException.class, () -> definition.validate(entity));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Bitte wählen Sie eine gültige Standard-Systemrolle aus.", exception.getTitle());
    }

    @Test
    void validateShouldRejectUnknownSystemRole() {
        var repository = mock(SystemRoleRepository.class);
        when(repository.existsById(999)).thenReturn(false);

        var definition = new DefaultUserSystemRoleSystemConfigDefinition(repository);
        var entity = new SystemConfigEntity()
                .setKey(DefaultUserSystemRoleSystemConfigDefinition.KEY)
                .setValue("999")
                .setPublicConfig(false);

        var exception = assertThrows(ResponseException.class, () -> definition.validate(entity));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Die ausgewählte Standard-Systemrolle existiert nicht.", exception.getTitle());
    }
}
