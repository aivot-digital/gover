package de.aivot.prosuna.backend.process.configs;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.storage.repositories.StorageProviderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultStorageProcessAttachmentsSystemConfigDefinitionTest {
    @Test
    void validateChangeShouldRejectUnconfirmedChangeWithRunningProcesses() {
        var processInstanceRepository = mock(ProcessInstanceRepository.class);
        var definition = createDefinition(processInstanceRepository);

        when(processInstanceRepository.countAllByStatusIs(ProcessInstanceStatus.Running))
                .thenReturn(2L);

        var exception = assertThrows(
                ResponseException.class,
                () -> definition.validateChange("1", "2", false)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void validateChangeShouldAllowConfirmedChangeWithRunningProcesses() {
        var processInstanceRepository = mock(ProcessInstanceRepository.class);
        var definition = createDefinition(processInstanceRepository);

        assertDoesNotThrow(() -> definition.validateChange("1", "2", true));
        verifyNoInteractions(processInstanceRepository);
    }

    @Test
    void validateChangeShouldIgnoreUnchangedValue() {
        var processInstanceRepository = mock(ProcessInstanceRepository.class);
        var definition = createDefinition(processInstanceRepository);

        assertDoesNotThrow(() -> definition.validateChange("1", "1", false));
        verifyNoInteractions(processInstanceRepository);
    }

    private DefaultStorageProcessAttachmentsSystemConfigDefinition createDefinition(
            ProcessInstanceRepository processInstanceRepository
    ) {
        return new DefaultStorageProcessAttachmentsSystemConfigDefinition(
                mock(StorageProviderRepository.class),
                processInstanceRepository
        );
    }
}
