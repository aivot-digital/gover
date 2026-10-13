package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntityId;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.repositories.ProcessVersionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessVersionDraftGuardTest {
    @Test
    void requireDrafted_AllowsDrafts() {
        var guard = guardFor(ProcessVersionStatus.Drafted);

        assertDoesNotThrow(() -> guard.requireDrafted(12, 5));
    }

    @ParameterizedTest
    @EnumSource(value = ProcessVersionStatus.class, names = {"Published", "Revoked"})
    void requireDrafted_RejectsVersionsThatAreNoLongerDrafts(ProcessVersionStatus status) {
        var guard = guardFor(status);

        var exception = assertThrows(ResponseException.class, () -> guard.requireDrafted(12, 5));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void requireDrafted_RejectsMissingVersions() {
        var repository = mock(ProcessVersionRepository.class);
        when(repository.findById(any(ProcessVersionEntityId.class))).thenReturn(Optional.empty());
        var guard = new ProcessVersionDraftGuard(repository);

        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseException.class, () -> guard.requireDrafted(12, 5)).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseException.class, () -> guard.requireDrafted(null, 5)).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseException.class, () -> guard.requireDrafted(12, null)).getStatus());
    }

    private static ProcessVersionDraftGuard guardFor(ProcessVersionStatus status) {
        var repository = mock(ProcessVersionRepository.class);
        when(repository.findById(ProcessVersionEntityId.of(12, 5)))
                .thenReturn(Optional.of(new ProcessVersionEntity()
                        .setProcessId(12)
                        .setProcessVersion(5)
                        .setStatus(status)));
        return new ProcessVersionDraftGuard(repository);
    }
}
