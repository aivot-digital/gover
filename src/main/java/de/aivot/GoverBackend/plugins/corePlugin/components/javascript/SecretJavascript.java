package de.aivot.GoverBackend.plugins.corePlugin.components.javascript;

import de.aivot.GoverBackend.javascript.providers.JavascriptFunctionProvider;
import de.aivot.GoverBackend.plugin.models.PluginComponent;
import de.aivot.GoverBackend.plugins.corePlugin.Core;
import de.aivot.GoverBackend.secrets.services.SecretService;
import jakarta.annotation.Nonnull;
import org.graalvm.polyglot.HostAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * This class provides JavaScript functions for retrieving secrets.
 * The functions are exposed to the JavaScript environment through the GraalVM Polyglot API.
 */
@Component
public class SecretJavascript implements JavascriptFunctionProvider, PluginComponent {
    private final SecretService secretService;

    @Autowired
    public SecretJavascript(SecretService secretService) {
        this.secretService = secretService;
    }

    @Override
    public @Nonnull String getKey() {
        return "secrets";
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return Core.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public String getName() {
        return "Geheimnisse";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Dieses Paket enthält Funktionen für Geheimnisse.";
    }

    @Override
    public String getObjectName() {
        return "_" + getKey();
    }

    @Override
    public String[] getMethodTypeDefinitions() {
        return new String[]{
                "get(key: string): string;"
        };
    }

    /**
     * Retrieves and decrypts a secret by its key.
     *
     * @param key The key of the secret to retrieve.
     * @return The value of the secret.
     */
    @HostAccess.Export
    public String get(String key) {
        UUID parsedKey;
        try {
            parsedKey = UUID.fromString(key);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format", e);
        }

        var secret = secretService
                .retrieve(parsedKey)
                .orElseThrow(() -> new IllegalArgumentException("Secret not found"));
        try {
            return secretService.decrypt(secret);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt secret", e);
        }
    }
}
