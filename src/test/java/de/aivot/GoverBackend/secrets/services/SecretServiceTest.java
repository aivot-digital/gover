package de.aivot.GoverBackend.secrets.services;

import de.aivot.GoverBackend.secrets.entities.SecretEntity;
import de.aivot.GoverBackend.secrets.properties.SecretConfigurationProperties;
import de.aivot.GoverBackend.secrets.repositories.SecretRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SecretServiceTest {
    private SecretRepository secretRepository;
    private SecretConfigurationProperties secretConfigurationProperties;
    private SecretService secretService;

    @BeforeEach
    void setUp() {
        secretRepository = mock(SecretRepository.class);
        secretConfigurationProperties = mock(SecretConfigurationProperties.class);
        when(secretConfigurationProperties.getKey()).thenReturn("test-secret-key");
        secretService = new SecretService(secretConfigurationProperties, secretRepository);
    }

    @Test
    void testEncryptAndDecryptDataAES() throws Exception {
        String original = "SensitiveData123!";
        String salt = "randomSaltValue";
        String key = "test-secret-key";
        String encrypted = SecretService.encryptDataAES(original, key, salt);
        assertNotNull(encrypted);
        String decrypted = SecretService.decryptDataAES(encrypted, key, salt);
        assertEquals(original, decrypted);
    }

    @Test
    void testEncryptDecryptWithDifferentSalts() throws Exception {
        String original = "AnotherSecret";
        String key = "test-secret-key";
        String salt1 = "saltOne";
        String salt2 = "saltTwo";
        String encrypted1 = SecretService.encryptDataAES(original, key, salt1);
        String encrypted2 = SecretService.encryptDataAES(original, key, salt2);
        assertNotEquals(encrypted1, encrypted2);
        assertEquals(original, SecretService.decryptDataAES(encrypted1, key, salt1));
        assertEquals(original, SecretService.decryptDataAES(encrypted2, key, salt2));
    }

    @Test
    void testDecryptWithWrongKeyOrSaltFails() throws Exception {
        String original = "FailTest";
        String key = "test-secret-key";
        String salt = "rightSalt";
        String encrypted = SecretService.encryptDataAES(original, key, salt);
        assertThrows(Exception.class, () -> SecretService.decryptDataAES(encrypted, "wrong-key", salt));
        assertThrows(Exception.class, () -> SecretService.decryptDataAES(encrypted, key, "wrongSalt"));
    }

    @Test
    void testCreateAndDecryptSecretEntity() throws Exception {
        SecretEntity entity = new SecretEntity();
        entity.setName("Test");
        entity.setDescription("desc");
        entity.setValue("SuperSecret");
        when(secretRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        SecretEntity created = secretService.create(entity);
        assertNotNull(created.getValue());
        assertNotEquals("SuperSecret", created.getValue());
        // Decrypt
        String decrypted = secretService.decrypt(created);
        assertEquals("SuperSecret", decrypted);
    }

    @Test
    void testPerformUpdateKeepsMaskedValue() throws Exception {
        SecretEntity original = new SecretEntity();
        original.setName("Test");
        original.setDescription("desc");
        original.setSalt("salt");
        original.setValue("encrypted");
        SecretEntity updated = new SecretEntity();
        updated.setName("Test");
        updated.setDescription("desc");
        updated.setValue("****");
        when(secretRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        SecretEntity result = secretService.performUpdate(UUID.randomUUID(), updated, original);
        assertEquals("encrypted", result.getValue());
    }

    @Test
    void testPerformUpdateChangesValue() throws Exception {
        SecretEntity original = new SecretEntity();
        original.setName("Test");
        original.setDescription("desc");
        original.setSalt("salt");
        original.setValue("encrypted");
        SecretEntity updated = new SecretEntity();
        updated.setName("Test");
        updated.setDescription("desc");
        updated.setValue("newSecret");
        when(secretRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        SecretEntity result = secretService.performUpdate(UUID.randomUUID(), updated, original);
        assertNotEquals("encrypted", result.getValue());
        // Should be decryptable
        String decrypted = secretService.decrypt(result);
        assertEquals("newSecret", decrypted);
    }

    @Test
    void testPerformDelete() {
        SecretEntity entity = new SecretEntity();
        doNothing().when(secretRepository).delete(entity);
        assertDoesNotThrow(() -> secretService.performDelete(entity));
        verify(secretRepository, times(1)).delete(entity);
    }
}

