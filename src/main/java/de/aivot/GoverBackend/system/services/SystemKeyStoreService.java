package de.aivot.GoverBackend.system.services;

import de.aivot.GoverBackend.system.properties.SecurityProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Optional;

@Service
public class SystemKeyStoreService {
    private final static String KEYSTORE_TYPE = "PKCS12";

    private final SecurityProperties securityProperties;
    private final KeyStore keyStore;

    @Autowired
    public SystemKeyStoreService(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;

        try {
            this.keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            char[] keyStorePassword = securityProperties
                    .getKeystorePassword()
                    .toCharArray();

            File keystoreFile = new File(securityProperties.getKeystorePath());

            if (!keystoreFile.exists()) {
                this.keyStore.load(null, keyStorePassword);
            } else {
                InputStream inputStream = new FileInputStream(keystoreFile);
                this.keyStore.load(inputStream, keyStorePassword);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize KeyStore", e);
        }
    }

    /**
     * Check if the keystore contains the given alias
     *
     * @param alias the alias to check
     * @return true if the alias exists, false otherwise
     * @throws KeyStoreException if an error occurs while accessing the keystore
     */
    public boolean containsAlias(String alias) throws KeyStoreException {
        return keyStore.containsAlias(alias);
    }

    /**
     * Generate a new AES key and store it in the keystore
     *
     * @param alias    the alias of the key
     * @param password the password to protect the key
     * @return the generated key
     * @throws UnrecoverableKeyException thrown if the key cannot be recovered
     * @throws KeyStoreException         thrown if an error occurs while accessing the keystore
     * @throws NoSuchAlgorithmException  thrown if the algorithm is not supported
     * @throws CertificateException      thrown if an error occurs while saving the keystore
     * @throws IOException               thrown if an I/O error occurs
     */
    public Key generateKey(String alias, char[] password) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        var existing = getKey(alias, password);
        if (existing.isPresent()) {
            return existing.get();
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256); // You can choose 128, 192, or 256 bits
        SecretKey secretKey = keyGenerator.generateKey();
        KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(secretKey);
        KeyStore.ProtectionParameter protectionParameter = new KeyStore.PasswordProtection(password);
        keyStore.setEntry(alias, secretKeyEntry, protectionParameter);

        saveKeyStore();

        return secretKey;
    }

    /**
     * Retrieve a key from the keystore
     *
     * @param alias    the alias of the key
     * @param password the password to access the key
     * @return an Optional containing the key if it exists, or an empty Optional if it does not
     * @throws UnrecoverableKeyException thrown if the key cannot be recovered
     * @throws KeyStoreException         thrown if an error occurs while accessing the keystore
     * @throws NoSuchAlgorithmException  thrown if the algorithm is not supported
     */
    public Optional<Key> getKey(String alias, char[] password) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        if (!containsAlias(alias)) {
            return Optional.empty();
        }

        Key key = keyStore.getKey(alias, password);
        if (key == null) {
            return Optional.empty();
        }

        return Optional.of(key);
    }

    private void saveKeyStore() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        File file = new File(securityProperties.getKeystorePath());
        FileOutputStream fos = new FileOutputStream(file);

        char[] keyStorePassword = securityProperties
                .getKeystorePassword()
                .toCharArray();

        keyStore.store(fos, keyStorePassword);
    }
}
