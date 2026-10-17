package de.aivot.prosuna.backend.customLink.services;

import de.aivot.prosuna.backend.customLink.entities.CustomLink;
import de.aivot.prosuna.backend.customLink.enums.CustomLinkType;
import de.aivot.prosuna.backend.customLink.repositories.CustomLinkRepository;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomLinkServiceTest {
    @Test
    void createShouldAppendLinkToExistingOrder() {
        var repository = mock(CustomLinkRepository.class);
        var link = new CustomLink().setLabel("Status").setType(CustomLinkType.Dashboard);
        when(repository.getMaximumPosition(CustomLinkType.Dashboard)).thenReturn(3);
        when(repository.save(any(CustomLink.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = new CustomLinkService(repository).create(link);

        assertEquals(4, result.getPosition());
        verify(repository).save(link);
    }

    @Test
    void reorderShouldRejectDuplicateIds() {
        var service = new CustomLinkService(mock(CustomLinkRepository.class));

        var exception = assertThrows(ResponseException.class, () -> service.reorder(CustomLinkType.Dashboard, List.of(1, 1)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void reorderShouldRejectIncompleteOrder() {
        var repository = mock(CustomLinkRepository.class);
        when(repository.findAllByType(CustomLinkType.Dashboard)).thenReturn(List.of(link(1), link(2)));
        var service = new CustomLinkService(repository);

        var exception = assertThrows(ResponseException.class, () -> service.reorder(CustomLinkType.Dashboard, List.of(1)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void reorderShouldAssignConsecutivePositions() throws ResponseException {
        var repository = mock(CustomLinkRepository.class);
        var first = link(1);
        var second = link(2);
        when(repository.findAllByType(CustomLinkType.Dashboard)).thenReturn(List.of(first, second));
        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        new CustomLinkService(repository).reorder(CustomLinkType.Dashboard, List.of(2, 1));

        assertEquals(1, first.getPosition());
        assertEquals(0, second.getPosition());
    }

    private static CustomLink link(int id) {
        return new CustomLink().setId(id).setType(CustomLinkType.Dashboard).setPosition(id - 1);
    }
}
