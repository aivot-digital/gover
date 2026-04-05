package de.aivot.GoverBackend.user.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.user.entities.UserDeputyEntity;
import de.aivot.GoverBackend.user.repositories.UserDeputyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserDeputyServiceTest {
    @Mock
    private UserDeputyRepository repository;

    @InjectMocks
    private UserDeputyService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void create_ThrowsBadRequest_WhenUntilTimestampIsNotAfterFromTimestamp() {
        var fromTimestamp = LocalDateTime.of(2026, 4, 1, 0, 0);
        var deputy = createDeputy(fromTimestamp, fromTimestamp);

        var exception = assertThrows(ResponseException.class, () -> service.create(deputy));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Das Ende der Vertretung muss nach dem Start der Vertretung liegen.", exception.getTitle());
        verify(repository, never()).save(any(UserDeputyEntity.class));
    }

    @Test
    void create_SavesDeputy_WhenUntilTimestampIsAfterFromTimestamp() throws Exception {
        var deputy = createDeputy(
                LocalDateTime.of(2026, 4, 1, 0, 0),
                LocalDateTime.of(2026, 4, 2, 0, 0)
        );

        when(repository.save(any(UserDeputyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(deputy);

        verify(repository).save(deputy);
    }

    private static UserDeputyEntity createDeputy(LocalDateTime fromTimestamp, LocalDateTime untilTimestamp) {
        return new UserDeputyEntity()
                .setId(123)
                .setOriginalUserId("11111111-1111-1111-1111-111111111111")
                .setDeputyUserId("22222222-2222-2222-2222-222222222222")
                .setFromTimestamp(fromTimestamp)
                .setUntilTimestamp(untilTimestamp);
    }
}
