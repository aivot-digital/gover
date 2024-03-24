package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.exceptions.NotFoundException;
import de.aivot.GoverBackend.models.auth.KeyCloakDetailsUser;
import de.aivot.GoverBackend.models.auth.KeyCloakListUser;
import de.aivot.GoverBackend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

@RestController
public class UserController {
    private final UserRepository userRepository;

    @Autowired
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/api/users")
    public Collection<KeyCloakListUser> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false, name = "id") List<String> filterIds
    ) throws IOException, URISyntaxException, InterruptedException {
        if (filterIds != null) {
            LinkedList<KeyCloakListUser> users = new LinkedList<>();

            for (var id : filterIds) {
                userRepository
                        .getUser(id, jwt)
                        .ifPresent(users::add);
            }

            return users;
        }

        return userRepository.getUsers(jwt);
    }

    @GetMapping("/api/users/self")
    public KeyCloakDetailsUser retrieveSelf(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return KeyCloakDetailsUser
                .fromJwt(jwt);
    }

    @GetMapping("/api/users/{id}")
    public KeyCloakDetailsUser retrieve(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id
    ) throws URISyntaxException, IOException, InterruptedException {
        return userRepository
                .getUser(id, jwt)
                .orElseThrow(NotFoundException::new);
    }
}
