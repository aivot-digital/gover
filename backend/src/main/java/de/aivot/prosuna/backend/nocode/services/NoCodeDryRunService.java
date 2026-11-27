package de.aivot.prosuna.backend.nocode.services;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.nocode.dtos.NoCodeDryRunRequestDTO;
import de.aivot.prosuna.backend.nocode.dtos.NoCodeDryRunResultDTO;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.concurrent.Semaphore;

/**
 * Runs no-code expressions from the editor against test data. Nothing is saved and only operators that support dry runs are executed, see
 * {@link de.aivot.prosuna.backend.nocode.providers.NoCodeOperatorsProvider#supportsDryRun}.
 */
@Service
public class NoCodeDryRunService {
    private static final String RESULT_LIMIT_MESSAGE = "Der Testlauf überschreitet die Ergebnisgrenze von 1 MiB. Verkleinern Sie die Testdaten oder testen Sie einen Teilausdruck.";
    private final NoCodeEvaluationService noCodeEvaluationService;
    private final PermissionService permissionService;
    private final JsonMapper jsonMapper;
    // Browser cancellation does not stop a running operator. Bound admitted tests per backend instance.
    private final Semaphore concurrentTests = new Semaphore(4);

    public NoCodeDryRunService(
            @Nonnull NoCodeEvaluationService noCodeEvaluationService,
            @Nonnull PermissionService permissionService,
            @Nonnull JsonMapper jsonMapper
    ) {
        this.noCodeEvaluationService = noCodeEvaluationService;
        this.permissionService = permissionService;
        this.jsonMapper = jsonMapper;
    }

    /**
     * The editor does not know which process an expression belongs to, so the dry run is limited to people who may author expressions in at least one process. A system permission
     * grants it as well.
     */
    @Nonnull
    public NoCodeDryRunResultDTO dryRun(
            @Nullable String userId,
            @Nonnull NoCodeDryRunRequestDTO request
    ) throws ResponseException {
        if (!permissionService.hasInAnyProcessPermission(userId, ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE)) {
            throw ResponseException.forbidden(
                    "Sie benötigen die Berechtigung %s in mindestens einem Prozess, um Testläufe auszuführen.",
                    StringUtils.quote(ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE)
            );
        }

        if (!NoCodeDryRunLimits.expressionFits(request.expression())) {
            throw ResponseException.badRequest("Der Testlauf unterstützt höchstens 512 Ausdrucksknoten und 32 Verschachtelungsebenen. Testen Sie einen kleineren Teilausdruck.");
        }
        if (!concurrentTests.tryAcquire()) {
            throw new ResponseException(HttpStatus.TOO_MANY_REQUESTS, "Alle Plätze für Testläufe sind belegt. Versuchen Sie es später erneut.");
        }
        try {
            if (!NoCodeDryRunLimits.jsonFits(jsonMapper, request, NoCodeDryRunLimits.MAX_REQUEST_BYTES)) {
                throw ResponseException.badRequest("Ausdruck und Testdaten dürfen zusammen höchstens 256 KiB groß sein. Verkleinern Sie die Testdaten oder testen Sie einen Teilausdruck.");
            }
            var processData = new ProcessExecutionData();
            if (request.processData() != null) {
                processData.put(ProcessExecutionData.PROCESS_DATA_KEY, new HashMap<>(request.processData()));
            }
            if (request.instanceData() != null) {
                processData.put(ProcessExecutionData.PROCESS_METADATA_KEY, new HashMap<>(request.instanceData()));
            }
            if (request.nodeData() != null) {
                processData.put(ProcessExecutionData.NODE_RESULTS_KEY, new HashMap<>(request.nodeData()));
            }

            var remainingResultBytes = new AtomicInteger(NoCodeDryRunLimits.MAX_RESULT_BYTES);
            var result = noCodeEvaluationService.dryRun(
                    request.expression(),
                    request.elementData() == null ? new DerivedRuntimeElementData() : request.elementData(),
                    processData,
                    intermediate -> {
                        // Child values have already consumed their share. Count each value once, plus its envelope
                        // and a separator, so many individually small results cannot accumulate without a bound.
                        var ownResult = new NoCodeDryRunResultDTO(intermediate.status(), intermediate.value(), intermediate.message(), List.of());
                        int bytes = NoCodeDryRunLimits.jsonSizeWithinLimit(jsonMapper, ownResult, remainingResultBytes.get());
                        if (bytes < 0 || remainingResultBytes.addAndGet(-bytes - 1) < 0) {
                            return NoCodeDryRunResultDTO.skipped(RESULT_LIMIT_MESSAGE, List.of());
                        }
                        return intermediate;
                    }
            );
            return NoCodeDryRunLimits.jsonFits(jsonMapper, result, NoCodeDryRunLimits.MAX_RESULT_BYTES)
                    ? result
                    : NoCodeDryRunResultDTO.skipped(RESULT_LIMIT_MESSAGE, List.of());
        } finally {
            concurrentTests.release();
        }
    }
}
