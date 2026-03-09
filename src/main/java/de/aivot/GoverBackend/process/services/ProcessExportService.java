package de.aivot.GoverBackend.process.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.GoverBackend.config.services.SystemConfigService;
import de.aivot.GoverBackend.core.configs.ProviderNameSystemConfigDefinition;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.process.entities.*;
import de.aivot.GoverBackend.process.filters.ProcessDefinitionEdgeFilter;
import de.aivot.GoverBackend.process.filters.ProcessNodeFilter;
import de.aivot.GoverBackend.system.properties.BuildProperties;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

        var processVersion = processDefinitionVersionService
                .retrieve(ProcessVersionEntityId.of(processId, version))
                .orElseThrow(ResponseException::notFound);

        var nodes = processDefinitionNodeService
                .list(
                        ProcessNodeFilter
                                .create()
                                .setProcessId(processId)
                                .setProcessVersion(version)
                )
                .stream()
                .peek((node) -> {
                    var nodeProvider = processNodeProviderService
                            .getProcessNodeDefinition(node.getProcessNodeDefinitionKey(), node.getProcessNodeDefinitionVersion())
                            .orElseThrow(() -> new RuntimeException("Eine Prozesselementdefinition mit dem Schlüssel „%s“ und der Version „%d“ konnte nicht gefunden werden."
                                    .formatted(node.getProcessNodeDefinitionKey(), node.getProcessNodeDefinitionVersion())));

                    var cleanedNodeData = nodeProvider
                            .cleanConfigurationForExport(node.getConfiguration());

                    node.setConfiguration(cleanedNodeData);
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

        var exportData = new ProcessExportData(
                buildProperties.getBuildVersion(),
                LocalDateTime.now(),
                vendorName,
                processDefinition,
                processVersion,
                nodes,
                edges
        );

        /*
        String exportDataString;
        try {
            exportDataString = exportData.toJSONString();
        } catch (JsonProcessingException e) {
            throw ResponseException.internalServerError("Failed to serialize process export data", e);
        }

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(1024);
        return kpg.genKeyPair();

        KeyPair keyPair = getKeyPair();

        byte[] data = "test".getBytes("UTF8");

        Signature sig = Signature.getInstance("SHA1WithRSA");
        sig.initSign(keyPair.getPrivate());
        sig.update(data);
        byte[] signatureBytes = sig.sign();

        var sig = new BASE64Encoder().encode(signatureBytes);

        sig.initVerify(keyPair.getPublic());
        sig.update(data);

        System.out.println(sig.verify(signatureBytes));
         */


        return new ProcessExport(
                exportData,
                "TODO"
        );
    }

    public record ProcessExport(
            @Nonnull
            @NotNull
            ProcessExportData data,
            @Nullable
            String signature
    ) {
    }

    public record ProcessExportData(
            @Nonnull
            String appVersion,
            @Nonnull
            LocalDateTime exportTimestamp,
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
        public String toJSONString() throws JsonProcessingException {
            return ObjectMapperFactory
                    .getInstance()
                    .writeValueAsString(this);
        }
    }
}
