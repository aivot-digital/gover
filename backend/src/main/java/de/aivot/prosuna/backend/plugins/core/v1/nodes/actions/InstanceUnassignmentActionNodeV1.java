package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultInstanceAssigned;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class InstanceUnassignmentActionNodeV1 implements ProcessNodeDefinition<InstanceUnassignmentActionNodeV1.Config> {
    public static final String NODE_KEY = "unassign_instance";

    private static final String PORT_SUCCESS = "success";

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return CorePlugin.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public String getComponentKey() {
        return NODE_KEY;
    }

    @Nonnull
    @Override
    public String getComponentVersion() {
        return "1.0.0";
    }

    @Nonnull
    @Override
    public String getName() {
        return "Vorgangszuweisung entfernen";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Entfernt die Zuweisung eines Vorgangs und setzt den Prozess fort.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                Entfernt die zentrale Zuweisung des aktuellen Vorgangs. Ist keine Person zugewiesen, bleibt die Zuweisung leer und der Prozess wird fortgesetzt.
                """;
    }

    @Nonnull
    @Override
    public ProcessNodeType getType() {
        return ProcessNodeType.Action;
    }

    @Nonnull
    @Override
    public ProcessNodeExecutionType[] getExecutionTypes() {
        return new ProcessNodeExecutionType[]{ProcessNodeExecutionType.Automatic};
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(new ProcessNodePort(
                PORT_SUCCESS,
                "Zuweisung entfernt",
                "Der Vorgang hat nach der Ausführung keine zugewiesene Person."
        ));
    }

    @Nonnull
    @Override
    public Class<Config> getNodeConfigurationClass() {
        return Config.class;
    }

    @Nonnull
    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<Config> context) {
        var result = ProcessNodeExecutionResultInstanceAssigned.clear().setViaPort(PORT_SUCCESS);
        result.setProcessData(context.getCurrentProcessExecutionData().getProcessData());
        result.setRuntimeData(Map.of());
        result.setNodeData(Map.of());
        return result;
    }

    /** Configuration has no node-specific fields. */
    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class Config {
    }
}
