package de.aivot.prosuna.backend.ai.services;

import de.aivot.prosuna.backend.ai.models.AiProcessConfigurationChange;
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
import tools.jackson.databind.JavaType;
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

    public record ConfigurationError(@Nonnull String code, @Nonnull String valuePath, @Nullable String mode,
                                     @Nonnull String message, @Nonnull List<String> expectedJsonTypes,
                                     @Nullable String actualJsonType, @Nonnull List<String> violations) {}

    public record PatchResult(@Nonnull AuthoredElementValues configuration,
                              @Nonnull List<ConfigurationError> errors) {
        public boolean valid() { return errors.isEmpty(); }
    }

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
        result.put("literalValueType", compactType(inputValueType(input)));
        result.put("currentValue", AiToolResults.value(mapper, currentValue(node, field.valuePath()), 0, 240));
        return result;
    }

    @Nonnull
    public Map<String, Object> details(@Nonnull Field field, @Nonnull ProcessNodeEntity node, int valueOffset) {
        return details(field, node, valueOffset, 0);
    }

    @Nonnull
    public Map<String, Object> details(@Nonnull Field field, @Nonnull ProcessNodeEntity node, int valueOffset, int constraintsOffset) {
        var result = summary(field, node);
        result.put("literalValueSchema", schemas.forElement(field.element()));
        result.put("writeContract", writeContract(field));
        result.put("inputModePolicy", field.element().getInputModePolicy());
        result.put("hint", field.element().getHint());
        var definition = (ObjectNode) mapper.valueToTree(field.element());
        // Functions, nested forms and option lists are fetched separately, never recursively expanded here.
        definition.remove(List.of("children", "options", "value", "validation", "visibility", "override", "metadata"));
        result.put("constraints", AiToolResults.value(mapper, definition, constraintsOffset, 3000));
        result.put("currentValue", AiToolResults.value(mapper, currentValue(node, field.valuePath()), valueOffset, 4000));
        return result;
    }

    @Nonnull
    public PatchResult patch(@Nonnull ProcessNodeEntity node, @Nonnull UserEntity user,
                             @Nonnull List<AiProcessConfigurationChange> changes,
                             @Nonnull List<String> removals) throws ResponseException {
        var tree = (ObjectNode) mapper.valueToTree(node.getConfiguration());
        var candidate = mapper.convertValue(node, ProcessNodeEntity.class);
        var errors = new ArrayList<ConfigurationError>();
        var removalPaths = new HashSet<String>();
        for (var path : removals) {
            if (path == null || path.isBlank()) {
                errors.add(error("INVALID_PATH", Objects.requireNonNullElse(path, ""), null,
                        "Der valuePath darf nicht leer sein."));
                continue;
            }
            if (!removalPaths.add(path)) {
                errors.add(error("DUPLICATE_PATH", path, null,
                        "Der valuePath ist in removePaths mehrfach enthalten."));
                continue;
            }
            candidate.setConfiguration(mapper.treeToValue(tree, AuthoredElementValues.class));
            try {
                var field = field(candidate, user, path);
                requireConcrete(field);
                parent(tree, path).remove(JsonPointer.compile(path).last().getMatchingProperty());
            } catch (ResponseException | IllegalArgumentException exception) {
                errors.add(error("FIELD_NOT_AVAILABLE", path, null, exception.getMessage()));
            }
        }

        // Resolve sequentially against the candidate so a batch can create rows and then configure their children.
        var changedPaths = new HashSet<String>();
        for (var change : changes) {
            var path = change == null ? null : change.valuePath();
            var mode = change == null ? null : change.mode();
            if (path == null || path.isBlank()) {
                errors.add(error("INVALID_PATH", Objects.requireNonNullElse(path, ""), mode,
                        "Der valuePath darf nicht leer sein."));
                continue;
            }
            if (!changedPaths.add(path)) {
                errors.add(error("DUPLICATE_PATH", path, mode,
                        "Der valuePath ist in configurationChanges mehrfach enthalten."));
                continue;
            }
            if (removalPaths.contains(path)) {
                errors.add(error("SET_AND_REMOVE", path, mode,
                        "Ein Feld darf nicht zugleich gesetzt und entfernt werden."));
                continue;
            }
            if (mode == null) {
                errors.add(error("UNKNOWN_MODE", path, null,
                        "Für die Konfigurationsänderung fehlt der Eingabemodus."));
                continue;
            }
            candidate.setConfiguration(mapper.treeToValue(tree, AuthoredElementValues.class));
            try {
                var field = field(candidate, user, path);
                requireConcrete(field);
                var raw = envelope(change);
                validateEnvelopeDetailed(field.element(), raw);
                parent(tree, path).set(JsonPointer.compile(path).last().getMatchingProperty(), raw);
            } catch (ValueValidationFailure failure) {
                errors.add(new ConfigurationError(failure.code, path, mode.name(), failure.getMessage(),
                        failure.expectedJsonTypes, failure.actualJsonType, failure.violations));
            } catch (ResponseException | IllegalArgumentException exception) {
                errors.add(error("FIELD_NOT_AVAILABLE", path, mode, exception.getMessage()));
            }
        }
        return new PatchResult(mapper.treeToValue(tree, AuthoredElementValues.class), List.copyOf(errors));
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
        try {
            validateEnvelopeDetailed(input, raw);
        } catch (ValueValidationFailure failure) {
            throw ResponseException.badRequest(failure.getMessage());
        }
    }

    private void validateEnvelopeDetailed(BaseInputElement<?> input, JsonNode raw) {
        if (!raw.isObject()) throw failure("INVALID_MODE_VALUE",
                "Der Konfigurationswert hat keine gültige Modusstruktur.", List.of(), jsonType(raw));
        final AuthoredInputValue value;
        try {
            value = mapper.readerFor(AuthoredInputValue.class).with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(raw);
        } catch (IllegalArgumentException | tools.jackson.core.JacksonException exception) {
            throw failure("INVALID_MODE_VALUE", "Der Wert passt nicht zur Struktur des Eingabemodus.",
                    List.of(), jsonType(raw));
        }
        var modes = input.getInputModePolicy() == null ? List.of(InputMode.Literal) : input.getInputModePolicy().allowedModes();
        if (modes.stream().noneMatch(mode -> mode.name().equals(value.type()))) {
            throw new ValueValidationFailure("MODE_NOT_ALLOWED", "Dieser Eingabemodus ist für das Feld nicht erlaubt.",
                    modes.stream().map(Enum::name).toList(), null, List.of());
        }
        if (value instanceof VariableAuthoredInputValue variable
                && (variable.reference() == null || !input.getInputModePolicy().allowedVariableSources().contains(variable.reference().source()))) {
            throw new ValueValidationFailure("VARIABLE_SOURCE_NOT_ALLOWED", "Diese Variablenquelle ist für das Feld nicht erlaubt.",
                    input.getInputModePolicy().allowedVariableSources().stream().map(Enum::name).toList(), null, List.of());
        }
        if (value instanceof LiteralAuthoredInputValue literal && literal.value() != null) {
            var rawValue = raw.path("value");
            var valueType = inputValueType(input);
            var cls = valueType.getRawClass();
            boolean valid = cls == String.class ? rawValue.isString()
                    : cls == Boolean.class ? rawValue.isBoolean()
                    : Number.class.isAssignableFrom(cls) ? rawValue.isNumber()
                    : Collection.class.isAssignableFrom(cls) ? rawValue.isArray()
                    : java.time.temporal.TemporalAccessor.class.isAssignableFrom(cls) ? rawValue.isString()
                    : rawValue.isObject();
            var expectedTypes = schemaTypes(schemas.forElement(input));
            if (!valid) throw failure("INVALID_LITERAL_TYPE", "Der JSON-Typ passt nicht zum Eingabefeld.",
                    expectedTypes, jsonType(rawValue));
            if (!(input instanceof ReplicatingContainerLayoutElement)) {
                var schema = com.networknt.schema.SchemaRegistry.withDefaultDialect(com.networknt.schema.SpecificationVersion.DRAFT_2020_12)
                        .getSchema(schemas.forElement(input));
                var violations = schema.validate(rawValue).stream().limit(3).map(com.networknt.schema.Error::getMessage).toList();
                if (!violations.isEmpty()) throw new ValueValidationFailure("INVALID_LITERAL_SCHEMA",
                        "Der Literal-Wert entspricht nicht dem Eingabewertschema.", expectedTypes,
                        jsonType(rawValue), violations);
            }
            if (input instanceof de.aivot.prosuna.backend.elements.models.elements.form.input.UiDefinitionInputElement) {
                try {
                    mapper.treeToValue(rawValue, BaseElement.class);
                } catch (IllegalArgumentException | tools.jackson.core.JacksonException exception) {
                    throw failure("INVALID_EMBEDDED_FORM",
                            "Die eingebettete Formularstruktur enthält ungültige Elemente oder Kindelemente.",
                            expectedTypes, jsonType(rawValue));
                }
            }
            if (input instanceof ReplicatingContainerLayoutElement repeat) {
                var allowed = new HashMap<String, BaseInputElement<?>>();
                collectInputs(repeat, allowed);
                for (var row : rawValue) {
                    if (!row.isObject() || !row.path("values").isObject()) throw failure("INVALID_REPEAT_ROW",
                            "Listenzeilen benötigen ein values-Objekt.", expectedTypes, jsonType(row));
                    for (var entry : row.path("values").properties()) {
                        var child = allowed.get(entry.getKey());
                        if (child == null) throw failure("INVALID_REPEAT_ROW",
                                "Die Listenzeile enthält ein unbekanntes Eingabefeld.", expectedTypes, jsonType(row));
                        try {
                            validateEnvelopeDetailed(child, entry.getValue());
                        } catch (ValueValidationFailure failure) {
                            throw new ValueValidationFailure("INVALID_REPEAT_ROW",
                                    "Ein Wert in der Listenzeile ist ungültig.", expectedTypes, jsonType(row),
                                    failure.violations.isEmpty()
                                            ? List.of(entry.getKey() + ": " + failure.getMessage())
                                            : failure.violations.stream().map(item -> entry.getKey() + ": " + item).toList());
                        }
                    }
                }
            }
        }
    }

    private ObjectNode envelope(AiProcessConfigurationChange change) {
        var result = mapper.createObjectNode().put("type", change.mode().name());
        var property = switch (change.mode()) {
            case Literal -> "value";
            case Variable -> "reference";
            case NoCode -> "operand";
            case LowCode -> "code";
        };
        result.set(property, mapper.valueToTree(change.value()));
        return result;
    }

    private JsonNode currentValue(ProcessNodeEntity node, String path) {
        var raw = mapper.valueToTree(node.getConfiguration()).at(path);
        if (!raw.isObject() || !raw.path("type").isTextual()) return mapper.valueToTree(null);
        var mode = raw.path("type").asString();
        var property = switch (mode) {
            case "Literal" -> "value";
            case "Variable" -> "reference";
            case "NoCode" -> "operand";
            case "LowCode" -> "code";
            default -> null;
        };
        if (property == null) return raw;
        var result = mapper.createObjectNode().put("mode", mode);
        result.set("value", raw.has(property) ? raw.get(property) : mapper.valueToTree(null));
        return result;
    }

    private Map<String, Object> writeContract(Field field) {
        var input = field.element();
        var policy = input.getInputModePolicy();
        var modes = policy == null ? List.of(InputMode.Literal) : policy.allowedModes();
        var values = new LinkedHashMap<String, Object>();
        for (var mode : modes) {
            values.put(mode.name(), switch (mode) {
                case Literal -> "Rohwert gemäß literalValueSchema; null ist erlaubt";
                case Variable -> Map.of("source", policy.allowedVariableSources(), "path", "Pfad aus liste-knotenvariablen", "nodeDataKey", "optional");
                case NoCode -> "Operand aus hole-konfigurationshilfe";
                case LowCode -> "JavaScript-Text mit return";
            });
        }
        return Map.of("valuePath", field.valuePath(), "modeValues", values);
    }

    private JavaType inputValueType(BaseInputElement<?> input) {
        return mapper.constructType(input.getClass()).findSuperType(InputElement.class).containedType(0);
    }

    private String compactType(JavaType type) {
        if (type == null) return "any";
        var cls = type.getRawClass();
        if (cls == String.class || cls.isEnum() || java.time.temporal.TemporalAccessor.class.isAssignableFrom(cls)) return "string";
        if (cls == Boolean.class || cls == boolean.class) return "boolean";
        if (Number.class.isAssignableFrom(cls) || cls.isPrimitive() && cls != boolean.class && cls != char.class) return "number";
        if (type.isArrayType() || Collection.class.isAssignableFrom(cls)) return "array<" + compactType(type.getContentType()) + ">";
        return "object";
    }

    private List<String> schemaTypes(JsonNode schema) {
        var type = schema.path("type");
        if (type.isTextual()) return List.of(type.asString());
        if (type.isArray()) {
            var result = new ArrayList<String>();
            type.forEach(item -> result.add(item.asString()));
            return List.copyOf(result);
        }
        return List.of();
    }

    private String jsonType(JsonNode value) {
        if (value == null || value.isNull() || value.isMissingNode()) return "null";
        if (value.isObject()) return "object";
        if (value.isArray()) return "array";
        if (value.isTextual()) return "string";
        if (value.isBoolean()) return "boolean";
        if (value.isNumber()) return "number";
        return value.getNodeType().name().toLowerCase(Locale.ROOT);
    }

    private ConfigurationError error(String code, String path, @Nullable InputMode mode, String message) {
        return new ConfigurationError(code, path, mode == null ? null : mode.name(), message,
                List.of(), null, List.of());
    }

    private ValueValidationFailure failure(String code, String message, List<String> expected, String actual) {
        return new ValueValidationFailure(code, message, expected, actual, List.of());
    }

    private static final class ValueValidationFailure extends RuntimeException {
        private final String code;
        private final List<String> expectedJsonTypes;
        private final String actualJsonType;
        private final List<String> violations;

        private ValueValidationFailure(String code, String message, List<String> expectedJsonTypes,
                                       String actualJsonType, List<String> violations) {
            super(message);
            this.code = code;
            this.expectedJsonTypes = expectedJsonTypes;
            this.actualJsonType = actualJsonType;
            this.violations = violations;
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
