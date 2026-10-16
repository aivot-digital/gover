package de.aivot.prosuna.backend.process.controllers;

import de.aivot.prosuna.backend.asset.services.AssetService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConstants;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntityId;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.filters.ProcessInstanceTaskFilter;
import de.aivot.prosuna.backend.process.services.ProcessInstanceService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceTaskService;
import de.aivot.prosuna.backend.process.services.ProcessService;
import de.aivot.prosuna.backend.process.services.ProcessSubmissionBindingService;
import de.aivot.prosuna.backend.process.services.ProcessVersionService;
import de.aivot.prosuna.backend.process.utils.ProcessSubmissionCookieUtils;
import de.aivot.prosuna.backend.theme.dtos.ResolvedThemeDTO;
import de.aivot.prosuna.backend.theme.services.ThemeService;
import de.aivot.prosuna.backend.utils.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
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
    private final ProcessSubmissionBindingService processSubmissionBindingService;

    public CustomerProcessInstanceViewController(ProcessInstanceService processInstanceService,
                                                 ProcessInstanceTaskService processInstanceTaskService,
                                                 ProcessVersionService processVersionService,
                                                 ProcessService processService,
                                                 ThemeService themeService,
                                                 AssetService assetService,
                                                 ProcessSubmissionBindingService processSubmissionBindingService) {
        this.processInstanceService = processInstanceService;
        this.processInstanceTaskService = processInstanceTaskService;
        this.processVersionService = processVersionService;
        this.processService = processService;
        this.themeService = themeService;
        this.assetService = assetService;
        this.processSubmissionBindingService = processSubmissionBindingService;
    }

    @GetMapping("")
    @Operation(
            summary = "Retrieve Process Instance Status",
            description = "Retrieves the public status of a process instance. The task list only contains the task " +
                    "whose access key is passed as the task parameter and, for the browser that submitted the " +
                    "instance, its first task. The tasks of other participants are never listed."
    )
    public ProcessInstanceStatusResponse retrieve(
            @Nonnull @PathVariable String procAccess,
            @Nullable @RequestParam(value = "task", required = false) String taskAccessKey,
            @Nullable @CookieValue(value = ProcessSubmissionCookieUtils.PROCESS_SUBMISSION_COOKIE_NAME, required = false) String submissionSecret
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

        var tasks = listVisibleTasks(instance, taskAccessKey, submissionSecret)
                .stream()
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
                ResolvedThemeDTO.fromResolvedTheme(resolvedTheme, assetService)
        );
    }

    /**
     * Every customer who participates in an instance receives its access key in their task link. The access key alone
     * must therefore not reveal the tasks of other participants, whose task access keys grant access to their data. A
     * caller only sees the task whose access key they already hold, and the browser that submitted the instance also
     * sees its first task, so the confirmation page can offer the submitted application and the payment.
     */
    @Nonnull
    private List<ProcessInstanceTaskEntity> listVisibleTasks(@Nonnull ProcessInstanceEntity instance,
                                                             @Nullable String taskAccessKey,
                                                             @Nullable String submissionSecret) throws ResponseException {
        var visibleTasks = new ArrayList<ProcessInstanceTaskEntity>();

        if (processSubmissionBindingService.isBound(submissionSecret, instance.getId())) {
            var firstTaskPage = PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "started"));
            var firstTaskFilter = new ProcessInstanceTaskFilter()
                    .setProcessInstanceId(instance.getId());
            processInstanceTaskService
                    .list(firstTaskPage, firstTaskFilter)
                    .stream()
                    .findFirst()
                    .ifPresent(visibleTasks::add);
        }

        if (StringUtils.isNotNullOrEmpty(taskAccessKey)) {
            processInstanceTaskService
                    .retrieveByProcessInstanceIdAndAccessKey(instance.getId(), taskAccessKey)
                    .filter(task -> visibleTasks.stream().noneMatch(visibleTask -> visibleTask.getId().equals(task.getId())))
                    .ifPresent(visibleTasks::add);
        }

        return visibleTasks;
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
