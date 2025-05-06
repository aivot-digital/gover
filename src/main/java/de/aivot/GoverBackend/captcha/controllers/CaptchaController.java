package de.aivot.GoverBackend.captcha.controllers;

import de.aivot.GoverBackend.captcha.dtos.CaptchaVerificationRequestDTO;
import de.aivot.GoverBackend.captcha.dtos.CaptchaVerificationResponseDTO;
import de.aivot.GoverBackend.captcha.services.AltchaService;
import de.aivot.GoverBackend.captcha.services.RedisCaptchaReplayGuard;
import org.altcha.altcha.Altcha;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/*
 * This controller handles the Altcha captcha verification process.
 * It provides endpoints for creating a new challenge and verifying the solution.
 * The challenge is a proof-of-work that requires the client to perform some computation
 * before sending the solution back to the server.
 */
@RestController
public class CaptchaController {

    private final AltchaService altchaService;
    private final RedisCaptchaReplayGuard replayGuard;

    public CaptchaController(AltchaService altchaService, RedisCaptchaReplayGuard replayGuard) {
        this.altchaService = altchaService;
        this.replayGuard = replayGuard;
    }

    @GetMapping("/api/public/captcha/challenge/")
    public Altcha.Challenge challenge() throws Exception {
        return altchaService.createChallenge();
    }

    /* verification for forms is handled in the Submit endpoint, this authenticated verification endpoint is only used for debugging */
    @PostMapping("/api/captcha/verify/")
    public ResponseEntity<CaptchaVerificationResponseDTO> verify(
            @RequestBody CaptchaVerificationRequestDTO request) throws Exception {

        if (replayGuard.isUsed(request.payload())) {
            return ResponseEntity.status(400)
                    .body(new CaptchaVerificationResponseDTO(false));
        }

        boolean ok = altchaService.verify(request.payload());

        return ok
                ? ResponseEntity.ok(new CaptchaVerificationResponseDTO(true))
                : ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new CaptchaVerificationResponseDTO(false));
    }
}