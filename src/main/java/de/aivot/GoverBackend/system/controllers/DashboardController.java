package de.aivot.GoverBackend.system.controllers;

import de.aivot.GoverBackend.enums.SubmissionStatus;
import de.aivot.GoverBackend.form.repositories.FormRepository;
import de.aivot.GoverBackend.submission.repositories.SubmissionRepository;
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

    private final SubmissionRepository submissionRepository;
    private final FormRepository formRepository;
    private final UserRepository userRepository;

    @Autowired
    public DashboardController(SubmissionRepository submissionRepository,
                               FormRepository formRepository,
                               UserRepository userRepository) {
        this.submissionRepository = submissionRepository;
        this.formRepository = formRepository;
        this.userRepository = userRepository;
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
        var workingOnSubmissions = submissionRepository
                .countAllByStatusIs(SubmissionStatus.ManualWorkingOn);

        var waitingSubmissions = submissionRepository
                .countAllByStatusIs(SubmissionStatus.OpenForManualWork);

        return new DashboardStatsItemDTO(
                "submissions",
                "Vorgänge in Bearbeitung",
                String.format("%d warten auf Bearbeitung", waitingSubmissions),
                workingOnSubmissions,
                "/submissions"
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
        return new DashboardStatsItemDTO(
                "processes",
                "Modellierte Prozesse",
                "werden von eingehenden Anträgen durchlaufen",
                999,
                "/processes"
        );
    }
}
