package de.aivot.GoverBackend.process.services;

import de.aivot.GoverBackend.config.services.SystemConfigService;
import de.aivot.GoverBackend.core.configs.ProviderNameSystemConfigDefinition;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.process.entities.*;
import de.aivot.GoverBackend.process.enums.ProcessVersionStatus;
import de.aivot.GoverBackend.process.filters.ProcessDefinitionEdgeFilter;
import de.aivot.GoverBackend.process.filters.ProcessNodeFilter;
import de.aivot.GoverBackend.system.properties.BuildProperties;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ProcessExportService {
    private final ProcessService processDefinitionService;
    private final ProcessVersionService processDefinitionVersionService;
    private final ProcessNodeService processDefinitionNodeService;
    private final ProcessEdgeService processDefinitionEdgeService;
    private final ProcessNodeDefinitionService processNodeProviderService;
    private final BuildProperties buildProperties;
    private final SystemConfigService systemConfigService;

    public ProcessExportService(ProcessService processDefinitionService,
                                ProcessVersionService processDefinitionVersionService,
                                ProcessNodeService processDefinitionNodeService,
                                ProcessEdgeService processDefinitionEdgeService,
                                ProcessNodeDefinitionService processNodeProviderService,
                                BuildProperties buildProperties, SystemConfigService systemConfigService) {
        this.processDefinitionService = processDefinitionService;
        this.processDefinitionVersionService = processDefinitionVersionService;
        this.processDefinitionNodeService = processDefinitionNodeService;
        this.processDefinitionEdgeService = processDefinitionEdgeService;
        this.processNodeProviderService = processNodeProviderService;
        this.buildProperties = buildProperties;
        this.systemConfigService = systemConfigService;
    }

    public ProcessExport export(Integer processId, Integer version) throws ResponseException {
        var processDefinition = processDefinitionService
                .retrieve(processId)
                .orElseThrow(ResponseException::notFound);

        cleanProcessDataForExport(processDefinition);

        var processVersion = processDefinitionVersionService
                .retrieve(ProcessVersionEntityId.of(processId, version))
                .orElseThrow(ResponseException::notFound);

        cleanProcessVersionDataForExport(processVersion);

        var nodes = processDefinitionNodeService
                .list(
                        ProcessNodeFilter
                                .create()
                                .setProcessId(processId)
                                .setProcessVersion(version)
                )
                .stream()
                .map((node) -> {
                    var nodeProvider = processNodeProviderService
                            .getProcessNodeDefinition(node.getProcessNodeDefinitionKey(), node.getProcessNodeDefinitionVersion())
                            .orElseThrow(() -> new RuntimeException("Eine Prozesselementdefinition mit dem Schlüssel „%s“ und der Version „%d“ konnte nicht gefunden werden."
                                    .formatted(node.getProcessNodeDefinitionKey(), node.getProcessNodeDefinitionVersion())));

                    var clonedConfiguration = node
                            .getConfiguration()
                            .clone();

                    var cleanedConfiguration = nodeProvider
                            .cleanConfigurationForExport(clonedConfiguration);

                    return new ProcessNodeEntity(
                            node.getId(), // id,
                            node.getProcessId(), // processId
                            node.getProcessVersion(), // processVersion
                            node.getName(), // name
                            node.getDescription(), // description
                            node.getDataKey(), // dataKey
                            node.getProcessNodeDefinitionKey(), // processNodeDefinitionKey
                            node.getProcessNodeDefinitionVersion(), // processNodeDefinitionVersion
                            cleanedConfiguration, // configuration
                            node.getOutputMappings(), // outputMappings
                            null, // timeLimitDays
                            null, // requirements
                            null, // notes
                            false // savedWithErrors
                    );
                })
                .toList();

        var edges = processDefinitionEdgeService
                .list(
                        ProcessDefinitionEdgeFilter
                                .create()
                                .setProcessDefinitionId(processId)
                                .setProcessDefinitionVersion(version)
                )
                .stream()
                .toList();

        var vendorName = systemConfigService
                .retrieve(ProviderNameSystemConfigDefinition.KEY)
                .getValue();
        if (vendorName == null) {
            vendorName = "Unbekannt";
        }

        return new ProcessExport(
                buildProperties.getBuildVersion(),
                buildProperties.getBuildNumber(),
                Instant.now(),
                vendorName,
                processDefinition,
                processVersion,
                nodes,
                edges
        );
    }

    private static void cleanProcessVersionDataForExport(ProcessVersionEntity processVersion) {
        processVersion.setPublished(null);
        processVersion.setRevoked(null);
        processVersion.setStatus(ProcessVersionStatus.Drafted);
    }

    private static void cleanProcessDataForExport(ProcessEntity processDefinition) {
        processDefinition.setAccessKey(null);
        processDefinition.setDepartmentId(null);
        processDefinition.setVersionCount(null);
        processDefinition.setPublishedVersion(null);
        processDefinition.setDraftedVersion(null);
        processDefinition.setInternalTitle(null);
    }

    public record ProcessExport(
            @Nonnull
            String appVersion,
            @Nonnull
            String appBuildNumber,
            @Nonnull
            Instant exportTimestamp,
            @Nonnull
            String createdByVendor,
            @NotNull
            @Nonnull
            ProcessEntity process,
            @NotNull
            @Nonnull
            ProcessVersionEntity version,
            @NotNull
            @Nonnull
            List<ProcessNodeEntity> nodes,
            @NotNull
            @Nonnull
            List<ProcessEdgeEntity> edges
    ) {
    }
}
