package de.aivot.prosuna.backend.ai.services;

import de.aivot.prosuna.backend.elements.models.*;
import de.aivot.prosuna.backend.elements.models.elements.*;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.prosuna.backend.elements.models.input.*;
import de.aivot.prosuna.backend.elements.enums.InputMode;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.services.ProcessNodeDefinitionService;
import de.aivot.prosuna.backend.process.services.ProcessNodeService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;
import tools.jackson.core.JsonPointer;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.*;

@Service
public class AiProcessConfigurationService {
    private final JsonMapper mapper;
    private final ProcessNodeService nodes;
    private final ProcessNodeDefinitionService definitions;
    private final AiInputValueSchemaService schemas;

    public AiProcessConfigurationService(@Nonnull JsonMapper mapper, @Nonnull ProcessNodeService nodes,
                                         @Nonnull ProcessNodeDefinitionService definitions, @Nonnull AiInputValueSchemaService schemas) {
        this.mapper = mapper;
        this.nodes = nodes;
        this.definitions = definitions;
        this.schemas = schemas;
    }

    public record Field(@Nonnull BaseInputElement<?> element, @Nonnull String fieldPath,
                        @Nonnull String valuePath, @Nonnull ComputedElementState state, boolean template) {}

    @Nonnull
    public List<Field> fields(@Nonnull ProcessNodeEntity node, @Nonnull UserEntity user) throws ResponseException {
        var provider = definitions.getProcessNodeDefinition(node).orElseThrow(ResponseException::badRequest);
        var layout = nodes.getConfigLayoutElement(node, provider, user);
        var derived = nodes.deriveConfigurationForAuthoring(node, provider, user, node.getConfiguration(), new ElementDerivationOptions());
        var fields = new ArrayList<Field>();
        walk(layout, "", "", mapper.valueToTree(node.getConfiguration()), derived.getElementStates(), false, fields);
        return fields;
    }

    private void walk(BaseElement element, String fieldPath, String valuePrefix, JsonNode values,
                      Map<String, ComputedElementState> states, boolean template, List<Field> fields) {
        var state = states.getOrDefault(element.getId(), new ComputedElementState());
        if (state.getOverride() != null) element = state.getOverride();
        var valuePath = valuePrefix + "/" + escape(element.getId());
        if (element instanceof BaseInputElement<?> input) {
            fields.add(new Field(input, fieldPath, valuePath, state, template));
        }
        if (element instanceof LayoutElement<?> layout && layout.getChildren() != null) {
            if (element instanceof ReplicatingContainerLayoutElement) {
                var rows = values.path(element.getId()).path("value");
                // Templates expose child IDs even before the first authored row exists.
                for (int row = -1; row < (rows.isArray() ? rows.size() : 0); row++) {
                    Map<String, ComputedElementState> childStates = Map.of();
                    if (row >= 0 && state.getSubStates() != null && row < state.getSubStates().size()) {
                        childStates = state.getSubStates().get(row).getStates();
                    }
                    for (int i = 0; i < layout.getChildren().size(); i++) {
                        walk(layout.getChildren().get(i), fieldPath + "/children/" + i,
                                valuePath + "/value/" + (row < 0 ? "*" : row) + "/values",
                                row < 0 ? mapper.createObjectNode() : rows.get(row).path("values"), childStates,
                                template || row < 0, fields);
                    }
                }
            } else {
                for (int i = 0; i < layout.getChildren().size(); i++) {
                    walk(layout.getChildren().get(i), fieldPath + "/children/" + i, valuePrefix, values, states, template, fields);
                }
            }
        }
    }

    @Nonnull
    public Field field(@Nonnull ProcessNodeEntity node, @Nonnull UserEntity user, @Nonnull String valuePath) throws ResponseException {
        return fields(node, user).stream().filter(f -> f.valuePath().equals(valuePath)).findFirst()
                .orElseThrow(() -> ResponseException.badRequest("Das Konfigurationsfeld ist nicht verfügbar. Rufen Sie die Feldliste erneut ab."));
    }

