package de.aivot.GoverBackend.system.controllers;

import de.aivot.GoverBackend.form.repositories.FormRepository;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import de.aivot.GoverBackend.process.enums.ProcessVersionStatus;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceRepository;
import de.aivot.GoverBackend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.GoverBackend.process.repositories.ProcessVersionRepository;
import de.aivot.GoverBackend.system.dtos.DashboardStatsItemDTO;
import de.aivot.GoverBackend.user.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/dashboard/")
public class DashboardController {

    private final FormRepository formRepository;
    private final UserRepository userRepository;
    private final ProcessVersionRepository processVersionRepository;
    private final ProcessInstanceRepository processInstanceRepository;
    private final ProcessInstanceTaskRepository processInstanceTaskRepository;

    @Autowired
    public DashboardController(FormRepository formRepository,
                               UserRepository userRepository,
                               ProcessVersionRepository processVersionRepository,
                               ProcessInstanceRepository processInstanceRepository,
                               ProcessInstanceTaskRepository processInstanceTaskRepository) {
        this.formRepository = formRepository;
        this.userRepository = userRepository;
        this.processVersionRepository = processVersionRepository;
        this.processInstanceRepository = processInstanceRepository;
        this.processInstanceTaskRepository = processInstanceTaskRepository;
    }

    @GetMapping("stats/")
    public List<DashboardStatsItemDTO> getStats() {
        return List.of(
                getActiveSubmissionsStat(),
                getPublishedFormsStat(),
                getUsersStat(),
                getProcessesStat()
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
        var publishedForms = formRepository
                .countAllByPublishedVersionIsNotNull();

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
