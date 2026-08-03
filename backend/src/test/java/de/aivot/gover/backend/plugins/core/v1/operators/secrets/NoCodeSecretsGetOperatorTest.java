package de.aivot.gover.backend.plugins.core.v1.operators.secrets;

import de.aivot.gover.backend.core.services.BusinessTime;
import de.aivot.gover.backend.nocode.exceptions.NoCodeException;
import de.aivot.gover.backend.plugins.core.v1.operators.CommonOperatorsV1;
import de.aivot.gover.backend.secrets.entities.SecretEntity;
import de.aivot.gover.backend.secrets.services.SecretService;
import de.aivot.gover.backend.user.repositories.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static de.aivot.gover.backend.TestData.runtime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NoCodeSecretsGetOperatorTest {
    @Test
    void shouldResolveDecryptedSecretByKey() throws Exception {
        var secretService = mock(SecretService.class);
        var secretKey = UUID.randomUUID();
        var secret = new SecretEntity();

        when(secretService.retrieve(secretKey))
                .thenReturn(Optional.of(secret));
        when(secretService.decrypt(secret))
                .thenReturn("decrypted-secret");

        var operator = new NoCodeSecretsGetOperator(secretService);

        var result = operator.evaluate(runtime(), secretKey.toString());

        assertEquals("decrypted-secret", result.getValue());
        verify(secretService).retrieve(secretKey);
        verify(secretService).decrypt(secret);
    }

    @Test
    void shouldReturnNullForUnknownSecretKey() throws NoCodeException {
        var secretService = mock(SecretService.class);
        var secretKey = UUID.randomUUID();

        when(secretService.retrieve(secretKey))
                .thenReturn(Optional.empty());

        var operator = new NoCodeSecretsGetOperator(secretService);

        var result = operator.evaluate(runtime(), secretKey.toString());

        assertNull(result.getValue());
        verify(secretService).retrieve(secretKey);
    }

    @Test
    void shouldReturnNullForBlankSecretKey() throws NoCodeException {
        var secretService = mock(SecretService.class);
        var operator = new NoCodeSecretsGetOperator(secretService);

        var result = operator.evaluate(runtime(), " ");

        assertNull(result.getValue());
        verifyNoInteractions(secretService);
    }

    @Test
    void shouldThrowNoCodeExceptionForInvalidSecretKey() {
        var secretService = mock(SecretService.class);
        var operator = new NoCodeSecretsGetOperator(secretService);

        assertThrows(NoCodeException.class, () -> operator.evaluate(runtime(), "not-a-uuid"));
        verifyNoInteractions(secretService);
    }

    @Test
    void shouldBeRegisteredInCommonOperators() {
        var userRepository = mock(UserRepository.class);
        var secretService = mock(SecretService.class);
        var operators = new CommonOperatorsV1(
                userRepository,
                secretService,
                new BusinessTime(ZoneId.of("Europe/Berlin"), Clock.systemUTC())
        ).getOperators();

        assertTrue(Arrays
                .stream(operators)
                .anyMatch(NoCodeSecretsGetOperator.class::isInstance));
    }
}
