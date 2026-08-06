package de.aivot.gover.backend.process.enums;

public enum ProcessTaskStatus {
    Running,
    Paused,
    AwaitingPayment,
    AwaitingCitizen,
    Completed,
    Aborted,
    Failed,
    Restarted,
}
