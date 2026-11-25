package de.aivot.prosuna.backend.nocode.enums;

/**
 * Outcome of a single operand in a dry run of a no-code expression.
 */
public enum NoCodeDryRunStatus {
    /** The operand was evaluated and its value is part of the result. */
    Evaluated,
    /** The operator failed for the given values. */
    Failed,
    /** The operand was not evaluated, because its operator does not run in dry runs or a parameter has no value. */
    Skipped,
}
