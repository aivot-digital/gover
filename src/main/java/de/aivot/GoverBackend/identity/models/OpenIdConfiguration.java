package de.aivot.GoverBackend.identity.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the OpenID Connect configuration for an identity provider.
 *
 * <p>This class is used to deserialize the OpenID Connect configuration
 * retrieved from an identity provider's discovery endpoint. It includes
 * information such as endpoints for authorization, token exchange, user
 * information, and supported scopes.</p>
 *
 * <p>Key functionalities:</p>
 * <ul>
 *     <li>Maps JSON properties from the OpenID Connect discovery document
 *         to Java fields using Jackson annotations.</li>
 *     <li>Provides getter and setter methods for accessing and modifying
 *         the configuration properties.</li>
 *     <li>Supports unknown JSON properties by ignoring them during
 *         deserialization.</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 *     ObjectMapper objectMapper = new ObjectMapper();
 *     OpenIdConfiguration config = objectMapper.readValue(json, OpenIdConfiguration.class);
 *     String authorizationEndpoint = config.getAuthorizationEndpoint();
 * </pre>
 *
 * @see com.fasterxml.jackson.annotation.JsonProperty
 * @see com.fasterxml.jackson.annotation.JsonIgnoreProperties
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenIdConfiguration {
    private String issuer;

    @JsonProperty("authorization_endpoint")
    private String authorizationEndpoint;

    @JsonProperty("token_endpoint")
    private String tokenEndpoint;

    @JsonProperty("userinfo_endpoint")
    private String userinfoEndpoint;

    @JsonProperty("end_session_endpoint")
    private String endSessionEndpoint;

    @JsonProperty("scopes_supported")
    private String[] scopesSupported;

    public String getIssuer() {
        return issuer;
    }

    public OpenIdConfiguration setIssuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public OpenIdConfiguration setAuthorizationEndpoint(String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
        return this;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public OpenIdConfiguration setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
        return this;
    }

    public String getUserinfoEndpoint() {
        return userinfoEndpoint;
    }

    public OpenIdConfiguration setUserinfoEndpoint(String userinfoEndpoint) {
        this.userinfoEndpoint = userinfoEndpoint;
        return this;
    }

    public String getEndSessionEndpoint() {
        return endSessionEndpoint;
    }

    public OpenIdConfiguration setEndSessionEndpoint(String endSessionEndpoint) {
        this.endSessionEndpoint = endSessionEndpoint;
        return this;
    }

    public String[] getScopesSupported() {
        return scopesSupported;
    }

    public OpenIdConfiguration setScopesSupported(String[] scopesSupported) {
        this.scopesSupported = scopesSupported;
        return this;
    }
}
