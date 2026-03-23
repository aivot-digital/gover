package de.aivot.GoverBackend.nocode.controllers;

import de.aivot.GoverBackend.nocode.dtos.NoCodeTestRequestDTO;
import de.aivot.GoverBackend.nocode.models.NoCodeResult;
import de.aivot.GoverBackend.nocode.services.NoCodeEvaluationService;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to send data about available operators to the frontend.
 */
@RestController
@Tag(
        name = "No-Code Testing",
        description = "Endpoints for testing no-code expressions."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class NoCodeTestController {
    private final NoCodeEvaluationService noCodeEvaluationService;

    @Autowired
    public NoCodeTestController(NoCodeEvaluationService noCodeEvaluationService) {
        this.noCodeEvaluationService = noCodeEvaluationService;
    }

    /**
     * List all available operators to display them in the frontend.
     *
     * @return A list of operators.
     */
    @PostMapping("/api/no-code/test")
    @Operation(
            summary = "Test No-Code Expression",
            description = "Evaluate a no-code expression with provided element data."
    )
    public NoCodeResult test(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid NoCodeTestRequestDTO requestDTO
    ) {
        return noCodeEvaluationService
                .evaluate(requestDTO.expression(), requestDTO.elementData());
    }
}
