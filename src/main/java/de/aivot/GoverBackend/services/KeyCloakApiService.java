package de.aivot.GoverBackend.services;

import de.aivot.GoverBackend.models.config.KeyCloakIdConfig;
import de.aivot.GoverBackend.models.config.KeyCloakOIDCConfig;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
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

@Component
public class KeyCloakApiService {
    private final KeyCloakOIDCConfig keyCloakOIDCConfig;
    private final KeyCloakIdConfig keyCloakIdConfig;

    @Autowired
    public KeyCloakApiService(KeyCloakOIDCConfig keyCloakOIDCConfig, KeyCloakIdConfig keyCloakIdConfig) {
        this.keyCloakOIDCConfig = keyCloakOIDCConfig;
        this.keyCloakIdConfig = keyCloakIdConfig;
    }

    public HttpResponse<String> get(String path) throws URISyntaxException, IOException, InterruptedException {
        var uri = new URI(keyCloakOIDCConfig.getHostname() + "/realms/" + keyCloakOIDCConfig.getRealm() + "/protocol/openid-connect/token");

        var client = HttpClient.newHttpClient();
        var request = HttpRequest
                .newBuilder(uri)
                .headers("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials&client_id=" + keyCloakOIDCConfig.getBackendClientId() + "&client_secret=" + keyCloakOIDCConfig.getBackendClientSecret()))
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to get token from KeyCloak: " + response.body());
        }

        var data = new JSONObject(response.body());

        client.close();

        return get(path, data.getString("access_token"));
    }

    public HttpResponse<String> get(String path, Jwt jwt) throws URISyntaxException, IOException, InterruptedException {
        return get(path, jwt.getTokenValue());
    }

    public HttpResponse<String> get(String path, String accessToken) throws URISyntaxException, IOException, InterruptedException {
        var uri = new URI(keyCloakOIDCConfig.getHostname() + "/admin/realms/" + keyCloakOIDCConfig.getRealm() + path);

        var client = HttpClient.newHttpClient();
        var request = HttpRequest
                .newBuilder(uri)
                .headers("Content-Type", "application/json")
                .headers("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        client.close();

        return response;
    }

    public RSAPublicKey getRealmPublicKey() throws URISyntaxException, IOException, InterruptedException, NoSuchAlgorithmException, InvalidKeySpecException {
        var uri = new URI(keyCloakIdConfig.getHostname() + "/realms/" + keyCloakIdConfig.getRealm());

        var client = HttpClient.newHttpClient();
        var request = HttpRequest
                .newBuilder(uri)
                .headers("Content-Type", "application/json")
                .GET()
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        var data = new JSONObject(response.body());

        var publicKey = data.getString("public_key");
        var publicKeyBytes = Base64.getDecoder().decode(publicKey);

        var spec = new X509EncodedKeySpec(publicKeyBytes);
        var kf = KeyFactory.getInstance("RSA");

        client.close();

        return (RSAPublicKey) kf.generatePublic(spec);
    }
}
