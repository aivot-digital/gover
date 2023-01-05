package de.aivot.GoverBackend;

import com.oracle.truffle.js.runtime.Strings;
import de.aivot.GoverBackend.enums.UserRole;
import de.aivot.GoverBackend.models.User;
import de.aivot.GoverBackend.repositories.UserRepository;
import de.aivot.GoverBackend.services.BlobService;
import de.aivot.GoverBackend.services.SystemMailService;
import de.aivot.GoverBackend.utils.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ServerStartup implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger logger = LoggerFactory.getLogger(SystemMailService.class);
    private final UserRepository userRepository;
    private final SystemMailService systemMailService;
    private final BlobService blobService;

    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public ServerStartup(UserRepository userRepository, SystemMailService systemMailService, BlobService blobService) {
        this.userRepository = userRepository;
        this.systemMailService = systemMailService;
        this.blobService = blobService;
    }

    @Override
    public void onApplicationEvent(@NotNull final ApplicationReadyEvent event) {
        createInitialAdminUser();
        try {
            blobService.init();
        } catch (IOException e) {
            logger.error("Failed to prepare storage", e);
            systemMailService.sendExceptionMail(e);
        }
    }

    private void createInitialAdminUser() {
        boolean adminExists = userRepository.existsByRole(UserRole.Admin);
        if (!adminExists) {
            String initialEmail = "admin@gover.aivot.de";
            String initialPassword = StringUtils.generateRandomString(12);

            User newAdmin = new User();
            newAdmin.setName("Default Admin User");
            newAdmin.setEmail(initialEmail);
            newAdmin.setActive(true);
            newAdmin.setRole(UserRole.Admin);
            newAdmin.setPassword(passwordEncoder.encode(initialPassword));
            userRepository.save(newAdmin);

            logger.info("Created default admin with email \"{}\" and password \"{}\"", initialEmail, initialPassword);

            systemMailService.sendInfoMail(
                    "Created a default admin",
                    Strings.format("E-Mail: %s Password: %s", initialEmail, initialPassword).toString()
            );
        }
    }
}
