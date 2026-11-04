package de.aivot.prosuna.backend.nocode.services;

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

import java.util.HashMap;

/**
 * Runs no-code expressions from the editor against test data. Nothing is saved and only operators that support dry
 * runs are executed, see {@link de.aivot.prosuna.backend.nocode.providers.NoCodeOperatorsProvider#supportsDryRun}.
 */
@Service
public class NoCodeDryRunService {
    private final NoCodeEvaluationService noCodeEvaluationService;
    private final PermissionService permissionService;

    public NoCodeDryRunService(
            @Nonnull NoCodeEvaluationService noCodeEvaluationService,
            @Nonnull PermissionService permissionService
    ) {
        this.noCodeEvaluationService = noCodeEvaluationService;
        this.permissionService = permissionService;
    }

    /**
     * The editor does not know which process an expression belongs to, so the dry run is limited to people who may
     * author expressions in at least one process. A system permission grants it as well.
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

        return noCodeEvaluationService.dryRun(
                request.expression(),
                request.elementData() == null ? new DerivedRuntimeElementData() : request.elementData(),
                processData
        );
    }
}
