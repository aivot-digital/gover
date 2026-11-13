package de.aivot.prosuna.backend.ai.services;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.LayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.UiDefinitionInputElement;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;
import tools.jackson.core.JsonPointer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.*;

@Service
public class AiProcessFormService {
    private final JsonMapper mapper;

    public AiProcessFormService(@Nonnull JsonMapper mapper) { this.mapper = mapper; }

    private UiDefinitionInputElement requireField(AiProcessConfigurationService.Field field) throws ResponseException {
        if (field.template() || !(field.element() instanceof UiDefinitionInputElement ui)) throw ResponseException.badRequest("Der Wertpfad muss auf ein konkretes UI-Definitionsfeld zeigen.");
        return ui;
    }

    private JsonNode root(ProcessNodeEntity node, AiProcessConfigurationService.Field field) throws ResponseException {
        requireField(field);
        var envelope = mapper.valueToTree(node.getConfiguration()).at(field.valuePath());
        if (envelope.isMissingNode() || envelope.isNull()) return mapper.nullNode();
        if (!"Literal".equals(envelope.path("type").asText())) throw ResponseException.badRequest("Ein eingebettetes Formular kann nur im Literal-Modus bearbeitet werden.");
        return envelope.path("value");
    }

    @Nonnull
    public Object read(@Nonnull ProcessNodeEntity node, @Nonnull AiProcessConfigurationService.Field field,
                       @Nullable String path, @Nullable Integer offset, @Nullable Integer limit) throws ResponseException {
        return read(node, field, path, null, 0, offset, limit);
    }

    @Nonnull
    public Object read(@Nonnull ProcessNodeEntity node, @Nonnull AiProcessConfigurationService.Field field,
                       @Nullable String path, @Nullable String property, int valueOffset,
                       @Nullable Integer offset, @Nullable Integer limit) throws ResponseException {
        var root = root(node, field);
        if (root.isNull() || root.isMissingNode()) return Map.of("exists", false);
        if (path != null) {
            var element = element(root, path).deepCopy();
            element.remove("children");
            if (property != null) {
                if (!element.has(property)) throw ResponseException.badRequest("Diese Eigenschaft ist am Formularelement nicht vorhanden.");
                return AiToolResults.value(mapper, element.get(property), valueOffset, 4000);
            }
            var properties = new ArrayList<Map<String, Object>>();
            element.properties().forEach(e -> {
                if (!e.getValue().isNull()) properties.add(Map.of("property", e.getKey(), "value", AiToolResults.value(mapper, e.getValue(), 0, 500)));
            });
            return AiToolResults.page(properties, offset, limit);
        }
        var entries = new ArrayList<Map<String, Object>>();
        tree(root, "", entries);
        return AiToolResults.page(entries, offset, limit);
    }

    private void tree(JsonNode root, String path, List<Map<String, Object>> entries) {
        entries.add(Map.of("path", path, "id", root.path("id").asText(), "type", root.path("type").asInt(),
                "label", root.path("label").asText(root.path("title").asText(root.path("name").asText("")))));
        var children = root.path("children");
        for (int i = 0; i < children.size(); i++) tree(children.get(i), path + "/children/" + i, entries);
    }

