package de.aivot.GoverBackend;

import de.aivot.GoverBackend.data.SystemConfigKey;
import de.aivot.GoverBackend.mail.ExceptionMailService;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.models.config.PuppetPdfConfig;
import de.aivot.GoverBackend.models.entities.SystemConfig;
import de.aivot.GoverBackend.repositories.SystemConfigRepository;
import de.aivot.GoverBackend.services.AssetStorageService;
import de.aivot.GoverBackend.mail.MailService;
import de.aivot.GoverBackend.services.SubmissionStorageService;
import io.sentry.Sentry;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;


@Component
public class ServerStartup implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger logger = LoggerFactory.getLogger(ServerStartup.class);
    private final ExceptionMailService mailService;
    private final SubmissionStorageService submissionStorageService;
    private final AssetStorageService assetStorageService;
    private final GoverConfig goverConfig;
    private final PuppetPdfConfig puppetPdfConfig;
    private final SystemConfigRepository systemConfigRepository;

    @Autowired
    public ServerStartup(
            ExceptionMailService mailService,
            SubmissionStorageService submissionStorageService,
            AssetStorageService assetStorageService,
            GoverConfig goverConfig, PuppetPdfConfig puppetPdfConfig, SystemConfigRepository systemConfigRepository
    ) {
        this.mailService = mailService;
        this.submissionStorageService = submissionStorageService;
        this.assetStorageService = assetStorageService;
        this.goverConfig = goverConfig;
        this.puppetPdfConfig = puppetPdfConfig;
        this.systemConfigRepository = systemConfigRepository;
    }

    @Override
    public void onApplicationEvent(@NotNull final ApplicationReadyEvent event) {
        initializeSentry();
        initializeStorages();
        initializeConfig();

        if (puppetPdfConfig.isEnabled()) {
            logger.info("Puppeteer PDF generation is enabled.");
        }
    }

    private void initializeConfig() {
        goverConfig.getSystemConfig().forEach((keyStr, value) -> {
            var key = SystemConfigKey
                    .fromString(keyStr)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown system config key: " + keyStr));

            var config = systemConfigRepository
                    .findById(key.getKey())
                    .orElseGet(() -> {
                        var newConfig = new SystemConfig();
                        newConfig.setKey(key.getKey());
                        newConfig.setCreated(LocalDateTime.now());
                        return newConfig;
                    });

            config.setPublicConfig(key.isPublic());
            config.setUpdated(LocalDateTime.now());
            config.setValue(value);

            logger.info("Setting system config: {} = {}", config.getKey(), config.getValue());

            systemConfigRepository.save(config);
        });
    }

    private void initializeSentry() {
        if (!goverConfig.getSentryServer().isBlank()) {
            logger.info("Starting server with Sentry: {}.", goverConfig.getSentryServer());
            Sentry.init(options -> {
                options.setDsn(goverConfig.getSentryServer());
                options.setEnvironment(goverConfig.getEnvironment());
                options.setTracesSampleRate(0.1);
            });
        } else {
            logger.warn("Starting server without Sentry.");
        }
    }

    private void initializeStorages() {
        try {
            assetStorageService.init();
        } catch (IOException e) {
            logger.error("Failed to prepare asset storage", e);
            mailService.send(e);
        }

        try {
            submissionStorageService.initRoot();
        } catch (IOException e) {
            logger.error("Failed to prepare submission storage", e);
            mailService.send(e);
        }
    }
}