    @Nonnull
    public Map<String, Object> summary(@Nonnull Field field, @Nonnull ProcessNodeEntity node) {
        var result = new LinkedHashMap<String, Object>();
        var input = field.element();
        result.put("id", input.getId());
        result.put("type", input.getType().getKey());
        result.put("label", input.getLabel());
        result.put("fieldPath", field.fieldPath());
        result.put("valuePath", field.valuePath());
        result.put("template", field.template());
        result.put("required", input.getRequired());
        result.put("visible", field.state().getVisible());
        result.put("disabled", field.state().getDisabled());
        result.put("error", field.state().getError());
        result.put("allowedModes", input.getInputModePolicy() == null ? List.of(InputMode.Literal) : input.getInputModePolicy().allowedModes());
        result.put("value", AiToolResults.value(mapper, mapper.valueToTree(node.getConfiguration()).at(field.valuePath()), 0, 240));
        return result;
    }

    @Nonnull
    public Map<String, Object> details(@Nonnull Field field, @Nonnull ProcessNodeEntity node, int valueOffset) {
        return details(field, node, valueOffset, 0);
    }

    @Nonnull
    public Map<String, Object> details(@Nonnull Field field, @Nonnull ProcessNodeEntity node, int valueOffset, int constraintsOffset) {
        var result = summary(field, node);
        result.put("valueSchema", schemas.forElement(field.element()));
        result.put("inputModePolicy", field.element().getInputModePolicy());
        result.put("hint", field.element().getHint());
        var definition = (ObjectNode) mapper.valueToTree(field.element());
        // Functions, nested forms and option lists are fetched separately, never recursively expanded here.
        definition.remove(List.of("children", "options", "value", "validation", "visibility", "override", "metadata"));
        result.put("constraints", AiToolResults.value(mapper, definition, constraintsOffset, 3000));
        result.put("value", AiToolResults.value(mapper, mapper.valueToTree(node.getConfiguration()).at(field.valuePath()), valueOffset, 4000));
        return result;
    }

    @Nonnull
    public AuthoredElementValues patch(@Nonnull ProcessNodeEntity node, @Nonnull UserEntity user,
                                      @Nonnull Map<String, Object> changes, @Nonnull List<String> removals) throws ResponseException {
        var tree = (ObjectNode) mapper.valueToTree(node.getConfiguration());
        if (removals.stream().anyMatch(changes::containsKey)) throw ResponseException.badRequest("Ein Feld darf nicht zugleich gesetzt und entfernt werden.");
        // Resolve sequentially against the candidate so a batch can create rows and then configure their children.
        var candidate = mapper.convertValue(node, ProcessNodeEntity.class);
        for (var path : removals) {
            candidate.setConfiguration(mapper.treeToValue(tree, AuthoredElementValues.class));
            var field = field(candidate, user, path);
            requireConcrete(field);
            parent(tree, path).remove(JsonPointer.compile(path).last().getMatchingProperty());
        }
        for (var change : changes.entrySet()) {
            candidate.setConfiguration(mapper.treeToValue(tree, AuthoredElementValues.class));
            var field = field(candidate, user, change.getKey());
            requireConcrete(field);
            var raw = mapper.valueToTree(change.getValue());
            validateEnvelope(field.element(), raw);
            parent(tree, change.getKey()).set(JsonPointer.compile(change.getKey()).last().getMatchingProperty(), raw);
        }
        return mapper.treeToValue(tree, AuthoredElementValues.class);
    }

    private void requireConcrete(Field field) throws ResponseException {
        if (field.template()) throw ResponseException.badRequest("Vorlagenpfade mit * können nicht gespeichert werden. Legen Sie zuerst eine konkrete Listenzeile an.");
    }

    private ObjectNode parent(ObjectNode root, String path) throws ResponseException {
        var parent = root.at(JsonPointer.compile(path).head());
        if (!(parent instanceof ObjectNode object)) throw ResponseException.badRequest("Der übergeordnete Konfigurationswert ist nicht vorhanden.");
        return object;
    }

