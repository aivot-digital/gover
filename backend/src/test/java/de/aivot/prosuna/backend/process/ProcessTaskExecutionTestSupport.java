package de.aivot.prosuna.backend.process;

import de.aivot.prosuna.backend.process.services.ProcessTaskExecutionService;

import static org.mockito.Mockito.mock;

/** Isolates existing provider/controller tests; transaction and deadline rules have dedicated tests. */
public final class ProcessTaskExecutionTestSupport {
    private ProcessTaskExecutionTestSupport() {}

    public static ProcessTaskExecutionService passthrough() {
        return mock(ProcessTaskExecutionService.class, invocation -> {
            var arguments = invocation.getArguments();
            for (var argument : arguments) {
                if (argument instanceof ProcessTaskExecutionService.Authorization authorization) authorization.run();
            }
            for (var argument : arguments) {
                if (argument instanceof ProcessTaskExecutionService.Operation<?> operation) return operation.run();
            }
            return null;
        });
    }
}
