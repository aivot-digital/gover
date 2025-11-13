package de.aivot.GoverBackend.user.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.core.exceptions.HttpConnectionException;
import de.aivot.GoverBackend.core.models.HttpServiceHeaders;
import de.aivot.GoverBackend.core.services.HttpService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.KeyCloakOIDCConfig;
import de.aivot.GoverBackend.user.models.KeycloakRoleMapping;
import de.aivot.GoverBackend.user.models.KeycloakRoleMappings;
import de.aivot.GoverBackend.user.models.KeycloakUser;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KeyCloakApiService {
    private final static Logger logger = LoggerFactory.getLogger(KeyCloakApiService.class);

    private final KeyCloakOIDCConfig keyCloakOIDCConfig;
    private final HttpService httpService;

    @Autowired
    public KeyCloakApiService(KeyCloakOIDCConfig keyCloakOIDCConfig, HttpService httpService) {
        this.keyCloakOIDCConfig = keyCloakOIDCConfig;
        this.httpService = httpService;
    }

    public Optional<KeycloakUser> getUser(String userId) throws ResponseException {
        HttpResponse<String> response;
        try {
            response = get("/users/" + userId);
        } catch (HttpConnectionException | IOException | URISyntaxException e) {
            throw ResponseException.internalServerError("Mitarbeiter:in mit der ID " + userId + " konnte nicht geladen werden.", e);
        }

        if (response.statusCode() != 200) {
            if (response.statusCode() == 404) {
                return Optional.empty();
            } else {
                throw ResponseException.internalServerError(
                        "Mitarbeiter:in mit der ID " + userId + " konnte nicht geladen werden.",
                        "Status-Code: " + response.statusCode() + " - " + response.body()
                );
            }
        }

        var mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        KeycloakUser keycloakUser;
        try {
            keycloakUser = mapper
                    .readValue(response.body(), KeycloakUser.class);
        } catch (JsonProcessingException e) {
            throw ResponseException.internalServerError("Die Mitarbeiter:in konnte nicht geladen werden", e);
        }

        return Optional.of(keycloakUser);
    }

    public Collection<KeycloakUser> getUsers() throws ResponseException {
        HttpResponse<String> response;
        try {
            response = get("/users?max=1000");
        } catch (URISyntaxException | IOException | HttpConnectionException e) {
            throw ResponseException.internalServerError("Die Liste der Mitarbeiter:innen konnte nicht geladen werden.", e);
        }

        if (response.statusCode() != 200) {
            throw ResponseException.internalServerError(
                    "Die Liste der Mitarbeiter:innen konnte nicht geladen werden.",
                    "Status-Code: " + response.statusCode() + " - " + response.body()
            );
        }

        var mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Collection<KeycloakUser> keycloakUsers;
        try {
            keycloakUsers = mapper
                    .readerForListOf(KeycloakUser.class)
                    .readValue(response.body());
        } catch (JsonProcessingException e) {
            throw ResponseException.internalServerError("Die Mitarbeiter:in konnte nicht geladen werden", e);
        }

        return keycloakUsers;
    }

    public List<String> getRoles(String userId) throws ResponseException {
        HttpResponse<String> response;
        try {
            response = get("/users/" + userId + "/role-mappings");
        } catch (URISyntaxException | IOException | HttpConnectionException e) {
            throw ResponseException.internalServerError("Liste der Rollen für die Mitarbeiter:in mit der ID " + userId + " konnte nicht geladen werden.", e);
        }

        if (response.statusCode() != 200) {
            if (response.statusCode() == 404) {
                return List.of();
            } else {
                throw ResponseException.internalServerError(
                        "Liste der Rollen für die Mitarbeiter:in mit der ID " + userId + " konnte nicht geladen werden.",
                        "Status-Code: " + response.statusCode() + " - " + response.body()
                );
            }
        }

        var mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        KeycloakRoleMappings roles;
        try {
            roles = mapper
                    .readValue(response.body(), KeycloakRoleMappings.class);
        } catch (JsonProcessingException e) {
            throw ResponseException.internalServerError("Die Liste der Rollen für die Mitarbeiter:in mit der ID " + userId + " konnte nicht geladen werden", e);
        }

        return roles
                .getRealmMappings()
                .stream()
                .map(KeycloakRoleMapping::getName)
                .toList();
    }

    /**
     * Get a resource from the keycloak api
     *
     * @param path the path to the resource
     * @return the response from the api
     * @throws URISyntaxException      if the uri is invalid
     * @throws IOException             if the request fails
     * @throws HttpConnectionException if the request is interrupted
     */
    private HttpResponse<String> get(String path) throws URISyntaxException, IOException, HttpConnectionException {
        var accessToken = getAccessToken();

        var uri = new URI(keyCloakOIDCConfig.getHostname() + "/admin/realms/" + keyCloakOIDCConfig.getRealm() + path);

        logger.info("Starting GET request to Keycloak API at {}", uri);

        var res = httpService
                .get(uri, HttpServiceHeaders
                        .create()
                        .withContentType("application/json")
                        .withAuthorizationBearer(accessToken));

        logger.info("GET request to Keycloak API at {} finished with status code {}", uri, res.statusCode());

        return res;
    }

    private String accessKeyBuffer;
    private LocalDateTime accessKeyBufferExpiry;

    /**
     * Retrieve the access token for the backend from the keycloak server
     *
     * @return the access token
     * @throws URISyntaxException      if the uri is invalid
     * @throws IOException             if the request fails
     * @throws HttpConnectionException if the request is interrupted
     */
    private String getAccessToken() throws URISyntaxException, IOException, HttpConnectionException {
        if (accessKeyBuffer != null && accessKeyBufferExpiry != null && LocalDateTime.now().isBefore(accessKeyBufferExpiry)) {
            return accessKeyBuffer;
        }

        // Create the request body for fetching the access token
        var requestBody = Map.of(
                "grant_type", "client_credentials",
                "client_id", keyCloakOIDCConfig.getBackendClientId(),
                "client_secret", keyCloakOIDCConfig.getBackendClientSecret()
        );

        // Create the uri for the token endpoint
        var uri = new URI(keyCloakOIDCConfig.getHostname() + "/realms/" + keyCloakOIDCConfig.getRealm() + "/protocol/openid-connect/token");

        // Build the request for fetching the access token

        logger.info("Starting POST request to Keycloak Token Endpoint at {}", uri);

        var res = httpService.postFormUrlEncoded(uri, requestBody);

        logger.info("POST request to Keycloak Token Endpoint at {} finished with status code {}", uri, res.statusCode());

        // Check if the request was successful
        if (res.statusCode() != 200) {
            throw new IOException("Failed to get token from Keycloak: " + res.body());
        }

        // Parse the response and extract the access token
        var jobject = new JSONObject(res.body());

        var expirySeconds = jobject.getInt("expires_in");
        accessKeyBufferExpiry = LocalDateTime.now().plusSeconds(expirySeconds - 60); // Refresh the token 60 seconds before it expires
        accessKeyBuffer = jobject.getString("access_token");
        return accessKeyBuffer;
    }
}
