package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.dtos.ProcessDepartmentOptionDTO;
import de.aivot.prosuna.backend.process.services.ProcessDepartmentOptionService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ProcessDepartmentOptionControllerTest {
    @Test
    void usesTheAuthenticatedUserForOptions() throws Exception {
        var options = mock(ProcessDepartmentOptionService.class);
        var users = mock(UserService.class);
        when(users.fromJWT(null)).thenReturn(Optional.of(new UserEntity().setId("user")));
        var expected = List.of(new ProcessDepartmentOptionDTO(10, "Department"));
        when(options.listForUser("user")).thenReturn(expected);

        assertEquals(expected, new ProcessDepartmentOptionController(options, users).list(null));
        verify(options).listForUser("user");
    }

    @Test
    void rejectsRequestsWithoutAnAuthenticatedUser() throws Exception {
        var options = mock(ProcessDepartmentOptionService.class);
        var users = mock(UserService.class);
        when(users.fromJWT(null)).thenReturn(Optional.empty());

        assertThrows(ResponseException.class, () -> new ProcessDepartmentOptionController(options, users).list(null));
        verifyNoInteractions(options);
    }
}
