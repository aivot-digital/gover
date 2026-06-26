package de.aivot.GoverBackend.user.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.mail.services.UserOnboardingMailService;
import de.aivot.GoverBackend.user.dtos.CreateUserRequestDTO;
import de.aivot.GoverBackend.user.dtos.CreateUserResponseDTO;
import de.aivot.GoverBackend.user.dtos.UserInitialCredentialsDTO;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.userRoles.repositories.SystemRoleRepository;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserProvisioningService {
    private static final Logger logger = LoggerFactory.getLogger(UserProvisioningService.class);

    private static final List<String> INITIAL_REQUIRED_ACTIONS = List.of("VERIFY_EMAIL");
    private static final String UPPERCASE_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWERCASE_PASSWORD_CHARS = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGIT_PASSWORD_CHARS = "23456789";
    private static final String SPECIAL_PASSWORD_CHARS = "!@$%*?&";
    private static final String ALL_PASSWORD_CHARS = UPPERCASE_PASSWORD_CHARS + LOWERCASE_PASSWORD_CHARS + DIGIT_PASSWORD_CHARS + SPECIAL_PASSWORD_CHARS;
    private static final int TEMPORARY_PASSWORD_LENGTH = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserService userService;
    private final SystemRoleRepository systemRoleRepository;
    private final UserOnboardingMailService userOnboardingMailService;

    @Autowired
    public UserProvisioningService(
            UserService userService,
            SystemRoleRepository systemRoleRepository,
            UserOnboardingMailService userOnboardingMailService
    ) {
        this.userService = userService;
        this.systemRoleRepository = systemRoleRepository;
        this.userOnboardingMailService = userOnboardingMailService;
    }

    @Nonnull
    public CreateUserResponseDTO provision(@Nonnull CreateUserRequestDTO request) throws ResponseException {
        return provision(request.user(), Boolean.TRUE.equals(request.sendInitialCredentialsByEmail()));
    }

    @Nonnull
    public CreateUserResponseDTO provision(@Nonnull UserEntity entity, boolean sendInitialCredentialsByEmail) throws ResponseException {
        if (entity.getSystemRoleId() == null) {
            throw ResponseException.badRequest("Bitte eine Systemrolle auswählen.");
        }

        var systemRole = systemRoleRepository
                .findById(entity.getSystemRoleId())
                .orElseThrow(() -> ResponseException.badRequest("Die ausgewählte Systemrolle existiert nicht."));

        var temporaryPassword = generateTemporaryPassword();
        var createdUser = userService.create(entity, temporaryPassword, INITIAL_REQUIRED_ACTIONS);
        var initialCredentials = new UserInitialCredentialsDTO(
                createUserDisplayName(createdUser),
                createdUser.getEmail(),
                systemRole.getName(),
                temporaryPassword
        );

        if (!sendInitialCredentialsByEmail) {
            return new CreateUserResponseDTO(
                    createdUser,
                    false,
                    null,
                    initialCredentials
            );
        }

        if (!userOnboardingMailService.isSendingConfigured()) {
            return new CreateUserResponseDTO(
                    createdUser,
                    false,
                    "Der automatische E-Mail-Versand ist derzeit nicht konfiguriert.",
                    initialCredentials
            );
        }

        try {
            userOnboardingMailService.send(createdUser, initialCredentials);
            return new CreateUserResponseDTO(
                    createdUser,
                    true,
                    null,
                    null
            );
        } catch (Exception e) {
            logger.error("Initial access data mail could not be sent to {}", createdUser.getEmail(), e);

            return new CreateUserResponseDTO(
                    createdUser,
                    false,
                    "Die E-Mail mit den initialen Zugangsdaten konnte nicht versendet werden.",
                    initialCredentials
            );
        }
    }

    @Nonnull
    private String createUserDisplayName(@Nonnull UserEntity user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }

        var displayName = List.of(
                        user.getFirstName() == null ? "" : user.getFirstName(),
                        user.getLastName() == null ? "" : user.getLastName()
                )
                .stream()
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(" "))
                .trim();

        if (!displayName.isEmpty()) {
            return displayName;
        }

        return user.getEmail();
    }

    @Nonnull
    private String generateTemporaryPassword() {
        var passwordChars = new ArrayList<Character>();
        passwordChars.add(randomPasswordChar(UPPERCASE_PASSWORD_CHARS));
        passwordChars.add(randomPasswordChar(LOWERCASE_PASSWORD_CHARS));
        passwordChars.add(randomPasswordChar(DIGIT_PASSWORD_CHARS));
        passwordChars.add(randomPasswordChar(SPECIAL_PASSWORD_CHARS));

        while (passwordChars.size() < TEMPORARY_PASSWORD_LENGTH) {
            passwordChars.add(randomPasswordChar(ALL_PASSWORD_CHARS));
        }

        Collections.shuffle(passwordChars, SECURE_RANDOM);

        var password = new StringBuilder(TEMPORARY_PASSWORD_LENGTH);
        passwordChars.forEach(password::append);
        return password.toString();
    }

    private char randomPasswordChar(@Nonnull String allowedCharacters) {
        return allowedCharacters.charAt(SECURE_RANDOM.nextInt(allowedCharacters.length()));
    }
}
