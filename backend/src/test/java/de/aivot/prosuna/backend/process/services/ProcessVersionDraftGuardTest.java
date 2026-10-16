package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntityId;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.repositories.ProcessTestClaimRepository;
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
        var guard = guardFor(ProcessVersionStatus.Drafted, false);

        assertDoesNotThrow(() -> guard.requireDrafted(12, 5));
    }

    @Test
    void requireDrafted_AllowsDraftsWithTestClaim() {
        // Nodes of a tested draft may still be edited, so the plain draft check ignores test claims.
        var guard = guardFor(ProcessVersionStatus.Drafted, true);

        assertDoesNotThrow(() -> guard.requireDrafted(12, 5));
    }

    @ParameterizedTest
    @EnumSource(value = ProcessVersionStatus.class, names = {"Published", "Revoked"})
    void requireDrafted_RejectsVersionsThatAreNoLongerDrafts(ProcessVersionStatus status) {
        var guard = guardFor(status, false);

        var exception = assertThrows(ResponseException.class, () -> guard.requireDrafted(12, 5));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void requireDrafted_RejectsMissingVersions() {
        var repository = mock(ProcessVersionRepository.class);
        when(repository.findById(any(ProcessVersionEntityId.class))).thenReturn(Optional.empty());
        var guard = new ProcessVersionDraftGuard(repository, mock(ProcessTestClaimRepository.class));

        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseException.class, () -> guard.requireDrafted(12, 5)).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseException.class, () -> guard.requireDrafted(null, 5)).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseException.class, () -> guard.requireDrafted(12, null)).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseException.class, () -> guard.requireStructureEditable(12, 5)).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseException.class, () -> guard.requireStructureEditable(null, 5)).getStatus());
    }

    @Test
    void requireStructureEditable_AllowsDraftsWithoutTestClaim() {
        var guard = guardFor(ProcessVersionStatus.Drafted, false);

        assertDoesNotThrow(() -> guard.requireStructureEditable(12, 5));
    }

    @Test
    void requireStructureEditable_RejectsDraftsWithTestClaim() {
        var guard = guardFor(ProcessVersionStatus.Drafted, true);

        var exception = assertThrows(ResponseException.class, () -> guard.requireStructureEditable(12, 5));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("Im Testmodus können keine Prozesselemente hinzugefügt, ersetzt oder entfernt und keine Verbindungen geändert werden. Beenden Sie den Testmodus, um die Struktur des Prozesses zu ändern.", exception.getMessage());
    }

    @Test
    void requireStructureEditable_OnlyRejectsTheVersionUnderTest() {
        var versionRepository = mock(ProcessVersionRepository.class);
        when(versionRepository.findById(any(ProcessVersionEntityId.class)))
                .thenAnswer(invocation -> {
                    var id = invocation.getArgument(0, ProcessVersionEntityId.class);
                    return Optional.of(new ProcessVersionEntity()
                            .setProcessId(id.getProcessId())
                            .setProcessVersion(id.getProcessVersion())
                            .setStatus(ProcessVersionStatus.Drafted));
                });
        var testClaimRepository = mock(ProcessTestClaimRepository.class);
        when(testClaimRepository.existsByProcessIdAndProcessVersion(12, 5)).thenReturn(true);
        var guard = new ProcessVersionDraftGuard(versionRepository, testClaimRepository);

        assertThrows(ResponseException.class, () -> guard.requireStructureEditable(12, 5));
        assertDoesNotThrow(() -> guard.requireStructureEditable(12, 6));
        assertDoesNotThrow(() -> guard.requireStructureEditable(13, 5));
    }

    @ParameterizedTest
    @EnumSource(value = ProcessVersionStatus.class, names = {"Published", "Revoked"})
    void requireStructureEditable_RejectsVersionsThatAreNoLongerDrafts(ProcessVersionStatus status) {
        var guard = guardFor(status, false);

        var exception = assertThrows(ResponseException.class, () -> guard.requireStructureEditable(12, 5));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    private static ProcessVersionDraftGuard guardFor(ProcessVersionStatus status, boolean testClaimExists) {
        var repository = mock(ProcessVersionRepository.class);
        when(repository.findById(ProcessVersionEntityId.of(12, 5)))
                .thenReturn(Optional.of(new ProcessVersionEntity()
                        .setProcessId(12)
                        .setProcessVersion(5)
                        .setStatus(status)));
        var testClaimRepository = mock(ProcessTestClaimRepository.class);
        when(testClaimRepository.existsByProcessIdAndProcessVersion(12, 5)).thenReturn(testClaimExists);
        return new ProcessVersionDraftGuard(repository, testClaimRepository);
    }
}
