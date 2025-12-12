package de.aivot.GoverBackend.plugins.s3StoragePlugin.components;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.models.elements.form.input.TextField;
import de.aivot.GoverBackend.elements.models.elements.form.layout.GroupLayout;
import de.aivot.GoverBackend.plugin.models.PluginComponent;
import de.aivot.GoverBackend.process.entities.*;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionResult;
import de.aivot.GoverBackend.process.models.ProcessNodePort;
import de.aivot.GoverBackend.process.models.ProcessNodeProvider;
import de.aivot.GoverBackend.user.entities.UserEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class S3StoragePluginDownloadFileNode implements ProcessNodeProvider, PluginComponent {
    @Override
    public String getParentPluginKey() {
        return "";
    }

    @Nonnull
    @Override
    public String getName() {
        return "";
    }

    @Nonnull
    @Override
    public String getKey() {
        return "";
    }

    @Nonnull
    @Override
    @JsonIgnore
    public GroupLayout getConfigurationLayout(@Nonnull UserEntity user,
                                              @Nonnull ProcessDefinitionEntity processDefinition,
                                              @Nonnull ProcessDefinitionVersionEntity processDefinitionVersion,
                                              @Nullable ProcessDefinitionNodeEntity thisNode) {
        var accessKeyField = new TextField();
        accessKeyField.setId("accessKey");
        accessKeyField.setLabel("Access Key");
        accessKeyField.setHint("The access key for the S3 storage.");

        return new GroupLayout()
                .addChild(accessKeyField);
    }

    @Nonnull
    @Override
    public ProcessNodeType getType() {
        return ProcessNodeType.Action;
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "";
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of();
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessInstanceEntity processInstance,
                                           @Nonnull ProcessDefinitionNodeEntity thisNode,
                                           @Nonnull Map<String, Object> workingData) throws Exception {
        return null;
    }
}
