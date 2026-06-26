package de.aivot.gover.backend.plugins.core.v1.operators.user;

import de.aivot.gover.backend.nocode.exceptions.NoCodeException;
import de.aivot.gover.backend.plugins.core.v1.operators.CommonOperatorsV1;
import de.aivot.gover.backend.secrets.services.SecretService;
import de.aivot.gover.backend.user.entities.UserEntity;
import de.aivot.gover.backend.user.repositories.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Optional;

import static de.aivot.gover.backend.TestData.runtime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NoCodeUserFullNameOperatorTest {
    @Test
    void shouldResolveFullNameByUserId() throws NoCodeException {
        var userRepository = mock(UserRepository.class);
        when(userRepository.findById("user-1"))
                .thenReturn(Optional.of(new UserEntity()
                        .setId("user-1")
                        .setFullName("Max Mustermann")));

        var operator = new NoCodeUserFullNameOperator(userRepository);

        var result = operator.evaluate(runtime(), "user-1");

        assertEquals("Max Mustermann", result.getValue());
        verify(userRepository).findById("user-1");
    }

    @Test
    void shouldReturnNullForUnknownUserId() throws NoCodeException {
        var userRepository = mock(UserRepository.class);
        when(userRepository.findById("unknown-user"))
                .thenReturn(Optional.empty());

        var operator = new NoCodeUserFullNameOperator(userRepository);

        var result = operator.evaluate(runtime(), "unknown-user");

        assertNull(result.getValue());
        verify(userRepository).findById("unknown-user");
    }

    @Test
    void shouldReturnNullForBlankUserId() throws NoCodeException {
        var userRepository = mock(UserRepository.class);
        var operator = new NoCodeUserFullNameOperator(userRepository);

        var result = operator.evaluate(runtime(), " ");

        assertNull(result.getValue());
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldBeRegisteredInCommonOperators() {
        var userRepository = mock(UserRepository.class);
        var secretService = mock(SecretService.class);
        var operators = new CommonOperatorsV1(userRepository, secretService).getOperators();

        assertTrue(Arrays
                .stream(operators)
                .anyMatch(NoCodeUserFullNameOperator.class::isInstance));
    }
}