    @Nonnull
    public Object edit(@Nonnull ProcessNodeEntity node, @Nonnull AiProcessConfigurationService.Field field,
                       @Nonnull String operation, @Nonnull String path, @Nullable String parentPath,
                       @Nullable Integer typeKey, @Nonnull Map<String, Object> properties) throws ResponseException {
        var ui = requireField(field);
        var root = root(node, field).deepCopy();
        String resultPath = path;
        ObjectNode affected;
        if (operation.equals("create")) {
            if (typeKey == null) throw ResponseException.badRequest("Für ein neues Formularelement muss ein Typ angegeben werden.");
            try {
                var type = ElementType.findElement(typeKey).orElseThrow(ResponseException::badRequest);
                affected = (ObjectNode) mapper.valueToTree(ElementType.getElementClass(type));
            } catch (de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException exception) {
                throw ResponseException.badRequest("Der Formularelementtyp wird nicht unterstützt.");
            }
            patch(affected, properties);
            if (root.isNull() || root.isMissingNode()) {
                if (!path.isEmpty() || parentPath != null && !parentPath.isEmpty()) throw ResponseException.badRequest("Legen Sie zuerst das Wurzelelement an.");
                root = affected;
                resultPath = "";
            } else {
                if (parentPath == null) throw ResponseException.badRequest("Ein Pfad zum Elternelement ist erforderlich.");
                var children = children(element(root, parentPath));
                resultPath = parentPath + "/children/" + children.size();
                children.add(affected);
            }
        } else {
            affected = element(root, path);
            switch (operation) {
                case "update" -> patch(affected, properties);
                case "delete" -> {
                    if (path.isEmpty()) root = mapper.nullNode();
                    else remove(root, path);
                }
                case "move" -> {
                    if (path.isEmpty() || parentPath == null || parentPath.equals(path) || parentPath.startsWith(path + "/")) throw ResponseException.badRequest("Ein Element kann nicht in sich selbst oder seine Kindelemente verschoben werden.");
                    var parent = element(root, parentPath);
                    var children = children(parent);
                    remove(root, path);
                    children.add(affected);
                    var entries = new ArrayList<Map<String, Object>>();
                    tree(root, "", entries);
                    resultPath = (String) entries.stream().filter(e -> e.get("id").equals(affected.path("id").asText())).findFirst().orElseThrow().get("path");
                }
                default -> throw ResponseException.badRequest("Die Formularoperation wird nicht unterstützt. Erlaubt sind create, update, move und delete.");
            }
        }
        if (!root.isNull()) {
            var validated = mapper.treeToValue(root, BaseElement.class);
            if (ui.getElementType() != null && validated.getType() != ui.getElementType()) throw ResponseException.badRequest("Der Typ des Wurzelelements passt nicht zum Konfigurationsfeld.");
        }
        var configuration = (ObjectNode) mapper.valueToTree(node.getConfiguration());
        var pointer = JsonPointer.compile(field.valuePath());
        var parent = configuration.at(pointer.head());
        if (!(parent instanceof ObjectNode object)) throw ResponseException.badRequest("Der Konfigurationspfad ist nicht mehr verfügbar.");
        object.set(pointer.last().getMatchingProperty(), mapper.createObjectNode().put("type", "Literal").set("value", root));
        node.setConfiguration(mapper.treeToValue(configuration, AuthoredElementValues.class));
        return Map.of("operation", operation, "id", affected.path("id").asText(), "path", resultPath);
    }

    private void patch(ObjectNode element, Map<String, Object> properties) throws ResponseException {
        if (properties.keySet().stream().anyMatch(k -> k == null || k.isBlank() || Set.of("id", "type", "children").contains(k))) throw ResponseException.badRequest("id, type und children werden über die Strukturaktionen verwaltet.");
        properties.forEach((key, value) -> element.set(key, mapper.valueToTree(value)));
    }

    private ObjectNode element(JsonNode root, String path) throws ResponseException {
        if (!path.matches("(?:/children/(?:0|[1-9][0-9]*))*")) throw ResponseException.badRequest("Der Formularpfad ist ungültig.");
        var element = root.at(path);
        if (!(element instanceof ObjectNode object) || !object.has("type")) throw ResponseException.badRequest("Das Formularelement ist nicht mehr verfügbar.");
        return object;
    }

    private ArrayNode children(ObjectNode parent) throws ResponseException {
        if (!(mapper.treeToValue(parent, BaseElement.class) instanceof LayoutElement<?>)) throw ResponseException.badRequest("Das Elternelement muss ein Layout sein.");
        return parent.get("children") instanceof ArrayNode array ? array : parent.putArray("children");
    }

    private void remove(JsonNode root, String path) {
        var pointer = JsonPointer.compile(path);
        ((ArrayNode) root.at(pointer.head())).remove(pointer.last().getMatchingIndex());
    }
}
