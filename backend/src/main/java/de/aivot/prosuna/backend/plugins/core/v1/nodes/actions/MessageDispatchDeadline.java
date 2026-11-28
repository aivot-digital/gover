package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import de.aivot.prosuna.backend.process.models.ProcessNodeDeadlineDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;

/** Staff phase shared by the nodes whose message is prepared and dispatched manually. */
final class MessageDispatchDeadline {
    static final ProcessNodeDeadlineDefinition DEFINITION = new ProcessNodeDeadlineDefinition(
            "messageDispatch", "Versand der Nachricht",
            "Zeit ab Zuweisung der Mitarbeitendenaufgabe für die Vorbereitung und den Versand der Nachricht.",
            new ProcessNodePort("message_dispatch_overdue", "Versandfrist überschritten",
                    "Der Vorgang wird hier fortgesetzt, wenn die Versandfrist überschritten und die Mitarbeitendenaufgabe automatisch beendet wurde."));

    private MessageDispatchDeadline() {
    }
}
