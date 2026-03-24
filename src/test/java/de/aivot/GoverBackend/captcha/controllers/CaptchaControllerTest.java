package de.aivot.GoverBackend.captcha.controllers;

import de.aivot.GoverBackend.captcha.dtos.CaptchaVerificationRequestDTO;
import de.aivot.GoverBackend.captcha.services.AltchaService;
import de.aivot.GoverBackend.captcha.services.RedisCaptchaReplayGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CaptchaControllerTest {
    @Mock
    private AltchaService altchaService;

    @Mock
    private RedisCaptchaReplayGuard replayGuard;

    private CaptchaController captchaController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        captchaController = new CaptchaController(altchaService, replayGuard);
    }

    @Test
    void challengeEndpointShouldRemainPublic() throws NoSuchMethodException {
        var mapping = CaptchaController.class
                .getMethod("challenge")
                .getAnnotation(GetMapping.class);

        assertNotNull(mapping);
        assertArrayEquals(new String[]{"/api/public/captcha/challenge/"}, mapping.value());
    }

    @Test
    void verifyEndpointShouldRemainProtected() throws NoSuchMethodException {
        var mapping = CaptchaController.class
                .getMethod("verify", CaptchaVerificationRequestDTO.class)
                .getAnnotation(PostMapping.class);

        assertNotNull(mapping);
        assertArrayEquals(new String[]{"/api/captcha/verify/"}, mapping.value());
    }
}
