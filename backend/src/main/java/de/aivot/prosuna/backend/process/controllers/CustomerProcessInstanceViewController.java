package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.asset.services.AssetService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.openApi.OpenApiConstants;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntityId;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.filters.ProcessInstanceTaskFilter;
import de.aivot.prosuna.backend.process.services.ProcessInstanceService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceTaskService;
import de.aivot.prosuna.backend.process.services.ProcessService;
import de.aivot.prosuna.backend.process.services.ProcessVersionService;
import de.aivot.prosuna.backend.theme.dtos.ResolvedThemeDTO;
import de.aivot.prosuna.backend.theme.services.ThemeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/processes/{procAccess}/")
@Tag(
        name = OpenApiConstants.Tags.ProcessesDefinitionsName,
        description = "Operations for managing process instance tasks."
)
public class CustomerProcessInstanceViewController {
    private final ProcessInstanceService processInstanceService;
    private final ProcessInstanceTaskService processInstanceTaskService;
    private final ProcessVersionService processVersionService;
    private final ProcessService processService;
    private final ThemeService themeService;
    private final AssetService assetService;
    private final ProsunaConfig prosunaConfig;

    public CustomerProcessInstanceViewController(ProcessInstanceService processInstanceService,
                                                 ProcessInstanceTaskService processInstanceTaskService,
                                                 ProcessVersionService processVersionService,
                                                 ProcessService processService,
                                                 ThemeService themeService,
                                                 AssetService assetService,
                                                 ProsunaConfig prosunaConfig) {
        this.processInstanceService = processInstanceService;
        this.processInstanceTaskService = processInstanceTaskService;
        this.processVersionService = processVersionService;
        this.processService = processService;
        this.themeService = themeService;
        this.assetService = assetService;
        this.prosunaConfig = prosunaConfig;
    }

    @GetMapping("")
    @Operation(
            summary = "Retrieve Process Instance Task View Layout",
            description = "Retrieves the view layout for a specific task within a process instance. " +
                    "The layout defines how the task is presented to the user, including form fields and structure."
    )
    public ProcessInstanceStatusResponse retrieve(
            @Nonnull @PathVariable String procAccess
    ) throws ResponseException {
        var instance = processInstanceService
                .retrieveByAccessKey(procAccess)
                .orElseThrow(ResponseException::notFound);

        var processVersion = processVersionService
                .retrieve(ProcessVersionEntityId.of(instance.getProcessId(), instance.getInitialProcessVersion()))
                .orElseThrow(ResponseException::notFound);
        var process = processService
                .retrieve(instance.getProcessId())
                .orElseThrow(ResponseException::notFound);
        var resolvedTheme = themeService.resolveProcessTheme(processVersion, process.getDepartmentId());

        var taskFilter = new ProcessInstanceTaskFilter()
                .setProcessInstanceId(instance.getId());

        var taskPagination = Pageable
                .unpaged(Sort.by(Sort.Direction.ASC, "started"));

        var tasks = processInstanceTaskService
                .list(taskPagination, taskFilter)
                .map(ProcessInstanceTaskStatusResponse::of)
                .toList();

        return new ProcessInstanceStatusResponse(
                processVersion.getPublicTitle(),
                instance.getStatus(),
                instance.getStatusOverride(),
                tasks,
                processVersion.getAccessibilityDepartmentId(),
                processVersion.getPrivacyDepartmentId(),
                processVersion.getImprintDepartmentId(),
                processVersion.getLegalSupportDepartmentId(),
                processVersion.getTechnicalSupportDepartmentId(),
                ResolvedThemeDTO.fromResolvedTheme(resolvedTheme, assetService, prosunaConfig)
        );
    }

    public record ProcessInstanceStatusResponse(
            @Nonnull
            String title,
            @Nonnull
            ProcessInstanceStatus status,
            @Nullable
            String statusOverride,
            @Nullable
            List<ProcessInstanceTaskStatusResponse> tasks,
            @Nullable
            Integer accessibilityDepartmentId,
            @Nullable
            Integer privacyDepartmentId,
            @Nullable
            Integer imprintDepartmentId,
            @Nullable
            Integer legalSupportDepartmentId,
            @Nullable
            Integer technicalSupportDepartmentId,
            @Nonnull
            ResolvedThemeDTO theme
    ) {
    }

    public record  ProcessInstanceTaskStatusResponse(
            @Nonnull
            String accessKey,
            @Nonnull
            ProcessTaskStatus status,
            @Nullable
            String statusOverride
    ) {
        public static ProcessInstanceTaskStatusResponse of(ProcessInstanceTaskEntity task) {
            return new ProcessInstanceTaskStatusResponse(
                    task.getAccessKey(),
                    task.getStatus(),
                    task.getStatusOverride()
            );
        }
    }
}
