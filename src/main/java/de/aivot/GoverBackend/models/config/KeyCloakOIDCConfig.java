package de.aivot.GoverBackend.models.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "keycloak.oidc")
public class KeyCloakOIDCConfig {
    private String hostname;
    private String realm;
    private String frontendClientId;
    private String backendClientId;
    private String backendClientSecret;

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getFrontendClientId() {
        return frontendClientId;
    }

    public void setFrontendClientId(String frontendClientId) {
        this.frontendClientId = frontendClientId;
    }

    public String getBackendClientId() {
        return backendClientId;
    }

    public void setBackendClientId(String backendClientId) {
        this.backendClientId = backendClientId;
    }

    public String getBackendClientSecret() {
        return backendClientSecret;
    }

    public void setBackendClientSecret(String backendClientSecret) {
        this.backendClientSecret = backendClientSecret;
    }
}
