package de.aivot.prosuna.backend.process.enums;

/**
 * Kinds of pending process work persisted in {@code process_work_requests.kind}.
 * Existing database values must not be changed or reused; new kinds need a new value.
 */
public enum ProcessWorkRequestKind {
    /** Creates the first task of a started instance. */
    Start(0),
    /** Creates the successor of a completed or expired task. */
    Transition(1),
    /** Runs a failed task again. */
    Restart(2),
    /** Resumes a waiting task, e.g. after a payment status change. */
    Resume(3),
    ;

    private final short databaseValue;

    ProcessWorkRequestKind(int databaseValue) {
        this.databaseValue = (short) databaseValue;
    }

    public short getDatabaseValue() {
        return databaseValue;
    }
}
