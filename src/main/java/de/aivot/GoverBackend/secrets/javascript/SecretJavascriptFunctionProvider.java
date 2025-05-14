package de.aivot.GoverBackend.secrets.javascript;

import de.aivot.GoverBackend.javascript.providers.JavascriptFunctionProvider;
import de.aivot.GoverBackend.secrets.services.SecretService;
import org.graalvm.polyglot.HostAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class provides JavaScript functions for retrieving secrets.
 * The functions are exposed to the JavaScript environment through the GraalVM Polyglot API.
 */
@Component
public class SecretJavascriptFunctionProvider implements JavascriptFunctionProvider {
    private final SecretService secretService;

    @Autowired
    public SecretJavascriptFunctionProvider(SecretService secretService) {
        this.secretService = secretService;
    }

    @Override
    public String getPackageName() {
        return "secrets";
    }

    @Override
    public String getLabel() {
        return "Geheimnisse";
    }

    @Override
    public String getDescription() {
        return "Dieses Paket enthält Funktionen für Geheimnisse.";
    }

    /**
     * Retrieves and decrypts a secret by its key.
     *
     * @param key The key of the secret to retrieve.
     * @return The value of the secret.
     */
    @HostAccess.Export
    public String get(String key) {
        var secret = secretService
                .retrieve(key)
                .orElseThrow(() -> new IllegalArgumentException("Secret not found"));
        try {
            return secretService.decrypt(secret);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt secret", e);
        }
    }
}
