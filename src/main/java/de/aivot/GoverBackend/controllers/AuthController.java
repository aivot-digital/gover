package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.models.auth.AuthCredentials;
import de.aivot.GoverBackend.models.auth.AuthResponse;
import de.aivot.GoverBackend.models.entities.User;
import de.aivot.GoverBackend.repositories.UserRepository;
import de.aivot.GoverBackend.services.JwtService;
import de.aivot.GoverBackend.services.PasswordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController
public class AuthController {
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Autowired
    public AuthController(
            UserRepository userRepository,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/api/login")
    public AuthResponse login(@RequestBody AuthCredentials credentials) {
        Optional<User> foundUser = userRepository.getByEmail(credentials.getEmail());

        if (foundUser.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        User user = foundUser.get();

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        if (PasswordService.testPassword(credentials.getPassword(), user.getPassword())) {
            String token = jwtService.generateToken(user);
            AuthResponse authResponse = new AuthResponse();
            authResponse.setJwtToken(token);
            return authResponse;
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
