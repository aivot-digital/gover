package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.config.services.SystemConfigService;
import de.aivot.GoverBackend.core.configs.ProviderNameSystemConfigDefinition;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.system.properties.BuildProperties;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ProcessNodeExportService {
    private final ProcessNodeService processNodeService;
    private final ProcessNodeDefinitionService processNodeDefinitionService;
    private final BuildProperties buildProperties;
    private final SystemConfigService systemConfigService;

    public ProcessNodeExportService(ProcessService processService,
                                    ProcessNodeService processNodeService,
                                    ProcessNodeDefinitionService processNodeDefinitionService,
                                    BuildProperties buildProperties,
                                    SystemConfigService systemConfigService) {
        this.processNodeService = processNodeService;
        this.processNodeDefinitionService = processNodeDefinitionService;
        this.buildProperties = buildProperties;
        this.systemConfigService = systemConfigService;
    }

    public ProcessNodeExport export(Integer nodeId) throws ResponseException {
        var node = processNodeService
                .retrieve(nodeId)
                .orElseThrow(ResponseException::notFound);

        var provider = processNodeDefinitionService
                .getProcessNodeDefinition(node.getProcessNodeDefinitionKey(), node.getProcessNodeDefinitionVersion())
                .orElseThrow(() -> ResponseException.badRequest(
                        "Eine Prozesselementdefinition mit dem Schlüssel „%s“ und der Version „%d“ ist nicht verfügbar."
                                .formatted(node.getProcessNodeDefinitionKey(), node.getProcessNodeDefinitionVersion())
                ));

        var clonedConfiguration = node
                .getConfiguration()
                .clone();
        var cleanedConfiguration = provider
                .cleanConfigurationForExport(clonedConfiguration);

        var cleanedNode = new ProcessNodeEntity(
                null, // id,
                null, // processId
                null, // processVersion
                node.getName(), // name
                node.getDescription(), // description
                node.getDataKey(), // dataKey
                node.getProcessNodeDefinitionKey(), // processNodeDefinitionKey
                node.getProcessNodeDefinitionVersion(), // processNodeDefinitionVersion
                cleanedConfiguration, // configuration
                node.getOutputMappings(), // outputMappings
                node.getTimeLimitDays(), // timeLimitDays
                node.getRequirements(), // requirements
                node.getNotes(), // notes
                false // savedWithErrors
        );

        var vendorName = systemConfigService
                .retrieve(ProviderNameSystemConfigDefinition.KEY)
                .getValue();
        if (vendorName == null) {
            vendorName = "Unbekannt";
        }

        return new ProcessNodeExport(
                buildProperties.getBuildVersion(),
                buildProperties.getBuildNumber(),
                LocalDateTime.now(),
                vendorName,
                cleanedNode
        );
    }

    public record ProcessNodeExport(
            @Nonnull
            String appVersion,
            @Nonnull
            String appBuildNumber,
            @Nonnull
            LocalDateTime exportTimestamp,
            @Nonnull
            String createdByVendor,
            @Nonnull
            @NotNull
            ProcessNodeEntity node
    ) {
    }
}
