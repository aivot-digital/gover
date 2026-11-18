package de.aivot.prosuna.backend.process.projections;

import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;

public interface ProcessInstanceStatusCountProjection {
    ProcessInstanceStatus getStatus();

    long getTotal();
}
