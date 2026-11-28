package de.aivot.prosuna.backend.process.models;

import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Shared definitions for variable suggestions and the process data structure overview.
 */
public final class ProcessSystemDataFields {
    private ProcessSystemDataFields() {
    }

    public record Field(@Nonnull String path, @Nonnull String label, @Nonnull String description,
                        boolean unexpandedStructure) {
    }

    public static final List<Field> PROTECTED = List.of(
            new Field("processInstanceId", "Vorgangs-ID", "Interne ID des Vorgangs.", false),
            new Field("accessKey", "Zugriffsschlüssel", "Zugriffsschlüssel des Vorgangs.", false),
            new Field("caseNumber", "Vorgangskennung", "Von Prosuna vergebene Kennung des Vorgangs.", false),
            new Field("started", "Startzeitpunkt", "Zeitpunkt, zu dem der Vorgang gestartet wurde.", false),
            new Field("initialPayload", "Initiale Vorgangsdaten", "Daten, mit denen der Vorgang gestartet wurde.", true),
            new Field("assignedFileNumbers", "Zugeordnete Aktenzeichen", "Dem Vorgang zugeordnete Aktenzeichen.", true),
            new Field("identities", "Identitäten", "Identitäten und ihre Angaben im Vorgang.", true),
            new Field("assignedUserId", "Zuständige Person", "Konto-ID der für den Vorgang zuständigen Person.", false),
            new Field("initialNodeDataKey", "Datenschlüssel des Startelements", "Datenschlüssel des Prozesselements, das den Vorgang gestartet hat.", false),
            new Field("previousNodeDataKey", "Datenschlüssel des vorherigen Elements", "Datenschlüssel des vorherigen Prozesselements.", false),
            new Field("attachmentSets", "Anlagensätze", "Anlagensätze und enthaltene Anlagen des Vorgangs.", true),
            new Field("taskMetadata", "Elementmetadaten", "Angaben zur Ausführung der Prozesselemente, gruppiert nach ihrem Datenschlüssel.", false),
            new Field("currentTaskId", "Aktuelle Aufgaben-ID", "ID der aktuell ausgeführten Aufgabe.", false)
    );

    public static final List<Field> ELEMENT_METADATA = List.of(
            new Field("nodeId", "Element-ID", "ID des Prozesselements.", false),
            new Field("taskId", "Aufgaben-ID", "ID der Aufgabe zu diesem Prozesselement.", false),
            new Field("assignedUserId", "Zuständige Person", "Konto-ID der für diese Aufgabe zuständigen Person.", false),
            new Field("started", "Startzeitpunkt", "Startzeitpunkt der Aufgabe.", false),
            new Field("updated", "Änderungszeitpunkt", "Zeitpunkt der letzten Änderung an der Aufgabe.", false),
            new Field("finished", "Abschlusszeitpunkt", "Zeitpunkt, zu dem die Aufgabe abgeschlossen wurde.", false),
            new Field("runtime", "Laufzeit", "Laufzeit der Aufgabe.", false),
            new Field("previousProcessNodeId", "Vorherige Element-ID", "ID des vorherigen Prozesselements.", false),
            new Field("previousProcessNodePortKey", "Vorheriger Ausgangsport", "Ausgangsport, über den die Aufgabe erreicht wurde.", false),
            new Field("previousProcessInstanceTaskId", "Vorherige Aufgaben-ID", "ID der vorherigen Aufgabe.", false)
    );
}