    public void validateEnvelope(@Nonnull BaseInputElement<?> input, @Nonnull JsonNode raw) throws ResponseException {
        if (!raw.isObject()) throw ResponseException.badRequest("Ein Konfigurationswert muss ein Objekt mit type und den zugehörigen Modusdaten sein.");
        final AuthoredInputValue value;
        try {
            value = mapper.readerFor(AuthoredInputValue.class).with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(raw);
        } catch (IllegalArgumentException | tools.jackson.core.JacksonException exception) {
            throw ResponseException.badRequest("Der Aufbau des Konfigurationswertes ist ungültig.");
        }
        var modes = input.getInputModePolicy() == null ? List.of(InputMode.Literal) : input.getInputModePolicy().allowedModes();
        if (modes.stream().noneMatch(mode -> mode.name().equals(value.type()))) throw ResponseException.badRequest("Dieser Eingabemodus ist für das Feld nicht erlaubt.");
        if (value instanceof VariableAuthoredInputValue variable
                && (variable.reference() == null || !input.getInputModePolicy().allowedVariableSources().contains(variable.reference().source()))) {
            throw ResponseException.badRequest("Diese Variablenquelle ist für das Feld nicht erlaubt.");
        }
        if (value instanceof LiteralAuthoredInputValue literal && literal.value() != null) {
            var valueType = mapper.constructType(input.getClass()).findSuperType(InputElement.class).containedType(0);
            var rawValue = raw.path("value");
            var cls = valueType.getRawClass();
            boolean valid = cls == String.class ? rawValue.isString()
                    : cls == Boolean.class ? rawValue.isBoolean()
                    : Number.class.isAssignableFrom(cls) ? rawValue.isNumber()
                    : Collection.class.isAssignableFrom(cls) ? rawValue.isArray()
                    : java.time.temporal.TemporalAccessor.class.isAssignableFrom(cls) ? rawValue.isString()
                    : rawValue.isObject();
            if (!valid) throw ResponseException.badRequest("Der JSON-Typ passt nicht zum Eingabefeld.");
            if (!(input instanceof ReplicatingContainerLayoutElement)) {
                var schema = com.networknt.schema.SchemaRegistry.withDefaultDialect(com.networknt.schema.SpecificationVersion.DRAFT_2020_12)
                        .getSchema(schemas.forElement(input));
                if (!schema.validate(rawValue).isEmpty()) throw ResponseException.badRequest("Der Aufbau des Literal-Wertes passt nicht zum Eingabewertschema.");
            }
            if (input instanceof de.aivot.prosuna.backend.elements.models.elements.form.input.UiDefinitionInputElement) {
                try {
                    mapper.treeToValue(rawValue, BaseElement.class);
                } catch (IllegalArgumentException | tools.jackson.core.JacksonException exception) {
                    throw ResponseException.badRequest("Die eingebettete Formularstruktur enthält ungültige Elemente oder Kindelemente.");
                }
            }
            if (input instanceof ReplicatingContainerLayoutElement repeat) {
                var allowed = new HashMap<String, BaseInputElement<?>>();
                collectInputs(repeat, allowed);
                for (var row : rawValue) {
                    if (!row.isObject() || !row.path("values").isObject()) throw ResponseException.badRequest("Listenzeilen benötigen ein values-Objekt.");
                    for (var entry : row.path("values").properties()) {
                        var child = allowed.get(entry.getKey());
                        if (child == null) throw ResponseException.badRequest("Die Listenzeile enthält ein unbekanntes Eingabefeld.");
                        validateEnvelope(child, entry.getValue());
                    }
                }
            }
        }
    }

    private void collectInputs(LayoutElement<?> layout, Map<String, BaseInputElement<?>> result) {
        if (layout.getChildren() == null) return;
        for (var child : layout.getChildren()) {
            if (child instanceof BaseInputElement<?> input) result.put(input.getId(), input);
            if (child instanceof LayoutElement<?> nested && !(child instanceof ReplicatingContainerLayoutElement)) collectInputs(nested, result);
        }
    }

    public static String escape(String id) { return id.replace("~", "~0").replace("/", "~1"); }
}
