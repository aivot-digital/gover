package de.aivot.GoverBackend.nocode.controllers;

import de.aivot.GoverBackend.nocode.dtos.NoCodeOperatorDetailsDTO;
import de.aivot.GoverBackend.nocode.providers.NoCodeOperatorServiceProvider;
import de.aivot.GoverBackend.openApi.OpenAPIConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller to send data about available operators to the frontend.
 */
@RestController
@Tag(
        name = "No-Code Operators",
        description = "No-Code operators are the building blocks for creating no-code functionalities."
)
@SecurityRequirement(name = OpenAPIConfiguration.Name)
public class NoCodeOperatorController {
    private final List<NoCodeOperatorServiceProvider> noCodeOperatorServiceProviders;

    @Autowired
    public NoCodeOperatorController(List<NoCodeOperatorServiceProvider> noCodeOperatorServiceProviders) {
        this.noCodeOperatorServiceProviders = noCodeOperatorServiceProviders;
    }

    /**
     * List all available operators to display them in the frontend.
     *
     * @return A list of operators.
     */
    @GetMapping("/api/no-code/operators")
    @Operation(
            summary = "List No-Code Operators",
            description = "Retrieve a list of all available no-code operators."
    )
    public List<NoCodeOperatorDetailsDTO> listOperators() {
        return noCodeOperatorServiceProviders
                .stream()
                .flatMap(NoCodeOperatorDetailsDTO::fromSPI)
                .toList();
    }
}
