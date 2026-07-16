package de.aivot.gover.backend.models.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakConfig {
    private String hostname;
    private String internalHostname;
    private String realm;
    private String frontendClientId;
    private String frontendClientSecret;
    private String backendClientId;
    private String backendClientSecret;

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getInternalHostname() {
        return internalHostname;
    }

    public KeycloakConfig setInternalHostname(String internalHostname) {
        this.internalHostname = internalHostname;
        return this;
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

    public String getFrontendClientSecret() {
        return frontendClientSecret;
    }

    public KeycloakConfig setFrontendClientSecret(String frontendClientSecret) {
        this.frontendClientSecret = frontendClientSecret;
        return this;
    }
}
