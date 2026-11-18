package de.aivot.prosuna.backend.process.models;

/**
 * Describes a value produced by a process node.
 *
 * @param key            The key under which the value is stored in the node data.
 * @param label          The user-facing label of the output.
 * @param description    The user-facing description of the output.
 * @param typeDefinition A standalone TypeScript type expression describing the output value.
 * @param markdown       Whether the value is Markdown, so the variable picker inserts it as plain text where needed.
 */
public record ProcessNodeOutput(
        String key,
        String label,
        String description,
        String typeDefinition,
        boolean markdown
) {
    public ProcessNodeOutput {
        if (typeDefinition == null || typeDefinition.isBlank()) {
            throw new IllegalArgumentException("The TypeScript type definition must not be null or blank.");
        }
    }

    public ProcessNodeOutput(String key, String label, String description, String typeDefinition) {
        this(key, label, description, typeDefinition, false);
    }
}
