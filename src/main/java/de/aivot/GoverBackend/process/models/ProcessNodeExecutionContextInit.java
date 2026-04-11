package de.aivot.GoverBackend.process.models;

import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.elements.models.EffectiveElementValues;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessTestClaimEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

public class ProcessNodeExecutionContextInit extends ProcessNodeExecutionContextBase {
    /**
     * The process data available during the execution of the node.
     */
    @Nonnull
    private final ProcessExecutionData processData;

    /**
     * The configuration data available during the execution of the node.
     */
    private final DerivedRuntimeElementData configuration;

    public ProcessNodeExecutionContextInit(@Nonnull ProcessNodeExecutionLogger logger,
                                           @Nonnull ProcessNodeEntity thisNode,
                                           @Nonnull ProcessInstanceEntity thisProcessInstance,
                                           @Nonnull ProcessInstanceTaskEntity thisTask,
                                           @Nullable ProcessTestClaimEntity testClaim,
                                           @Nonnull ProcessExecutionData processData,
                                           @Nonnull DerivedRuntimeElementData configuration) {
        super(logger, thisNode, thisProcessInstance, thisTask, testClaim);
        this.processData = processData;
        this.configuration = configuration;
    }

    @Nonnull
    public ProcessExecutionData getProcessData() {
        return processData;
    }

    public DerivedRuntimeElementData getConfiguration() {
        return configuration;
    }
}
