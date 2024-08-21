package de.aivot.GoverBackend.models.config;

import de.aivot.GoverBackend.utils.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "storage")
public class StorageConfig {
    private String remoteEndpoint;
    private String remoteBucket;
    private String remoteAccessKey;
    private String remoteSecretKey;

    private String localStoragePath;

    public boolean localStorageEnabled() {
        return !remoteStorageEnabled();
    }

    public boolean remoteStorageEnabled() {
        return StringUtils.isNotNullOrEmpty(remoteEndpoint);
    }

    public String getRemoteEndpoint() {
        return remoteEndpoint;
    }

    public void setRemoteEndpoint(String remoteEndpoint) {
        this.remoteEndpoint = remoteEndpoint;
    }

    public String getRemoteAccessKey() {
        return remoteAccessKey;
    }

    public void setRemoteAccessKey(String remoteAccessKey) {
        this.remoteAccessKey = remoteAccessKey;
    }

    public String getRemoteSecretKey() {
        return remoteSecretKey;
    }

    public void setRemoteSecretKey(String remoteSecretKey) {
        this.remoteSecretKey = remoteSecretKey;
    }

    public String getRemoteBucket() {
        return remoteBucket;
    }

    public void setRemoteBucket(String remoteBucket) {
        this.remoteBucket = remoteBucket;
    }

    /**
     * Returns a normalized path without trailing slashes
     * @return the local storage path without trailing slashes
     */
    public String getLocalStoragePath() {
        if (localStoragePath == null) {
            return ".";
        } else if (localStoragePath.endsWith("/")) {
            return localStoragePath.substring(0, localStoragePath.length() - 1);
        }
        return localStoragePath;
    }

    public void setLocalStoragePath(String localStoragePath) {
        this.localStoragePath = localStoragePath;
    }

    public Map<String, Object> toMap() {
        return Map.of(
                "localStorageEnabled", localStorageEnabled(),
                "remoteStorageEnabled", remoteStorageEnabled(),
                "remoteEndpoint", remoteEndpoint,
                "remoteBucket", remoteBucket,
                "remoteAccessKey", StringUtils.obfuscateKey(remoteAccessKey),
                "remoteSecretKey", StringUtils.obfuscateKey(remoteSecretKey),
                "localStoragePath", localStoragePath
        );
    }
}
