package de.aivot.prosuna.backend.ai.tools;

import de.aivot.prosuna.backend.ai.models.AiProcessChatContext;
import de.aivot.prosuna.backend.ai.models.AiProcessConfigurationChange;
import de.aivot.prosuna.backend.ai.services.AiChatProcessService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class AiChatProcessTools {
    private final AiChatProcessService service;

    public AiChatProcessTools(@Nonnull AiChatProcessService service) { this.service = service; }

    @Nonnull
    @Tool(name = "liste-knotendefinitionen", description = "Knotendefinitionen kompakt suchen. Listen: Offset ab 0, Limit standardmäßig 20, maximal 50.")
    public Object listDefinitions(
            @ToolParam(description = "Suchbegriff", required = false) @Nullable String query,
            @ToolParam(description = "Offset", required = false) @Nullable Integer offset,
            @ToolParam(description = "Limit", required = false) @Nullable Integer limit,
            @Nonnull ToolContext context) {
        return execute(() -> {
            var scope = AiProcessChatContext.from(context);
            return service.listDefinitions(scope, query, offset, limit);
        });
    }

    @Nonnull
    @Tool(name = "hole-knotendefinition", description = "Definition mit Ausgängen, Ausgaben und Beschreibung abrufen. Schlüssel und Version aus der Definitionsliste übernehmen.")
    public Object definition(
            @ToolParam(description = "Definitionsschlüssel", required = true) @Nonnull String key,
            @ToolParam(description = "Definitionsversion", required = true) int version,
            @ToolParam(description = "Zeichenoffset der Beschreibung, Standard 0", required = false) @Nullable Integer descriptionOffset,
            @Nonnull ToolContext context) {
        return execute(() -> {
            var scope = AiProcessChatContext.from(context);
            return service.definition(scope, key, version, descriptionOffset == null ? 0 : descriptionOffset);
        });
    }

    @Nonnull
    @Tool(name = "hole-prozessstruktur", description = "Gespeicherte Knoten und Verbindungen der geöffneten Prozessversion kompakt und seitenweise abrufen.")
    public Object structure(
            @ToolParam(description = "Offset", required = false) @Nullable Integer offset,
            @ToolParam(description = "Limit", required = false) @Nullable Integer limit,
            @Nonnull ToolContext context) {
        return execute(() -> {
            var scope = AiProcessChatContext.from(context);
            return service.structure(scope, offset, limit);
        });
    }

    @Nonnull
    @Tool(name = "hole-prozessknoten", description = "Eigenschaften und Konfigurationsübersicht eines gespeicherten Knotens ohne große Konfigurationswerte abrufen.")
    public Object getNode(
            @ToolParam(description = "Knoten-ID aus der Prozessstruktur", required = true) int nodeId,
            @ToolParam(description = "Optional eine allgemeine Eigenschaft vollständig in Abschnitten lesen", required = false) @Nullable String property,
            @ToolParam(description = "Zeichenoffset für die einzelne Eigenschaft, Standard 0", required = false) @Nullable Integer valueOffset,
            @Nonnull ToolContext context) {
        return execute(() -> {
            var scope = AiProcessChatContext.from(context);
            return service.getNode(scope, nodeId, property, valueOffset == null ? 0 : valueOffset);
        });
    }

    @Nonnull
    @Tool(name = "liste-knotenkonfigurationsfelder", description = "Felder mit Eingabemodi, Zustand und exakten valuePaths abrufen. Pfade mit * beschreiben Vorlagen für Listenzeilen und sind nicht direkt schreibbar.")
    public Object fields(
            @ToolParam(description = "Knoten-ID", required = true) int nodeId,
            @ToolParam(description = "Filter für Feldnamen und Pfade", required = false) @Nullable String query,
            @ToolParam(description = "Offset", required = false) @Nullable Integer offset,
            @ToolParam(description = "Limit", required = false) @Nullable Integer limit,
            @Nonnull ToolContext context) {
        return execute(() -> {
            var scope = AiProcessChatContext.from(context);
            return service.fields(scope, nodeId, query, offset, limit);
        });
    }

    @Nonnull
    @Tool(name = "hole-knotenkonfigurationsfeld", description = "Wertschema, Regeln und aktuellen Wert eines Felds abrufen. Lange Werte in JSON-Textabschnitten: mit nextOffset fortsetzen, Abschnitte vor Verwendung zusammensetzen.")
    public Object field(
            @ToolParam(description = "Knoten-ID", required = true) int nodeId,
            @ToolParam(description = "Exakter valuePath aus der Feldliste", required = true) @Nonnull String valuePath,
            @ToolParam(description = "Zeichenoffset des Wert-JSONs, Standard 0", required = false) @Nullable Integer valueOffset,
            @ToolParam(description = "Zeichenoffset des constraints-JSONs, Standard 0", required = false) @Nullable Integer constraintsOffset,
            @Nonnull ToolContext context) {
        return execute(() -> {
            var scope = AiProcessChatContext.from(context);
            return service.field(scope, nodeId, valuePath, valueOffset == null ? 0 : valueOffset, constraintsOffset == null ? 0 : constraintsOffset);
        });
    }

    @Nonnull
    @Tool(name = "suche-konfigurationsoptionen", description = "Verfügbare Auswahlwerte für ein konkretes Feld suchen. Liefert nur erlaubte Kennungen und Anzeigenamen, keine Geheimniswerte.")
    public Object options(
            @ToolParam(description = "Knoten-ID", required = true) int nodeId,
            @ToolParam(description = "Feldpfad aus der Feldliste", required = true) @Nonnull String valuePath,
            @ToolParam(description = "Suchbegriff", required = false) @Nullable String query,
            @ToolParam(description = "Offset", required = false) @Nullable Integer offset,
            @ToolParam(description = "Limit", required = false) @Nullable Integer limit,
            @Nonnull ToolContext context) {
        return execute(() -> {
            var scope = AiProcessChatContext.from(context);
            return service.options(scope, nodeId, valuePath, query, offset, limit);
        });
    }

    @Nonnull
    @Tool(name = "liste-knotenvariablen", description = "Verfügbare Variablen und ihre Quellen für diesen Knoten ermitteln. Referenzen aus diesen Ergebnissen übernehmen.")
    public Object variables(
            @ToolParam(description = "Knoten-ID", required = true) int nodeId,
            @ToolParam(description = "Suchbegriff", required = false) @Nullable String query,
            @ToolParam(description = "Offset", required = false) @Nullable Integer offset,
            @ToolParam(description = "Limit", required = false) @Nullable Integer limit,
            @Nonnull ToolContext context) {
        return execute(() -> {
            var scope = AiProcessChatContext.from(context);
            return service.variables(scope, nodeId, query, offset, limit);
        });
    }

    @Nonnull
    @Tool(name = "hole-konfigurationshilfe", description = "Gezielte Hilfe abrufen: modes, nocode-operators, nocode-operator, nocode-operands oder javascript.")
    public Object help(
            @ToolParam(description = "Hilfethema", required = true) @Nonnull String topic,
            @ToolParam(description = "Operator-Kennung, Suchbegriff oder JavaScript-Objektname", required = false) @Nullable String key,
            @ToolParam(description = "Offset", required = false) @Nullable Integer offset,
            @ToolParam(description = "Limit", required = false) @Nullable Integer limit,
            @Nonnull ToolContext context) {
        return execute(() -> {
            var scope = AiProcessChatContext.from(context);
            return service.help(scope, topic, key, offset, limit);
        });
    }

    @Nonnull
    @Tool(name = "erstelle-prozessknoten", description = "Knoten in der geöffneten Entwurfsversion mit Definitionsstandardwerten direkt speichern. Liefert ID und Validierungshinweise. Danach Felder nachschlagen und gemeinsam konfigurieren.")
    public Object createNode(
            @ToolParam(description = "Definitionsschlüssel aus liste-knotendefinitionen", required = true) @Nonnull String key,
            @ToolParam(description = "Definitionsversion", required = true) int version,
            @ToolParam(description = "Eindeutiger Datenschlüssel mit 1 bis 32 Zeichen", required = true) @Nonnull String dataKey,
            @ToolParam(description = "Optionaler Anzeigename", required = false) @Nullable String name,
            @Nonnull ToolContext context) {
        return execute(() -> {
            var scope = AiProcessChatContext.from(context);
            return service.createNode(scope, key, version, dataKey, name);
        });
    }

    @Nonnull
    @Tool(name = "aktualisiere-prozessknoten", description = "Knoteneigenschaften und Konfiguration gemeinsam atomar speichern. properties erlaubt name, description, dataKey, outputMappings, timeLimitDays, notes, requirements. configurationChanges enthält valuePath, mode und value: Literal nutzt den Rohwert, Variable eine Referenz, NoCode einen Operanden und LowCode JavaScript-Text. Nicht angegebene Werte bleiben erhalten. removePaths entfernt Einträge; Literal mit value=null setzt ausdrücklich null. Bei Typfehlern wird nichts gespeichert.")
    public Object updateNode(
            @ToolParam(description = "Knoten-ID", required = true) int nodeId,
            @ToolParam(description = "Zu ändernde allgemeine Knoteneigenschaften", required = false) @Nullable Map<String, Object> properties,
            @ToolParam(description = "Geordnete Konfigurationsänderungen mit valuePath, mode und modeabhängigem value", required = false) @Nullable List<AiProcessConfigurationChange> configurationChanges,
            @ToolParam(description = "Ausdrücklich zu entfernende valuePaths", required = false) @Nullable List<String> removePaths,
            @Nonnull ToolContext context) {
        return execute(() -> {
            var scope = AiProcessChatContext.from(context);
            return service.updateNode(scope, nodeId, properties == null ? Map.of() : properties,
                    configurationChanges == null ? List.of() : configurationChanges,
                    removePaths == null ? List.of() : removePaths);
        });
    }

    @Nonnull
    @Tool(name = "loesche-prozessknoten", description = "Knoten und alle seine eingehenden und ausgehenden Verbindungen aus der Entwurfsversion löschen.")
    public Object deleteNode(
            @ToolParam(description = "Zu löschende Knoten-ID", required = true) int nodeId,
            @Nonnull ToolContext context) {
        return execute(() -> {
            var scope = AiProcessChatContext.from(context);
            return service.deleteNode(scope, nodeId);
        });
    }

    @Nonnull
    @Tool(name = "speichere-prozessverbindung", description = "Verbindung direkt speichern. Ohne edgeId neu anlegen; mit edgeId gezielt ändern. Nur deklarierte Ausgänge, höchstens eine Verbindung je Ausgang. Schleifen sind möglich.")
    public Object saveEdge(
            @ToolParam(description = "Vorhandene Verbindungs-ID zum Ändern", required = false) @Nullable Integer edgeId,
            @ToolParam(description = "Quellknoten-ID", required = true) int fromNodeId,
            @ToolParam(description = "Zielknoten-ID", required = true) int toNodeId,
            @ToolParam(description = "Exakter Ausgangsschlüssel aus hole-knotendefinition", required = true) @Nonnull String port,
            @Nonnull ToolContext context) {
        return execute(() -> {
            var scope = AiProcessChatContext.from(context);
            return service.saveEdge(scope, edgeId, fromNodeId, toNodeId, port);
        });
    }

    @Nonnull
    @Tool(name = "loesche-prozessverbindung", description = "Eine gespeicherte Verbindung aus der Entwurfsversion löschen.")
    public Object deleteEdge(
            @ToolParam(description = "Verbindungs-ID", required = true) int edgeId,
            @Nonnull ToolContext context) {
        return execute(() -> {
            var scope = AiProcessChatContext.from(context);
            return service.deleteEdge(scope, edgeId);
        });
    }

    @Nonnull
    @Tool(name = "hole-knotenformular", description = "Eingebettetes Literal-Formular abrufen. Ohne elementPath kompakte Struktur, mit Pfad Eigenschaften eines einzelnen Elements ohne Kinder.")
    public Object form(
            @ToolParam(description = "Knoten-ID", required = true) int nodeId,
            @ToolParam(description = "Pfad zum UI-Definitionsfeld", required = true) @Nonnull String valuePath,
            @ToolParam(description = "Formularpfad, leer für Wurzel; weglassen für Struktur", required = false) @Nullable String elementPath,
            @ToolParam(description = "Optional einzelne Eigenschaft des Formularelements lesen", required = false) @Nullable String property,
            @ToolParam(description = "Zeichenoffset für die einzelne Eigenschaft, Standard 0", required = false) @Nullable Integer valueOffset,
            @ToolParam(description = "Offset", required = false) @Nullable Integer offset,
            @ToolParam(description = "Limit", required = false) @Nullable Integer limit,
            @Nonnull ToolContext context) {
        return execute(() -> {
            var scope = AiProcessChatContext.from(context);
            return service.form(scope, nodeId, valuePath, elementPath, property, valueOffset == null ? 0 : valueOffset, offset, limit);
        });
    }

    @Nonnull
    @Tool(name = "bearbeite-knotenformular", description = "Eingebettetes Literal-Formular direkt am Datenbankknoten bearbeiten: create, update, move, delete. IDs werden serverseitig erstellt. create benötigt type und bei vorhandener Wurzel parentPath. move hängt das Element unter parentPath an. properties konfiguriert Eigenschaften gemeinsam; id, type und children werden durch Strukturaktionen verwaltet. Leerer elementPath bezeichnet die Wurzel.")
    public Object editForm(
            @ToolParam(description = "Knoten-ID", required = true) int nodeId,
            @ToolParam(description = "Pfad zum UI-Definitionsfeld", required = true) @Nonnull String valuePath,
            @ToolParam(description = "create, update, move oder delete", required = true) @Nonnull String operation,
            @ToolParam(description = "Elementpfad, leer für Wurzel", required = true) @Nonnull String elementPath,
            @ToolParam(description = "Elternpfad für create oder move", required = false) @Nullable String parentPath,
            @ToolParam(description = "Numerischer Elementtyp für create", required = false) @Nullable Integer type,
            @ToolParam(description = "Gemeinsam zu setzende Eigenschaften", required = false) @Nullable Map<String, Object> properties,
            @Nonnull ToolContext context) {
        return execute(() -> {
            var scope = AiProcessChatContext.from(context);
            return service.editForm(scope, nodeId, valuePath, operation, elementPath, parentPath, type, properties == null ? Map.of() : properties);
        });
    }

    @Nonnull
    @Tool(name = "pruefe-prozess", description = "Gespeicherte Prozessversion mit der bestehenden Prozessprüfung prüfen. Liefert offene Fehler kompakt; bestätigt keine fachliche Vollständigkeit.")
    public Object validate(
            @ToolParam(description = "Offset der Knotenfehler", required = false) @Nullable Integer offset,
            @ToolParam(description = "Limit", required = false) @Nullable Integer limit,
            @Nonnull ToolContext context) {
        return execute(() -> {
            var scope = AiProcessChatContext.from(context);
            return service.validate(scope, offset, limit);
        });
    }

    private Object execute(Operation operation) {
        try {
            return operation.run();
        } catch (ResponseException exception) {
            if (exception.getStatus().is5xxServerError()) throw new IllegalStateException("Die Prozessaktion ist fehlgeschlagen.", exception);
            return Map.of("saved", false, "error", exception.getMessage());
        } catch (tools.jackson.core.JacksonException exception) {
            return Map.of("saved", false, "error", "Die übergebenen JSON-Werte passen nicht zur erwarteten Struktur.");
        } catch (IllegalArgumentException exception) {
            return Map.of("saved", false, "error", "Die Eingabe ist ungültig. Prüfen Sie Typen, Pfade und Seitengrenzen anhand der Tool-Beschreibungen.");
        }
    }

    @FunctionalInterface
    private interface Operation { Object run() throws ResponseException; }
}
