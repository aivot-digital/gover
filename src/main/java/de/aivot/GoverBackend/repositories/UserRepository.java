package de.aivot.GoverBackend.repositories;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.models.auth.*;
import de.aivot.GoverBackend.redis.CacheUserRepository;
import de.aivot.GoverBackend.services.KeyCloakApiService;
import de.aivot.GoverBackend.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.util.*;

@Component
public class UserRepository {
    private final KeyCloakApiService keyCloakApiService;
    private final CacheUserRepository cacheUserRepository;

    @Autowired
    public UserRepository(KeyCloakApiService keyCloakApiService, CacheUserRepository cacheUserRepository) {
        this.keyCloakApiService = keyCloakApiService;
        this.cacheUserRepository = cacheUserRepository;
    }

    public Optional<KeyCloakDetailsUser> getUserAsServer(String id) {
        if (StringUtils.isNullOrEmpty(id)) {
            return Optional.empty();
        }

        var cacheUser = cacheUserRepository.findById(id);
        if (cacheUser.isPresent()) {
            return Optional.of(KeyCloakDetailsUser.fromJson(cacheUser.get().getJson()));
        }

        HttpResponse<String> response;
        try {
            response = keyCloakApiService.get("/users/" + id);
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (response.statusCode() != 200) {
            if (response.statusCode() == 404) {
                return Optional.empty();
            } else {
                throw new ResponseStatusException(HttpStatus.valueOf(response.statusCode()), response.body());
            }
        }

        return Optional.of(KeyCloakDetailsUser.fromJson(response.body()));
    }

    public Optional<KeyCloakDetailsUser> getUser(String id, Jwt jwt) {
        if (StringUtils.isNullOrEmpty(id)) {
            return Optional.empty();
        }

        HttpResponse<String> response;
        try {
            response = keyCloakApiService.get("/users/" + id, jwt);
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (response.statusCode() != 200) {
            if (response.statusCode() == 404) {
                return Optional.empty();
            } else {
                throw new ResponseStatusException(HttpStatus.valueOf(response.statusCode()));
            }
        }

        return Optional.of(KeyCloakDetailsUser.fromJson(response.body()));
    }

    public Collection<KeyCloakListUser> getUsers(Jwt jwt) {
        HttpResponse<String> response;
        try {
            response = keyCloakApiService.get("/users", jwt);
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (response.statusCode() != 200) {
            throw new ResponseStatusException(HttpStatus.valueOf(response.statusCode()));
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            var users = mapper.readValue(response.body(), KeyCloakListUser[].class);
            return Arrays.asList(users);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Collection<KeyCloakRole> getUserRoles(Jwt jwt, String userId) {
        HttpResponse<String> response;
        try {
            response = keyCloakApiService.get("/users/" + userId + "/role-mappings", jwt);
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (response.statusCode() != 200) {
            throw new ResponseStatusException(HttpStatus.valueOf(response.statusCode()));
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        KeyCloakRealmMappings mappings;
        try {
            mappings = mapper.readValue(response.body(), KeyCloakRealmMappings.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return List.of(mappings.getRealmMappings());
    }

    private Collection<KeyCloakGroup> getUserGroups(Jwt jwt, String userId) {
        HttpResponse<String> response;
        try {
            response = keyCloakApiService.get("/users/" + userId + "/groups", jwt);
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (response.statusCode() != 200) {
            throw new ResponseStatusException(HttpStatus.valueOf(response.statusCode()));
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            return List.of(mapper.readValue(response.body(), KeyCloakGroup[].class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
