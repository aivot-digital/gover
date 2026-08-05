package de.aivot.gover.backend.process.enums;

/**
 * Task status values persisted in {@code process_instance_tasks.status}.
 * Database values must not be changed or reused.
 */
public enum ProcessTaskStatus {
    Running(0),
    Paused(1),
    AwaitingPayment(2),
    Completed(3),
    Aborted(4),
    Failed(5),
    Restarted(6),
    ;

    private final short databaseValue;

    ProcessTaskStatus(int databaseValue) {
        if (databaseValue < Short.MIN_VALUE || databaseValue > Short.MAX_VALUE) {
            throw new IllegalArgumentException("Process task status database value must fit into a smallint");
        }
        this.databaseValue = (short) databaseValue;
    }

    public short getDatabaseValue() {
        return databaseValue;
    }
}
