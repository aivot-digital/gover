package de.aivot.GoverBackend.secrets.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.secrets.entities.SecretEntity;
import de.aivot.GoverBackend.secrets.properties.SecretConfigurationProperties;
import de.aivot.GoverBackend.secrets.repositories.SecretRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * This service is responsible for handling secrets.
 * Secrets are used to store sensitive information like passwords, API keys, etc.
 * The service provides methods for listing, retrieving, decrypting, saving, and destroying secrets.
 * It also provides methods for encrypting and decrypting secret values.
 * The service is used by the {@link de.aivot.GoverBackend.secrets.controllers.SecretController} to handle API requests related to secrets.
 */
@Service
public class SecretService implements EntityService<SecretEntity, String> {
    private final SecretConfigurationProperties secretConfigurationProperties;
    private final SecretRepository secretRepository;

    // The random number generator used for generating salts
    private static final Random RANDOM = new Random();

    // Constants for encryption and decryption
    private static final int SALT_LENGTH = 16;
    private static final int KEY_LENGTH = 256;
    private static final int IV_LENGTH = 16;
    private static final int ITERATION_COUNT = 65536;

    @Autowired
    public SecretService(
            SecretConfigurationProperties secretConfigurationProperties,
            SecretRepository secretRepository
    ) {
        this.secretConfigurationProperties = secretConfigurationProperties;
        this.secretRepository = secretRepository;
    }

    @Nonnull
    @Override
    public Page<SecretEntity> performList(
            @Nonnull Pageable pageable,
            @Nonnull Specification<SecretEntity> specification,
            Filter<SecretEntity> filter) {
        return secretRepository
                .findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public SecretEntity create(@Nonnull SecretEntity newEntity) throws ResponseException {
        var secretEntity = new SecretEntity();

        secretEntity.setKey(UUID.randomUUID().toString());
        secretEntity.setName(newEntity.getName());
        secretEntity.setDescription(newEntity.getDescription());
        secretEntity.setSalt(createRandomSalt());

        String encryptedValue;
        try {
            encryptedValue = encryptDataAES(
                    newEntity.getValue(),
                    secretConfigurationProperties.getKey(),
                    secretEntity.getSalt()
            );
        } catch (Exception e) {
            throw new ResponseException(HttpStatus.INTERNAL_SERVER_ERROR, "Fehler beim Verschlüsseln des Geheimnisses", e.getMessage(), e);
        }

        secretEntity.setValue(encryptedValue);

        return secretRepository.save(secretEntity);
    }

    private static String createRandomSalt() {
        var saltBytes = new byte[SALT_LENGTH];
        RANDOM.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    @Nonnull
    @Override
    public Optional<SecretEntity> retrieve(
            @Nonnull String id
    ) {
        return secretRepository
                .findById(id);
    }

    @Nonnull
    @Override
    public Optional<SecretEntity> retrieve(@Nonnull Specification<SecretEntity> specification) {
        return secretRepository
                .findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull String id) {
        return secretRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<SecretEntity> specification) {
        return secretRepository.exists(specification);
    }

    /**
     * Decrypts a secret entity.
     * The method returns the decrypted secret entity.
     * The method throws a {@link RuntimeException} if an error occurs during decryption.
     *
     * @param secretEntity The secret entity to decrypt.
     * @return The decrypted secret entity.
     */
    @Nonnull
    public String decrypt(
            @Nonnull SecretEntity secretEntity
    ) throws Exception {
        return decryptDataAES(
                secretEntity.getValue(),
                secretConfigurationProperties.getKey(),
                secretEntity.getSalt()
        );
    }

    @Nonnull
    @Override
    public SecretEntity performUpdate(
            @Nonnull String id,
            @Nonnull SecretEntity updatedEntity,
            @Nonnull SecretEntity originalEntity
    ) throws ResponseException {
        originalEntity.setName(updatedEntity.getName());
        originalEntity.setDescription(updatedEntity.getDescription());

        if (!updatedEntity.getValue().matches("^\\*+$")) {
            originalEntity.setSalt(createRandomSalt());

            String encryptedValue;
            try {
                encryptedValue = encryptDataAES(
                        updatedEntity.getValue(),
                        secretConfigurationProperties.getKey(),
                        originalEntity.getSalt()
                );
            } catch (Exception e) {
                throw new ResponseException(HttpStatus.INTERNAL_SERVER_ERROR, "Fehler beim Verschlüsseln des Geheimnisses", e.getMessage(), e);
            }

            originalEntity.setValue(encryptedValue);
        }

        return secretRepository
                .save(originalEntity);
    }

    @Override
    public void performDelete(@Nonnull SecretEntity entity) throws ResponseException {
        // TODO: Check usage
        secretRepository
                .delete(entity);
    }

    /**
     * Encrypts a string using AES encryption.
     * The method uses a secret key and salt to encrypt the string.
     * The method returns the encrypted string as a Base64 encoded string.
     * The method throws an exception if an error occurs during encryption.
     *
     * @param strToEncrypt The string to encrypt.
     * @param secretKey    The secret key used for encryption.
     * @param salt         The salt used for encryption.
     * @return The encrypted string as a Base64 encoded string.
     * @throws NoSuchAlgorithmException           If the algorithm is not available.
     * @throws InvalidKeySpecException            If the key specification is invalid.
     * @throws NoSuchPaddingException             If the padding is not available.
     * @throws InvalidAlgorithmParameterException If the algorithm parameters are invalid.
     * @throws InvalidKeyException                If the key is invalid.
     * @throws UnsupportedEncodingException       If the encoding is not supported.
     * @throws IllegalBlockSizeException          If the block size is illegal.
     * @throws BadPaddingException                If the padding is bad.
     */
    public static String encryptDataAES(String strToEncrypt, String secretKey, String salt) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException {
        SecureRandom secureRandom = new SecureRandom();
        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);
        IvParameterSpec ivspec = new IvParameterSpec(iv);

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(secretKey.toCharArray(), salt.getBytes(), ITERATION_COUNT, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKeySpec secretKeySpec = new SecretKeySpec(tmp.getEncoded(), "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivspec);

        byte[] cipherText = cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8));
        byte[] encryptedData = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, encryptedData, 0, iv.length);
        System.arraycopy(cipherText, 0, encryptedData, iv.length, cipherText.length);

        return Base64.getEncoder().encodeToString(encryptedData);
    }

