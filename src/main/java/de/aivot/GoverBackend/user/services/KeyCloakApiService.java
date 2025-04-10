package de.aivot.GoverBackend.user.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.KeyCloakIdConfig;
import de.aivot.GoverBackend.models.config.KeyCloakOIDCConfig;
import de.aivot.GoverBackend.user.models.KeycloakRoleMapping;
import de.aivot.GoverBackend.user.models.KeycloakRoleMappings;
import de.aivot.GoverBackend.user.models.KeycloakUser;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class KeyCloakApiService {
    private final KeyCloakOIDCConfig keyCloakOIDCConfig;
    private final KeyCloakIdConfig keyCloakIdConfig;

    @Autowired
    public KeyCloakApiService(KeyCloakOIDCConfig keyCloakOIDCConfig, KeyCloakIdConfig keyCloakIdConfig) {
        this.keyCloakOIDCConfig = keyCloakOIDCConfig;
        this.keyCloakIdConfig = keyCloakIdConfig;
    }

    public Optional<KeycloakUser> getUser(String userId) throws ResponseException {
        HttpResponse<String> response;
        try {
            response = get("/users/" + userId);
        } catch (URISyntaxException | IOException | InterruptedException e) {
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
        } catch (URISyntaxException | IOException | InterruptedException e) {
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
        } catch (URISyntaxException | IOException | InterruptedException e) {
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
     * @throws URISyntaxException   if the uri is invalid
     * @throws IOException          if the request fails
     * @throws InterruptedException if the request is interrupted
     */
    private HttpResponse<String> get(String path) throws URISyntaxException, IOException, InterruptedException {
        var accessToken = getAccessToken();

        var uri = new URI(keyCloakOIDCConfig.getHostname() + "/admin/realms/" + keyCloakOIDCConfig.getRealm() + path);

        try (var client = HttpClient.newHttpClient()) {
            var request = HttpRequest
                    .newBuilder(uri)
                    .headers("Content-Type", "application/json")
                    .headers("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    /**
     * Retrieve the access token for the backend from the keycloak server
     *
     * @return the access token
     * @throws URISyntaxException   if the uri is invalid
     * @throws IOException          if the request fails
     * @throws InterruptedException if the request is interrupted
     */
    private String getAccessToken() throws URISyntaxException, IOException, InterruptedException {
        // Create the request body for fetching the access token
        var requestBody = String.format(
                "grant_type=client_credentials&client_id=%s&client_secret=%s",
                keyCloakOIDCConfig.getBackendClientId(),
                keyCloakOIDCConfig.getBackendClientSecret()
        );

        // Create the uri for the token endpoint
        var uri = new URI(keyCloakOIDCConfig.getHostname() + "/realms/" + keyCloakOIDCConfig.getRealm() + "/protocol/openid-connect/token");

        // Build the request for fetching the access token
        var request = HttpRequest
                .newBuilder(uri)
                .headers("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        // Send the request and get the response
        HttpResponse<String> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }

        // Check if the request was successful
        if (response.statusCode() != 200) {
            throw new IOException("Failed to get token from Keycloak: " + response.body());
        }

        // Parse the response and extract the access token
        return new JSONObject(response.body())
                .getString("access_token");
    }

    /**
     * Retrieve the public key from the keycloak server
     *
     * @return the public key
     * @throws URISyntaxException       if the uri is invalid
     * @throws IOException              if the request fails
     * @throws InterruptedException     if the request is interrupted
     * @throws NoSuchAlgorithmException if the algorithm for the public rsa key is invalid
     * @throws InvalidKeySpecException  if the key is invalid
     */
    public RSAPublicKey getCustomerRealmPublicKey() throws URISyntaxException, IOException, InterruptedException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Create the uri for the customer realm
        var uri = new URI(keyCloakIdConfig.getHostname() + "/realms/" + keyCloakIdConfig.getRealm());

        // Build the request for fetching the public key
        var request = HttpRequest
                .newBuilder(uri)
                .headers("Content-Type", "application/json")
                .GET()
                .build();

        // Send the request and get the response
        HttpResponse<String> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }

        // Parse the response and extract the public key
        var publicKey = new JSONObject(response.body())
                .getString("public_key");

        // Decode the public key into a byte array
        var publicKeyBytes = Base64
                .getDecoder()
                .decode(publicKey);

        // Create a key factory and generate the public key
        var spec = new X509EncodedKeySpec(publicKeyBytes);
        var kf = KeyFactory.getInstance("RSA");

        return (RSAPublicKey) kf.generatePublic(spec);
    }
}
