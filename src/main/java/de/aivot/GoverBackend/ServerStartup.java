package de.aivot.GoverBackend;

import com.oracle.truffle.js.runtime.Strings;
import com.sun.istack.NotNull;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.models.entities.User;
import de.aivot.GoverBackend.repositories.UserRepository;
import de.aivot.GoverBackend.services.AssetStorageService;
import de.aivot.GoverBackend.services.MailService;
import de.aivot.GoverBackend.services.SubmissionStorageService;
import de.aivot.GoverBackend.utils.StringUtils;
import io.sentry.Sentry;
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
    private static final Logger logger = LoggerFactory.getLogger(ServerStartup.class);
    private final UserRepository userRepository;
    private final MailService mailService;
    private final SubmissionStorageService submissionStorageService;
    private final AssetStorageService assetStorageService;
    private final GoverConfig goverConfig;

    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public ServerStartup(
            UserRepository userRepository,
            MailService mailService,
            SubmissionStorageService submissionStorageService,
            AssetStorageService assetStorageService,
            GoverConfig goverConfig
    ) {
        this.userRepository = userRepository;
        this.mailService = mailService;
        this.submissionStorageService = submissionStorageService;
        this.assetStorageService = assetStorageService;
        this.goverConfig = goverConfig;
    }

    @Override
    public void onApplicationEvent(@NotNull final ApplicationReadyEvent event) {
        setupSentry();

        createInitialAdminUser();
        initializeStorages();
    }

    private void setupSentry() {
        if (!goverConfig.getSentryServer().isBlank()) {
            logger.info("Starting server with Sentry: {}.", goverConfig.getSentryServer());
            Sentry.init(options -> options.setDsn(goverConfig.getSentryServer()));
        } else {
            logger.warn("Starting server without Sentry.");
        }
    }

    private void createInitialAdminUser() {
        boolean adminExists = userRepository.existsByAdminIsTrue();
        if (!adminExists) {
            String initialEmail = "admin@gover.digital";
            String initialPassword = StringUtils.generateRandomString(12);

            User newAdmin = new User();
            newAdmin.setName("Default Admin User");
            newAdmin.setEmail(initialEmail);
            newAdmin.setActive(true);
            newAdmin.setAdmin(true);
            newAdmin.setPassword(passwordEncoder.encode(initialPassword));
            userRepository.save(newAdmin);

            logger.warn("Created default admin with email \"{}\" and password \"{}\"", initialEmail, initialPassword);

            mailService.sendInfoMail(
                    "Standard-Administrator erstellt",
                    Strings.format("E-Mail: %s Password: %s",
                            initialEmail,
                            initialPassword
                    ).toString()
            );
        }
    }

    private void initializeStorages() {
        try {
            assetStorageService.init();
        } catch (IOException e) {
            logger.error("Failed to prepare asset storage", e);
            mailService.sendExceptionMail(e);
        }

        try {
            submissionStorageService.initRoot();
        } catch (IOException e) {
            logger.error("Failed to prepare submission storage", e);
            mailService.sendExceptionMail(e);
        }
    }
}