    /**
     * Decrypts a string using AES decryption.
     * The method uses a secret key and salt to decrypt the string.
     * The method returns the decrypted string.
     *
     * @param strToDecrypt The string to decrypt.
     * @param secretKey    The secret key used for decryption.
     * @param salt         The salt used for decryption.
     * @return The decrypted string.
     * @throws NoSuchAlgorithmException           If the algorithm is not available.
     * @throws InvalidKeySpecException            If the key specification is invalid.
     * @throws NoSuchPaddingException             If the padding is not available.
     * @throws InvalidAlgorithmParameterException If the algorithm parameters are invalid.
     * @throws InvalidKeyException                If the key is invalid.
     * @throws IllegalBlockSizeException          If the block size is illegal.
     * @throws BadPaddingException                If the padding is bad.
     * @throws UnsupportedEncodingException       If the encoding is not supported.
     */
    public static String decryptDataAES(String strToDecrypt, String secretKey, String salt) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
        byte[] encryptedData = Base64.getDecoder().decode(strToDecrypt);
        byte[] iv = new byte[IV_LENGTH];
        System.arraycopy(encryptedData, 0, iv, 0, iv.length);
        IvParameterSpec ivspec = new IvParameterSpec(iv);

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(secretKey.toCharArray(), salt.getBytes(), ITERATION_COUNT, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKeySpec secretKeySpec = new SecretKeySpec(tmp.getEncoded(), "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivspec);

        byte[] cipherText = new byte[encryptedData.length - 16];
        System.arraycopy(encryptedData, 16, cipherText, 0, cipherText.length);

        byte[] decryptedText = cipher.doFinal(cipherText);
        return new String(decryptedText, StandardCharsets.UTF_8);
    }
}
