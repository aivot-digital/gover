package de.aivot.prosuna.backend.process.enums;

/** Persisted by ordinal; add new values at the end to preserve existing database values. */
public enum ProcessInstanceStatus {
    Created,
    Running,
    Paused,
    Completed,
    Aborted,
    Failed,
    InProgress,
}
