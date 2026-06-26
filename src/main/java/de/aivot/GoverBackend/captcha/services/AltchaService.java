package de.aivot.GoverBackend.captcha.services;

import de.aivot.GoverBackend.captcha.properties.CaptchaConfigurationProperties;
import org.altcha.altcha.v2.Altcha;
import org.springframework.stereotype.Service;

/**
 * This service is responsible for creating and verifying Altcha challenges.
 * It uses the Altcha library to generate a challenge and verify the solution.
 * The challenge is a proof-of-work that requires the client to perform some computation
 * before sending the solution back to the server.
 */
@Service
public class AltchaService {
    private static final String ALGORITHM = "PBKDF2/SHA-256";

    /** HMAC key used for signing and verifying the challenge. */
    private final String hmacKey;

    /** Proof-of-work difficulty (higher = more client CPU time). */
    private static final int COST = 5_000;

    /** Challenge validity in seconds. */
    private static final int EXPIRES_SEC = 300;  // 5 minutes

    public AltchaService(CaptchaConfigurationProperties config) {
        this.hmacKey = config.getKey();
    }

    /**
     * Creates a new Altcha challenge.
     * The challenge contains signed proof-of-work parameters and an expiration time.
     *
     * @return a new Altcha challenge
     * @throws Exception if an error occurs while creating the challenge
     */
    public Altcha.Challenge createChallenge() throws Exception {
        var opts = new Altcha.CreateChallengeOptions()
                .algorithm(ALGORITHM)
                .cost(COST)
                .hmacSignatureSecret(hmacKey)
                .expiresInSeconds(EXPIRES_SEC);

        return Altcha.createChallenge(opts);
    }

    /**
     * Verifies the Altcha solution.
     * The solution is a Base64-encoded JSON payload submitted by the widget.
     *
     * @param base64Payload the Base64‑encoded solution
     * @return true if the solution is valid, false otherwise
     * @throws Exception if an error occurs while verifying the solution
     */
    public boolean verify(String base64Payload) throws Exception {
        var result = Altcha.verifySolution(base64Payload, hmacKey, Altcha.kdf(ALGORITHM));
        return result.verified();
    }
}
