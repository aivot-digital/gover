package de.aivot.prosuna.backend.user.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.user.entities.UserDeputyEntity;
import de.aivot.prosuna.backend.user.repositories.UserDeputyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

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
    void createThrowsBadRequestWhenUntilDateIsBeforeFromDate() {
        var deputy = createDeputy(
                LocalDate.of(2026, 4, 2),
                LocalDate.of(2026, 4, 1)
        );

        var exception = assertThrows(ResponseException.class, () -> service.create(deputy));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals(
                "Das Ende der Vertretung darf nicht vor dem Start der Vertretung liegen.",
                exception.getTitle()
        );
        verify(repository, never()).save(any(UserDeputyEntity.class));
    }

    @Test
    void createAllowsSingleDayDeputyAssignment() throws Exception {
        var date = LocalDate.of(2026, 4, 1);
        var deputy = createDeputy(date, date);
        when(repository.save(any(UserDeputyEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(deputy);

        verify(repository).save(deputy);
    }

    @Test
    void createSavesUnlimitedDeputyAssignment() throws Exception {
        var deputy = createDeputy(LocalDate.of(2026, 4, 1), null);
        when(repository.save(any(UserDeputyEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(deputy);

        verify(repository).save(deputy);
    }

    @Test
    void performUpdateUpdatesDateRangeAndKeepsDeputyRelation() throws Exception {
        var existingEntity = createDeputy(LocalDate.of(2026, 4, 1), null);
        var updateEntity = createDeputy(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)
        )
                .setOriginalUserId("33333333-3333-3333-3333-333333333333")
                .setDeputyUserId("44444444-4444-4444-4444-444444444444");

        when(repository.save(any(UserDeputyEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.performUpdate(123, updateEntity, existingEntity);

        assertEquals(LocalDate.of(2026, 5, 1), result.getFromDate());
        assertEquals(LocalDate.of(2026, 5, 31), result.getUntilDate());
        assertEquals("11111111-1111-1111-1111-111111111111", result.getOriginalUserId());
        assertEquals("22222222-2222-2222-2222-222222222222", result.getDeputyUserId());
        verify(repository).save(existingEntity);
    }

    private static UserDeputyEntity createDeputy(LocalDate fromDate, LocalDate untilDate) {
        return new UserDeputyEntity()
                .setId(123)
                .setOriginalUserId("11111111-1111-1111-1111-111111111111")
                .setDeputyUserId("22222222-2222-2222-2222-222222222222")
                .setFromDate(fromDate)
                .setUntilDate(untilDate);
    }
}
