package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.process.cache.entities.ProcessSubmissionCacheEntity;
import de.aivot.prosuna.backend.process.cache.repositories.ProcessSubmissionCacheRepository;
import de.aivot.prosuna.backend.utils.RandomUtils;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Recognizes the browser that submitted a process instance.
 * <p>
 * The access key of an instance is not a secret of the submitter: every customer who is asked to participate later
 * receives it in their task link. Only the browser that submitted the instance holds the secret of this binding, so
 * it can find the first task of the instance before the submitter received a link to it.
 */
@Service
public class ProcessSubmissionBindingService {
    private static final int SECRET_LENGTH = 64;

    private final ProcessSubmissionCacheRepository processSubmissionCacheRepository;

    public ProcessSubmissionBindingService(ProcessSubmissionCacheRepository processSubmissionCacheRepository) {
        this.processSubmissionCacheRepository = processSubmissionCacheRepository;
    }

    /**
     * Binds the process instance to a new secret and returns the secret for the cookie of the submitting browser.
     */
    @Nonnull
    public String bind(@Nonnull Long processInstanceId) {
        var secret = RandomUtils.generateRandomString(SECRET_LENGTH);
        processSubmissionCacheRepository.save(new ProcessSubmissionCacheEntity(
                hashSecret(secret),
                processInstanceId
        ));
        return secret;
    }

    public boolean isBound(@Nullable String secret, @Nonnull Long processInstanceId) {
        if (StringUtils.isNullOrEmpty(secret)) {
            return false;
        }

        return processSubmissionCacheRepository
                .findById(hashSecret(secret))
                .map(binding -> processInstanceId.equals(binding.getProcessInstanceId()))
                .orElse(false);
    }

    @Nonnull
    private static String hashSecret(@Nonnull String secret) {
        try {
            var hash = MessageDigest
                    .getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Der SHA-256 Algorithmus wird nicht unterstützt.", e);
        }
    }
}
