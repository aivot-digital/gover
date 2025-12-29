package de.aivot.GoverBackend.process.controllers;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinition;
import de.aivot.GoverBackend.process.services.ProcessNodeDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/process-node-definitions/")
@Tag(
        name = OpenApiConstants.Tags.ProcessesDefinitionsName,
        description = "Operations for managing process instance tasks."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessNodeProviderController {
    private final ProcessNodeDefinitionService processNodeProviderService;

    public ProcessNodeProviderController(ProcessNodeDefinitionService processNodeProviderService) {
        this.processNodeProviderService = processNodeProviderService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Process Node Providers",
            description = "Retrieve a list of all available process node providers."
    )
    public List<ProcessNodeDefinition> list() throws ResponseException {
        return processNodeProviderService
                .getAllProcessNodeDefinitions();
    }
}
