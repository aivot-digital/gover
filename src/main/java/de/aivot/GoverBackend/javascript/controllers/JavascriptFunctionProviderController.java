package de.aivot.GoverBackend.javascript.controllers;

import de.aivot.GoverBackend.javascript.providers.JavascriptFunctionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/javascript-function-providers/")
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
    public String getTypeDefinitions() {
        return types;
    }
}
