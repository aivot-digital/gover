package de.aivot.prosuna.backend.process.projections;

public interface ProcessTaskStatusCountProjection {
    /** The database value of the status, because this comes from a native query. */
    short getStatus();

    long getTotal();
}
