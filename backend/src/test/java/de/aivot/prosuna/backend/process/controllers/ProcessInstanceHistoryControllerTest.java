package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.services.ProcessInstanceHistoryService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProcessInstanceHistoryControllerTest {
    @Test
    void authenticatesAndForwardsTheUserInstanceAndPageTarget() throws Exception {
        var users = mock(UserService.class);
        var history = mock(ProcessInstanceHistoryService.class);
        var controller = new ProcessInstanceHistoryController(users, history);
        var page = PageRequest.of(2, 20);
        when(users.fromJWT(null)).thenReturn(Optional.empty());
        assertThrows(ResponseException.class, () -> controller.getTasks(null, 12, page, "active"));
        verifyNoInteractions(history);
        when(users.fromJWT(null)).thenReturn(Optional.of(new UserEntity().setId("actor")));
        controller.getTasks(null, 12, page, "active");
        verify(history).getTasks("actor", 12, page, "active");
    }
}
