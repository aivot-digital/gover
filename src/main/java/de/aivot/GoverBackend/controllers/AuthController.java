package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.enums.UserRole;
import de.aivot.GoverBackend.models.AuthCredentials;
import de.aivot.GoverBackend.models.AuthResponse;
import de.aivot.GoverBackend.models.SetPasswordRequest;
import de.aivot.GoverBackend.models.User;
import de.aivot.GoverBackend.repositories.UserRepository;
import de.aivot.GoverBackend.services.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.Optional;

@RestController
@CrossOrigin
public class AuthController {
    @Autowired
    UserRepository userRepository;
    @Autowired
    JwtService jwtService;
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthCredentials credentials) {
        Optional<User> foundUser = userRepository.findByEmail(credentials.getEmail());

        if (foundUser.isEmpty()) {
            throw new AccessDeniedException("Invalid email or password");
        }

        User user = foundUser.get();

        if (!user.isActive()) {
            throw new AccessDeniedException("Inactive user");
        }

        if (passwordEncoder.matches(credentials.getPassword(), user.getPassword())) {
            String token = jwtService.generateToken(user);
            AuthResponse authResponse = new AuthResponse();
            authResponse.setJwtToken(token);
            return ResponseEntity.ok(authResponse);
        } else {
            throw new AccessDeniedException("Invalid email or password");
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<User> getProfile(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        if (user == null) {
            throw new ResourceNotFoundException();
        }
        if (!user.isActive()) {
            throw new AccessDeniedException("Inactive user");
        }
        return ResponseEntity.ok(user);
    }

    @PostMapping("/profile/set-password")
    public ResponseEntity<HttpStatus> setPassword(Authentication authentication, @RequestBody SetPasswordRequest setPasswordRequest) {
        User requester = (User) authentication.getPrincipal();

        Long userId = setPasswordRequest.getUserId() != null ? setPasswordRequest.getUserId() : requester.getId();

        Optional<User> foundUser = userRepository.findById(userId);

        if (foundUser.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        User user = foundUser.get();

        if (!Objects.equals(user.getId(), requester.getId()) && requester.getRole() != UserRole.Admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        String newPassword = passwordEncoder.encode(setPasswordRequest.getPassword());
        user.setPassword(newPassword);
        userRepository.save(user);

        return ResponseEntity.ok(HttpStatus.OK);
    }
}
