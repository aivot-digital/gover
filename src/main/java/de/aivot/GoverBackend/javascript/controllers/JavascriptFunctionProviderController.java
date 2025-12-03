package de.aivot.GoverBackend.javascript.controllers;

import de.aivot.GoverBackend.javascript.providers.JavascriptFunctionProvider;
import de.aivot.GoverBackend.openApi.OpenAPIConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/javascript-function-providers/")
@Tag(
        name = "Javascript Function Providers",
        description = "Provides type definitions for Javascript function providers."
)
@SecurityRequirement(name = OpenAPIConfiguration.Name)
public class JavascriptFunctionProviderController {
    private final String types;

    @Autowired
    public JavascriptFunctionProviderController(List<JavascriptFunctionProvider> providers) {
        this.types = providers
                .stream()
                .map(JavascriptFunctionProvider::getTypeDefinition)
                .collect(Collectors.joining("\n\n"));
    }

    @GetMapping("types.d.ts")
    @Operation(
            summary = "Get Javascript Function Provider Type Definitions",
            description = "Returns the combined TypeScript type definitions for all registered Javascript function providers."
    )
    public String getTypeDefinitions() {
        return types;
    }
}
