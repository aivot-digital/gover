package de.aivot.prosuna.backend.system.controllers;

import de.aivot.prosuna.backend.plugin.services.PluginUtils;
import de.aivot.prosuna.backend.plugins.form.FormPlugin;
import de.aivot.prosuna.backend.plugins.form.v1.nodes.FormTriggerNodeV1;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessNodeRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessVersionRepository;
import de.aivot.prosuna.backend.system.dtos.DashboardStatsItemDTO;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/dashboard/")
public class DashboardController {

    private final UserRepository userRepository;
    private final ProcessVersionRepository processVersionRepository;
    private final ProcessInstanceRepository processInstanceRepository;
    private final ProcessInstanceTaskRepository processInstanceTaskRepository;
    private final ProcessNodeRepository processNodeRepository;

    @Autowired
    public DashboardController(UserRepository userRepository,
                               ProcessVersionRepository processVersionRepository,
                               ProcessInstanceRepository processInstanceRepository,
                               ProcessInstanceTaskRepository processInstanceTaskRepository, ProcessNodeRepository processNodeRepository) {
        this.userRepository = userRepository;
        this.processVersionRepository = processVersionRepository;
        this.processInstanceRepository = processInstanceRepository;
        this.processInstanceTaskRepository = processInstanceTaskRepository;
        this.processNodeRepository = processNodeRepository;
    }

    @GetMapping("stats/")
    public List<DashboardStatsItemDTO> getStats() {
        return List.of(
                getProcessesStat(),
                getActiveSubmissionsStat(),
                getPublishedFormsStat(),
                getUsersStat()
        );
    }

    private DashboardStatsItemDTO getActiveSubmissionsStat() {
        var workingOnSubmissions = processInstanceRepository
                .countAllByStatusIs(ProcessInstanceStatus.Running);

        var waitingSubmissions = processInstanceTaskRepository
                .countAllByStatusIs(ProcessTaskStatus.Running);

        return new DashboardStatsItemDTO(
                "tasks",
                "Vorgänge in Bearbeitung",
                String.format("(%d warten auf Bearbeitung)", waitingSubmissions),
                workingOnSubmissions,
                "/tasks"
        );
    }

    private DashboardStatsItemDTO getPublishedFormsStat() {
        var publishedForms = processVersionRepository
                .countAllByStatusIsAndHasNode(
                        ProcessVersionStatus.Published,
                        PluginUtils.combineComponentKey(FormPlugin.PLUGIN_KEY, FormTriggerNodeV1.NODE_KEY)
                );

        return new DashboardStatsItemDTO(
                "published_forms",
                "Öffentliche Online-Formulare",
                "erlauben die digitale Antragstellung",
                publishedForms,
                "/forms?filter=published"
        );
    }

    private DashboardStatsItemDTO getUsersStat() {
        var activeUsers = userRepository
                .countAllByDeletedInIdpIsFalseAndEnabledIsTrue();

        return new DashboardStatsItemDTO(
                "total_users",
                "Registrierte Mitarbeiter:innen",
                "unterstützen mit Gover die Digitalisierung",
                activeUsers,
                "/users"
        );
    }

    private DashboardStatsItemDTO getProcessesStat() {
        var activeProcesses = processVersionRepository
                .countAllByStatusIs(ProcessVersionStatus.Published);

        return new DashboardStatsItemDTO(
                "processes",
                "Modellierte Prozesse",
                "werden von eingehenden Anträgen durchlaufen",
                activeProcesses,
                "/processes?filter=published"
        );
    }
}
