package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.config.services.SystemConfigService;
import de.aivot.GoverBackend.core.configs.ProviderNameSystemConfigDefinition;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.process.entities.ProcessEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessVersionEntity;
import de.aivot.GoverBackend.process.entities.ProcessVersionEntityId;
import de.aivot.GoverBackend.system.properties.BuildProperties;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ProcessNodeExportService {
    private final ProcessService processService;
    private final ProcessVersionService processVersionService;
    private final ProcessNodeService processNodeService;
    private final ProcessNodeDefinitionService processNodeDefinitionService;
    private final BuildProperties buildProperties;
    private final SystemConfigService systemConfigService;

    public ProcessNodeExportService(ProcessService processService,
                                    ProcessVersionService processVersionService,
                                    ProcessNodeService processNodeService,
                                    ProcessNodeDefinitionService processNodeDefinitionService,
                                    BuildProperties buildProperties,
                                    SystemConfigService systemConfigService) {
        this.processService = processService;
        this.processVersionService = processVersionService;
        this.processNodeService = processNodeService;
        this.processNodeDefinitionService = processNodeDefinitionService;
        this.buildProperties = buildProperties;
        this.systemConfigService = systemConfigService;
    }

    public ProcessNodeExport export(Integer nodeId) throws ResponseException {
        var node = processNodeService
                .retrieve(nodeId)
                .orElseThrow(ResponseException::notFound);

        var process = processService
                .retrieve(node.getProcessId())
                .orElseThrow(ResponseException::notFound);

        var processVersion = processVersionService
                .retrieve(ProcessVersionEntityId.of(process.getId(), node.getProcessVersion()))
                .orElseThrow(ResponseException::notFound);

        var provider = processNodeDefinitionService
                .getProcessNodeDefinition(node.getProcessNodeDefinitionKey(), node.getProcessNodeDefinitionVersion())
                .orElseThrow(() -> ResponseException.badRequest(
                        "Eine Prozesselementdefinition mit dem Schlüssel „%s“ und der Version „%d“ ist nicht verfügbar."
                                .formatted(node.getProcessNodeDefinitionKey(), node.getProcessNodeDefinitionVersion())
                ));

        var cleanedNode = new ProcessNodeEntity()
                .setId(node.getId())
                .setProcessId(node.getProcessId())
                .setProcessVersion(node.getProcessVersion())
                .setName(node.getName())
                .setDescription(node.getDescription())
                .setDataKey(node.getDataKey())
                .setProcessNodeDefinitionKey(node.getProcessNodeDefinitionKey())
                .setProcessNodeDefinitionVersion(node.getProcessNodeDefinitionVersion())
                .setConfiguration(provider.cleanConfigurationForExport(node.getConfiguration()))
                .setOutputMappings(node.getOutputMappings())
                .setTimeLimitDays(node.getTimeLimitDays())
                .setRequirements(node.getRequirements())
                .setNotes(node.getNotes());

        var vendorName = systemConfigService
                .retrieve(ProviderNameSystemConfigDefinition.KEY)
                .getValue();
        if (vendorName == null) {
            vendorName = "Unbekannt";
        }

        return new ProcessNodeExport(
                new ProcessNodeExportData(
                        buildProperties.getBuildVersion(),
                        LocalDateTime.now(),
                        vendorName,
                        process,
                        processVersion,
                        cleanedNode
                ),
                "TODO"
        );
    }

    public record ProcessNodeExport(
            @Nonnull
            @NotNull
            ProcessNodeExportData data,
            @Nullable
            String signature
    ) {
    }

    public record ProcessNodeExportData(
            @Nonnull
            String appVersion,
            @Nonnull
            LocalDateTime exportTimestamp,
            @Nonnull
            String createdByVendor,
            @Nonnull
            @NotNull
            ProcessEntity process,
            @Nonnull
            @NotNull
            ProcessVersionEntity version,
            @Nonnull
            @NotNull
            ProcessNodeEntity node
    ) {
    }
}
