package de.aivot.prosuna.backend.nocode.services;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayDeque;
import java.util.Set;

/**
 * Cleans a serialized copy: definitions and drafts held by persistence or execution must never be mutated.
 */
@Service
public class NoCodeEditorMetadataCleaner {
    private static final Set<String> OPERAND_TYPES = Set.of(
            "NoCodeExpression", "NoCodeReference", "NoCodeStaticValue", "NoCodeProcessDataReference",
            "NoCodeInstanceDataReference", "NoCodeNodeDataReference"
    );
    private final JsonMapper mapper;

    public NoCodeEditorMetadataCleaner(@Nonnull JsonMapper mapper) {
        this.mapper = mapper;
    }

    @Nonnull
    public JsonNode clean(@Nullable Object value) {
        JsonNode result = mapper.valueToTree(value);
        var pending = new ArrayDeque<JsonNode>();
        pending.push(result);
        while (!pending.isEmpty()) {
            var node = pending.pop();
            if (node instanceof ObjectNode object && OPERAND_TYPES.contains(object.path("type").asString(""))) {
                object.remove("editorMetadata");
            }
            if (node.isObject() || node.isArray()) node.forEach(pending::push);
        }
        return result;
    }

    @Nonnull
    public AuthoredElementValues cleanConfiguration(@Nonnull AuthoredElementValues configuration) {
        return mapper.treeToValue(clean(configuration), AuthoredElementValues.class);
    }
}
