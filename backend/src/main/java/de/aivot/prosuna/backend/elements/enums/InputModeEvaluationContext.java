package de.aivot.prosuna.backend.elements.enums;

/**
 * Controls whether dynamic authored values may be validated or executed. Literal envelopes are valid in every
 * context; the context is selected by trusted backend code and never by a client request.
 */
public enum InputModeEvaluationContext {
    LiteralOnly,
    Authoring,
    Runtime
}
