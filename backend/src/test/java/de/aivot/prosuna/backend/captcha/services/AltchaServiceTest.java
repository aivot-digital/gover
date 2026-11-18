package de.aivot.prosuna.backend.captcha.services;

import de.aivot.prosuna.backend.captcha.properties.CaptchaConfigurationProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.altcha.altcha.v2.Altcha;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AltchaServiceTest {
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final AltchaService service = new AltchaService(properties(), meterRegistry);

    @Test
    void countsSolvedChallenge() throws Exception {
        assertTrue(service.verify(payload(solve(service.createChallenge()))));

        assertEquals(1, verifications("solved"));
    }

    @Test
    void countsWrongSolution() throws Exception {
        var challenge = service.createChallenge();
        var solution = solve(challenge);
        var wrong = new Altcha.Payload(challenge, new Altcha.Solution(solution.solution().counter() + 1,
                solution.solution().derivedKey(), solution.solution().time()));

        assertFalse(service.verify(payload(wrong)));

        assertEquals(1, verifications("failed"));
    }

    @Test
    void countsMalformedPayload() {
        assertThrows(Exception.class, () -> service.verify("not a payload"));

        assertEquals(1, verifications("error"));
    }

    private double verifications(String outcome) {
        return meterRegistry.counter("prosuna.captcha.verifications", "outcome", outcome).count();
    }

    private static Altcha.Payload solve(Altcha.Challenge challenge) throws Exception {
        return new Altcha.Payload(challenge, Altcha.solveChallenge(challenge, Altcha.kdf("PBKDF2/SHA-256")));
    }

    private static String payload(Altcha.Payload payload) {
        var json = JsonMapper.builder().build().writeValueAsString(payload);
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static CaptchaConfigurationProperties properties() {
        var properties = new CaptchaConfigurationProperties();
        properties.setKey("test-key");
        return properties;
    }
}
