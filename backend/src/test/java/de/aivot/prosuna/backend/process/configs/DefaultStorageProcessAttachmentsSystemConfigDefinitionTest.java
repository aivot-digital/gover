package de.aivot.prosuna.backend.process.configs;

import de.aivot.prosuna.backend.config.entities.SystemConfigEntity;
import de.aivot.prosuna.backend.config.repositories.SystemConfigRepository;
import de.aivot.prosuna.backend.config.services.SystemConfigService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.storage.repositories.StorageProviderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

    @Test
    void getValueShouldBeUnsetWithoutStoredValue() throws ResponseException {
        var repository = mock(SystemConfigRepository.class);
        when(repository.findById(DefaultStorageProcessAttachmentsSystemConfigDefinition.KEY))
                .thenReturn(Optional.empty());
        var service = new SystemConfigService(repository, List.of(createDefinition(mock(ProcessInstanceRepository.class))));

        assertNull(service.getValue(DefaultStorageProcessAttachmentsSystemConfigDefinition.KEY));
    }

    @Test
    void saveShouldSelectFirstDefaultProviderWithoutStoredValue() throws ResponseException {
        var repository = mock(SystemConfigRepository.class);
        when(repository.findById(DefaultStorageProcessAttachmentsSystemConfigDefinition.KEY))
                .thenReturn(Optional.empty());
        var service = new SystemConfigService(repository, List.of(createDefinition(mock(ProcessInstanceRepository.class))));
        var entity = new SystemConfigEntity()
                .setValue("3");

        service.save(DefaultStorageProcessAttachmentsSystemConfigDefinition.KEY, entity);

        verify(repository).save(entity);
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
