package de.aivot.GoverBackend.user.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.core.exceptions.HttpConnectionException;
import de.aivot.GoverBackend.core.models.HttpServiceHeaders;
import de.aivot.GoverBackend.core.services.HttpService;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.KeyCloakOIDCConfig;
import de.aivot.GoverBackend.user.models.KeycloakUser;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.Collection;
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

    public KeycloakUser createUser(KeycloakUser userToCreate) throws ResponseException {
        // Normalize the user object before creating
        userToCreate.setId(null);
        userToCreate.setEmailVerified(false);

        var mapper = ObjectMapperFactory
                .getInstance();

        String keycloakUserJson;
        try {
            keycloakUserJson = mapper.writeValueAsString(userToCreate);
        } catch (JsonProcessingException e) {
            throw ResponseException.internalServerError("Die Mitarbeiter:in konnte nicht erstellt werden", e);
        }

        String accessToken;
        try {
            accessToken = getAccessToken();
        } catch (URISyntaxException | IOException | HttpConnectionException e) {
            throw ResponseException
                    .internalServerError("Die Mitarbeiter:in konnte nicht erstellt werden, da keine Anmeldung am Keycloak möglich war.", e);
        }

        URI uri;
        try {
            uri = new URI(keyCloakOIDCConfig.getHostname() + "/admin/realms/" + keyCloakOIDCConfig.getRealm() + "/users");
        } catch (URISyntaxException e) {
            throw ResponseException
                    .internalServerError("Die Mitarbeiter:in konnte nicht erstellt werden, da die Keycloak-URL ungültig ist.", e);
        }

        ResponseEntity<Void> response;
        try {
            response = httpService
                    .postEntity(
                            uri,
                            keycloakUserJson,
                            HttpServiceHeaders
                                    .create()
                                    .withContentType(MediaType.APPLICATION_JSON_VALUE)
                                    .withAuthorizationBearer(accessToken),
                            Void.class
                    );
        } catch (RestClientResponseException e) {
                throw switch (e.getStatusCode()) {
                    case HttpStatus.BAD_REQUEST ->  ResponseException
                            .badRequest("Die Mitarbeiter:in konnte nicht erstellt werden. Ungültige Daten wurden übermittelt.", e);
                    case HttpStatus.FORBIDDEN ->  ResponseException
                            .badRequest("Der Backend-Client hat keine Berechtigung, um Mitarbeiter:innen zu erstellen.", e);
                    default -> ResponseException
                            .internalServerError("Die Mitarbeiter:in konnte nicht erstellt werden.", e);
                };
        }

        if (response.getStatusCode().isError()) {
            throw switch (response.getStatusCode()) {
                case HttpStatus.BAD_REQUEST ->  ResponseException
                        .badRequest("Die Mitarbeiter:in konnte nicht erstellt werden. Ungültige Daten wurden übermittelt.");
                case HttpStatus.FORBIDDEN ->  ResponseException
                        .badRequest("Der Backend-Client hat keine Berechtigung, um Mitarbeiter:innen zu erstellen.");
                default -> ResponseException
                        .internalServerError("Die Mitarbeiter:in konnte nicht erstellt werden. Status-Code: " + response.getStatusCode());
            };
        }

        // Extract the created user ID from the Location header because no http body is returned
        var location = response.getHeaders().getFirst("Location");
        if (location == null || location.isEmpty()) {
            throw ResponseException.internalServerError("Die Mitarbeiter:in Das Mitarbeiter:innen-Profil steht nach der nächsten Synchronisation zur Verfügung.");
        }

        String createdUserId = location
                .substring(location.lastIndexOf("/") + 1);

        return retrieveUser(createdUserId)
                .orElseThrow(() -> ResponseException.internalServerError("Die Mitarbeiter:in konnte nach der Erstellung nicht geladen werden."));
    }

    public Collection<KeycloakUser> listUsers() throws ResponseException {
        HttpResponse<String> response;
        try {
            response = get("/users?max=1000");
        } catch (URISyntaxException | IOException | HttpConnectionException e) {
            throw ResponseException.internalServerError("Die Liste der Mitarbeiter:innen konnte nicht geladen werden.", e);
        }

        if (response.statusCode() != 200) {
            throw ResponseException.internalServerError("Die Liste der Mitarbeiter:innen konnte nicht geladen werden.", "Status-Code: " + response.statusCode() + " - " + response.body());
        }

        var mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Collection<KeycloakUser> keycloakUsers;
        try {
            keycloakUsers = mapper.readerForListOf(KeycloakUser.class).readValue(response.body());
        } catch (JsonProcessingException e) {
            throw ResponseException.internalServerError("Die Mitarbeiter:in konnte nicht geladen werden", e);
        }

        return keycloakUsers;
    }


    public Optional<KeycloakUser> retrieveUser(String userId) throws ResponseException {
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
                throw ResponseException.internalServerError("Mitarbeiter:in mit der ID " + userId + " konnte nicht geladen werden.", "Status-Code: " + response.statusCode() + " - " + response.body());
            }
        }

        var mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        KeycloakUser keycloakUser;
        try {
            keycloakUser = mapper.readValue(response.body(), KeycloakUser.class);
        } catch (JsonProcessingException e) {
            throw ResponseException.internalServerError("Die Mitarbeiter:in konnte nicht geladen werden", e);
        }

        return Optional.of(keycloakUser);
    }

    public KeycloakUser updateUser(String userId, KeycloakUser userToUpdate) throws ResponseException {
        // Normalize the user object before updating
        userToUpdate.setId(userId);

        var mapper = ObjectMapperFactory
                .getInstance();

        String keycloakUserJson;
        try {
            keycloakUserJson = mapper.writeValueAsString(userToUpdate);
        } catch (JsonProcessingException e) {
            throw ResponseException.internalServerError("Die Mitarbeiter:in konnte nicht aktualisiert werden", e);
        }

        String accessToken;
        try {
            accessToken = getAccessToken();
        } catch (URISyntaxException | IOException | HttpConnectionException e) {
            throw ResponseException
                    .internalServerError("Die Mitarbeiter:in konnte nicht erstellt werden, da keine Anmeldung am Keycloak möglich war.", e);
        }

        URI uri;
        try {
            uri = new URI(keyCloakOIDCConfig.getHostname() + "/admin/realms/" + keyCloakOIDCConfig.getRealm() + "/users/" + userId);
        } catch (URISyntaxException e) {
            throw ResponseException
                    .internalServerError("Die Mitarbeiter:in konnte nicht erstellt werden, da die Keycloak-URL ungültig ist.", e);
        }

        ResponseEntity<Void> response;
        try {
            response = httpService.put(
                    uri,
                    keycloakUserJson,
                    HttpServiceHeaders
                            .create()
                            .withContentType(MediaType.APPLICATION_JSON_VALUE)
                            .withAuthorizationBearer(accessToken),
                    Void.class
            );
        } catch (RestClientResponseException e) {
            throw ResponseException.internalServerError("Die Mitarbeiter:in konnte nicht aktualisiert werden.", e);
        }

        if (response.getStatusCode().isError()) {
            throw ResponseException.internalServerError("Die Mitarbeiter:in konnte nicht aktualisiert werden.", "Status-Code: " + response.getStatusCode());
        }

        return retrieveUser(userId)
                .orElseThrow(() -> ResponseException.internalServerError("Die Mitarbeiter:in konnte nach der Aktualisierung nicht geladen werden."));
    }

    public void triggerPasswordReset(String userId) throws ResponseException {
        String accessToken;
        try {
            accessToken = getAccessToken();
        } catch (URISyntaxException | IOException | HttpConnectionException e) {
            throw ResponseException
                    .internalServerError("Der Passwort-Reset kann nicht initiiert werden, da keine Anmeldung am Keycloak möglich war.", e);
        }

        URI uri;
        try {
            uri = new URI(keyCloakOIDCConfig.getHostname() + "/admin/realms/" + keyCloakOIDCConfig.getRealm() + "/users/" + userId + "/execute-actions-email");
        } catch (URISyntaxException e) {
            throw ResponseException
                    .internalServerError("Der Passwort-Reset kann nicht initiiert werden, da die Keycloak-URL ungültig ist.", e);
        }

        ResponseEntity<Void> response;
        try {
            response = httpService.put(
                    uri,
                    "[\"UPDATE_PASSWORD\"]",
                    HttpServiceHeaders
                            .create()
                            .withContentType(MediaType.APPLICATION_JSON_VALUE)
                            .withAuthorizationBearer(accessToken),
                    Void.class
            );
        } catch (RestClientResponseException e) {
            throw ResponseException.internalServerError("Der Passwort-Reset kann nicht initiiert werden.", e);
        }

        if (response.getStatusCode().isError()) {
            throw ResponseException.internalServerError("Der Passwort-Reset kann nicht initiiert werden.", "Status-Code: " + response.getStatusCode());
        }
    }

    public void deleteUser(String id) {
        try {
            var response = httpService.delete(new URI(keyCloakOIDCConfig.getHostname() + "/admin/realms/" + keyCloakOIDCConfig.getRealm() + "/users/" + id), HttpServiceHeaders.create().withAuthorizationBearer(getAccessToken()));

            if (response.getStatusCode().isError()) {
                logger.error("Mitarbeiter:in mit der ID {} konnte nicht gelöscht werden. Status-Code: {}", id, response.getStatusCode());
            }
        } catch (URISyntaxException | IOException | HttpConnectionException e) {
            logger.error("Mitarbeiter:in mit der ID {} konnte nicht gelöscht werden.", id, e);
        }
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

        var res = httpService.get(uri, HttpServiceHeaders.create().withContentType("application/json").withAuthorizationBearer(accessToken));

        logger.info("GET request to Keycloak API at {} finished with status code {}", uri, res.statusCode());

        return res;
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
    private <T> ResponseEntity<T> post(String path, String body, Class<T> clazz) throws URISyntaxException, IOException, RestClientResponseException, HttpConnectionException {
        var accessToken = getAccessToken();

        var uri = new URI(keyCloakOIDCConfig.getHostname() + "/admin/realms/" + keyCloakOIDCConfig.getRealm() + path);

        logger.info("Starting POST request to Keycloak API at {}", uri);

        var res = httpService.postEntity(
                uri,
                body,
                HttpServiceHeaders.create().withContentType("application/json").withAuthorizationBearer(accessToken),
                clazz);

        logger.info("POST request to Keycloak API at {} finished with status code {}", uri, res.getStatusCode());

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
        var requestBody = Map.of("grant_type", "client_credentials", "client_id", keyCloakOIDCConfig.getBackendClientId(), "client_secret", keyCloakOIDCConfig.getBackendClientSecret());

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
