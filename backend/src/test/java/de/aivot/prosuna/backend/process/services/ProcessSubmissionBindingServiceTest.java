package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.process.cache.entities.ProcessSubmissionCacheEntity;
import de.aivot.prosuna.backend.process.cache.repositories.ProcessSubmissionCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessSubmissionBindingServiceTest {
    private ProcessSubmissionCacheRepository repository;
    private ProcessSubmissionBindingService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProcessSubmissionCacheRepository.class);
        service = new ProcessSubmissionBindingService(repository);
    }

    @Test
    void bindStoresOnlyAHashOfTheReturnedSecret() {
        var secret = service.bind(42L);

        var bindingCaptor = ArgumentCaptor.forClass(ProcessSubmissionCacheEntity.class);
        verify(repository).save(bindingCaptor.capture());
        // 32 random bytes, Base64 URL encoded.
        assertEquals(43, secret.length());
        assertNotEquals(secret, bindingCaptor.getValue().getId());
        assertEquals(42L, bindingCaptor.getValue().getProcessInstanceId());
    }

    @Test
    void isBoundAcceptsTheSecretOfTheSameInstance() {
        var secret = bindAndStore(42L);

        assertTrue(service.isBound(secret, 42L));
    }

    @Test
    void isBoundRejectsTheSecretOfAnotherInstance() {
        var secret = bindAndStore(42L);

        assertFalse(service.isBound(secret, 43L));
    }

    @Test
    void isBoundRejectsUnknownSecrets() {
        when(repository.findById(any())).thenReturn(Optional.empty());

        assertFalse(service.isBound("unknown-secret", 42L));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void isBoundRejectsMissingSecretsWithoutLookup(String secret) {
        assertFalse(service.isBound(secret, 42L));
        verify(repository, never()).findById(any());
    }

    private String bindAndStore(Long processInstanceId) {
        var secret = service.bind(processInstanceId);
        var bindingCaptor = ArgumentCaptor.forClass(ProcessSubmissionCacheEntity.class);
        verify(repository).save(bindingCaptor.capture());
        var binding = bindingCaptor.getValue();
        when(repository.findById(binding.getId())).thenReturn(Optional.of(binding));
        return secret;
    }
}
