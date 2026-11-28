package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionSummaryContext;
import de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultInstanceAssigned;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.detail;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.user;

@Component
public class InstanceUnassignmentActionNodeV1 implements ProcessNodeDefinition<InstanceUnassignmentActionNodeV1.Config> {
    public static final String NODE_KEY = "unassign_instance";

    private static final String PORT_SUCCESS = "success";

    @Nonnull
    @Override
    public String generateExecutionSummary(@Nonnull ProcessNodeExecutionSummaryContext<InstanceUnassignmentActionNodeV1.Config> context) {
        var summary = new ExecutionSummaryMarkdown(context);
        var previousId = summary.metadata(ExecutionSummaryMarkdown.PREVIOUS_ASSIGNED_USER_ID);
        var markdown = previousId == null ? "Dem Vorgang ist weiterhin keine Person zugewiesen."
                : "Die Zuweisung des Vorgangs an "
                + user(previousId, summary.metadata(ExecutionSummaryMarkdown.PREVIOUS_ASSIGNED_USER_NAME)) + " wurde automatisch aufgehoben.";
        return markdown + detail("Kurzbeschreibung", context.thisNode().getDescription());
    }

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
        return "Hebt die Vorgangszuweisung automatisch auf und setzt den Vorgang ohne zuständige Person fort.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie dieses Prozesselement?

                Verwenden Sie dieses Prozesselement, wenn die bisherige Zuständigkeit für einen Vorgang enden soll, zum Beispiel nach dem Abschluss einer Bearbeitungsphase. Prosuna hebt die Vorgangszuweisung automatisch auf.

                # Was richten Sie ein?

                Das Element hat keine eigenen Einstellungen. Verbinden Sie den Ausgang „Keine Person zugewiesen“ mit dem nächsten Schritt. In der Kurzbeschreibung können Sie festhalten, warum die bisherige Zuständigkeit an dieser Stelle endet.

                # Was passiert bei der Ausführung?

                Prosuna entfernt die bestehende Zuweisung der zuständigen Person vom Vorgang. Danach wird der Vorgang über „Keine Person zugewiesen“ fortgesetzt.

                Ist dem Vorgang bereits keine Person zugewiesen, bleibt dieser Zustand bestehen. Der Schritt wird trotzdem erfolgreich abgeschlossen und über denselben Ausgang fortgesetzt.

                # Welches Ergebnis steht danach bereit?

                Dem Vorgang ist keine zuständige Person mehr zugewiesen. Bestehende Mitarbeitendenaufgaben behalten ihre eigene Zuweisung. Die Vorgangsdaten werden unverändert an den nächsten Schritt weitergegeben; das Element stellt keine eigenen Ausgangsdaten bereit.

                Die Zusammenfassung im Vorgangsverlauf nennt die Person, deren Vorgangszuweisung aufgehoben wurde, oder zeigt, dass weiterhin keine Person zugewiesen ist. Eine hinterlegte Kurzbeschreibung wird ebenfalls angezeigt.

                # Wie sieht ein Beispiel aus?

                Mara Beispiel ist für einen Vorgang zuständig. Nach dem Abschluss einer Bearbeitungsphase soll diese Zuständigkeit enden. Fügen Sie an dieser Stelle „Vorgangszuweisung entfernen“ ein.

                Nach der Ausführung hat der Vorgang keine zuständige Person mehr. Noch offene Mitarbeitendenaufgaben bleiben ihren bisherigen Personen zugewiesen. Ein späteres Element „Vorgang zuweisen“ kann die Zuständigkeit für die nächste Bearbeitungsphase festlegen.

                # Was ist zu beachten?

                Bei einer bestehenden Zuweisung versucht Prosuna, die bisher zuständige Person per E-Mail über deren Ende zu informieren. Ein Versandfehler wird im technischen Protokoll festgehalten. War dem Vorgang bereits keine Person zugewiesen, wird keine solche Benachrichtigung versendet.

                Für eine spätere neue Zuständigkeit verwenden Sie „Vorgang zuweisen“. Dieses Element hebt die Zuweisung am Vorgang auf; die Zuweisungen einzelner Mitarbeitendenaufgaben werden dadurch nicht geändert.
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
    public List<ProcessNodePort> getPorts(@Nonnull Config configuration) {
        return List.of(new ProcessNodePort(
                PORT_SUCCESS,
                "Keine Person zugewiesen",
                "Der Vorgang wird hier ohne zugewiesene Person fortgesetzt."
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
