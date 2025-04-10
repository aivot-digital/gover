package de.aivot.GoverBackend.nocode.controllers;

import de.aivot.GoverBackend.nocode.dtos.NoCodeOperatorDetailsDTO;
import de.aivot.GoverBackend.nocode.providers.NoCodeOperatorServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller to send data about available operators to the frontend.
 */
@RestController
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
    public List<NoCodeOperatorDetailsDTO> listOperators() {
        return noCodeOperatorServiceProviders
                .stream()
                .flatMap(NoCodeOperatorDetailsDTO::fromSPI)
                .toList();
    }
}
