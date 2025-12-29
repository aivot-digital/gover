package de.aivot.GoverBackend.plugins.form.v1.nodes;

import de.aivot.GoverBackend.plugin.models.PluginComponent;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionResult;
import de.aivot.GoverBackend.process.models.ProcessNodePort;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinition;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class FormActionNode implements ProcessNodeDefinition, PluginComponent {
    @Nonnull
    @Override
    public Integer getVersion() {
        return 0;
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return "";
    }

    @Nonnull
    @Override
    public String getKey() {
        return "";
    }

    @Nonnull
    @Override
    public ProcessNodeType getType() {
        return null;
    }

    @Nonnull
    @Override
    public String getName() {
        return "";
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
    public ProcessNodeExecutionResult init(@Nonnull ProcessInstanceEntity processInstance, @Nonnull ProcessNodeEntity thisNode, @Nonnull Map<String, Object> data) throws Exception {
        return null;
    }
}
