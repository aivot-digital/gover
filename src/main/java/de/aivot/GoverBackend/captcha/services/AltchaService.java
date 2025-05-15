package de.aivot.GoverBackend.captcha.services;

import de.aivot.GoverBackend.captcha.properties.CaptchaConfigurationProperties;
import org.altcha.altcha.Altcha;
import org.springframework.stereotype.Service;

/**
 * This service is responsible for creating and verifying Altcha challenges.
 * It uses the Altcha library to generate a challenge and verify the solution.
 * The challenge is a proof-of-work that requires the client to perform some computation
 * before sending the solution back to the server.
 */
@Service
public class AltchaService {
    /** HMAC key used for signing and verifying the challenge. */
    private final String hmacKey;

    /** Proof‑of‑work difficulty (higher = more client CPU time). */
    private static final long MAX_NUMBER = 200_000;

    /** Challenge validity in seconds. */
    private static final int EXPIRES_SEC = 300;  // 5 minutes

    public AltchaService(CaptchaConfigurationProperties config) {
        this.hmacKey = config.getKey();
    }

    /**
     * Creates a new Altcha challenge.
     * The challenge is a Base64‑encoded string that contains the proof‑of‑work difficulty and
     * the expiration time.
     *
     * @return a new Altcha challenge
     * @throws Exception if an error occurs while creating the challenge
     */
    public Altcha.Challenge createChallenge() throws Exception {
        var opts = new Altcha.ChallengeOptions()
                .setHmacKey(hmacKey)
                .setMaxNumber(MAX_NUMBER)
                .setExpiresInSeconds(EXPIRES_SEC);

        return Altcha.createChallenge(opts);
    }

    /**
     * Verifies the Altcha solution.
     * The solution is a Base64‑encoded string that contains the proof‑of‑work difficulty and
     * the expiration time.
     *
     * @param base64Payload the Base64‑encoded solution
     * @return true if the solution is valid, false otherwise
     * @throws Exception if an error occurs while verifying the solution
     */
    public boolean verify(String base64Payload) throws Exception {
        return Altcha.verifySolution(base64Payload, hmacKey, true);
    }
}