package de.aivot.prosuna.backend.elements.services;

import de.aivot.prosuna.backend.elements.enums.InputVariableSource;
import de.aivot.prosuna.backend.elements.models.input.InputVariableReference;
import de.aivot.prosuna.backend.process.models.ProcessDataValueUtils;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Resolves structured variable references while preserving the distinction between a missing path and an explicit null. */
@Service
public class InputVariableResolver {
    public record Resolution(boolean found, @Nullable Object value) {
    }

    public void validate(@Nonnull InputVariableReference reference) {
        if (reference.path().isBlank()) {
            throw new IllegalArgumentException("Der Variablenpfad darf nicht leer sein.");
        }
        ProcessDataValueUtils.validateDestinationKey(reference.path(), false);
        if (ProcessDataValueUtils.hasWildcardSegment(reference.path())) {
            throw new IllegalArgumentException("Variablenreferenzen dürfen keine Platzhaltersegmente enthalten.");
        }

        var requiresNodeDataKey = reference.source() == InputVariableSource.ElementData ||
                reference.source() == InputVariableSource.ElementMetadata;
        if (requiresNodeDataKey && (reference.nodeDataKey() == null || reference.nodeDataKey().isBlank())) {
            throw new IllegalArgumentException("Für diese Variablenquelle muss ein Element-Datenschlüssel angegeben werden.");
        }
        if (!requiresNodeDataKey && reference.nodeDataKey() != null && !reference.nodeDataKey().isBlank()) {
            throw new IllegalArgumentException("Für diese Variablenquelle ist kein Element-Datenschlüssel zulässig.");
        }
    }

    @Nonnull
    public Resolution resolve(@Nonnull InputVariableReference reference,
                              @Nonnull ProcessExecutionData processExecutionData) {
        validate(reference);

        Object root = switch (reference.source()) {
            case ProcessData -> processExecutionData.get(ProcessExecutionData.PROCESS_DATA_KEY);
            case ProtectedProcessData -> processExecutionData.get(ProcessExecutionData.PROCESS_METADATA_KEY);
            case ElementData -> resolveMapValue(
                    processExecutionData.get(ProcessExecutionData.NODE_RESULTS_KEY),
                    reference.nodeDataKey()
            );
            case ElementMetadata -> {
                var taskMetadata = resolveMapValue(
                        processExecutionData.get(ProcessExecutionData.PROCESS_METADATA_KEY),
                        "taskMetadata"
                );
                yield resolveMapValue(taskMetadata, reference.nodeDataKey());
            }
        };

        if (root == MissingValue.INSTANCE) {
            return new Resolution(false, null);
        }

        return resolvePath(root, Arrays.asList(reference.path().split("\\.")));
    }

    @Nonnull
    private Resolution resolvePath(@Nullable Object current, @Nonnull List<String> segments) {
        Object value = current;
        for (var segment : segments) {
            if (value instanceof Map<?, ?> map) {
                if (!map.containsKey(segment)) {
                    return new Resolution(false, null);
                }
                value = map.get(segment);
                continue;
            }

            if (value instanceof List<?> list) {
                int index;
                try {
                    index = Integer.parseInt(segment);
                } catch (NumberFormatException exception) {
                    return new Resolution(false, null);
                }
                if (index < 0 || index >= list.size()) {
                    return new Resolution(false, null);
                }
                value = list.get(index);
                continue;
            }

            return new Resolution(false, null);
        }
        return new Resolution(true, value);
    }

    @Nullable
    private Object resolveMapValue(@Nullable Object value, @Nullable String key) {
        if (!(value instanceof Map<?, ?> map) || key == null || !map.containsKey(key)) {
            return MissingValue.INSTANCE;
        }
        return map.get(key);
    }

    private enum MissingValue {
        INSTANCE
    }
}
