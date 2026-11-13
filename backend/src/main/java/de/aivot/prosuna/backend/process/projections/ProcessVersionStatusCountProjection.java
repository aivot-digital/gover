package de.aivot.prosuna.backend.process.projections;

import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;

public interface ProcessVersionStatusCountProjection {
    ProcessVersionStatus getStatus();

    long getTotal();
}
