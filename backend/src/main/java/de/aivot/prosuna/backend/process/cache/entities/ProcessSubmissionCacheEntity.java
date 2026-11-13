package de.aivot.prosuna.backend.process.cache.entities;

import jakarta.annotation.Nonnull;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.util.Objects;

/**
 * Binds a submitted process instance to the browser that submitted it.
 * <p>
 * The binding only lets the confirmation page of the submitting browser find the first task of the instance, for
 * example to download the submitted application or to pay. It is not a general access grant for the instance.
 */
@RedisHash(value = "CacheProcessSubmission", timeToLive = ProcessSubmissionCacheEntity.TIME_TO_LIVE_SECONDS)
public class ProcessSubmissionCacheEntity implements Serializable {
    /**
     * Submission bindings expire after 4 hours, like cached identities.
     */
    public static final int TIME_TO_LIVE_SECONDS = 60 * 60 * 4;

    /**
     * Hash of the secret in the cookie of the submitting browser.
     */
    @Id
    @Nonnull
    private String id;
    @Nonnull
    private Long processInstanceId;

    // region Constructors

    public ProcessSubmissionCacheEntity() {
    }

    public ProcessSubmissionCacheEntity(@Nonnull String id,
                                        @Nonnull Long processInstanceId) {
        this.id = id;
        this.processInstanceId = processInstanceId;
    }

    // endregion

    // region HashCode and Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ProcessSubmissionCacheEntity that = (ProcessSubmissionCacheEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(processInstanceId, that.processInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, processInstanceId);
    }

    // endregion

    // region Getters and Setters

    @Nonnull
    public String getId() {
        return id;
    }

    public ProcessSubmissionCacheEntity setId(@Nonnull String id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public Long getProcessInstanceId() {
        return processInstanceId;
    }

    public ProcessSubmissionCacheEntity setProcessInstanceId(@Nonnull Long processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    // endregion
}
