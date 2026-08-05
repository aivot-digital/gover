package de.aivot.gover.backend.process.enums;

/**
 * Task status values persisted in {@code process_instance_tasks.status}.
 * They preserve the ordinal values used before explicit conversion was introduced, so declaration and database order
 * intentionally differ.
 * Existing database values must not be changed or reused; new statuses need a new value.
 */
public enum ProcessTaskStatus {
    Running(0),
    Paused(1),
    AwaitingPayment(6),
    Completed(2),
    Aborted(3),
    Failed(4),
    Restarted(5),
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
