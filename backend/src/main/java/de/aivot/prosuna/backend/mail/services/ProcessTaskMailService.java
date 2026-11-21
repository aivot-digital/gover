package de.aivot.prosuna.backend.mail.services;

import de.aivot.prosuna.backend.department.services.DepartmentService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.mail.enums.MailTemplate;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.services.ProcessService;
import de.aivot.prosuna.backend.theme.entities.ThemeEntity;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserDeputyService;
import de.aivot.prosuna.backend.utils.ApplicationTimeZone;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ProcessTaskMailService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ApplicationTimeZone.getZoneId());

    private static final Logger logger = LoggerFactory.getLogger(ProcessTaskMailService.class);

    private final MailService mailService;
    private final ProcessService processService;
    private final DepartmentService departmentService;
    private final UserDeputyService deputyService;

    @Autowired
    public ProcessTaskMailService(MailService mailService,
                                  ProcessService processService,
                                  DepartmentService departmentService,
                                  UserDeputyService deputyService) {
        this.mailService = mailService;
        this.processService = processService;
        this.departmentService = departmentService;
        this.deputyService = deputyService;
    }

    public void sendAssigned(@Nullable UserEntity triggeringUser,
                             @Nonnull UserEntity assignedUser,
                             @Nonnull ProcessInstanceEntity processInstance,
                             @Nonnull ProcessInstanceTaskEntity processInstanceTask,
                             @Nonnull ProcessNodeEntity currentNode,
                             @Nonnull ProcessNodeDefinition provider,
                             boolean isReassignment) throws MessagingException, IOException, ResponseException {
        ProcessEntity process = processService
                .retrieve(processInstanceTask.getProcessId())
                .orElseThrow(() -> new MessagingException("Process with id " + processInstanceTask.getProcessId() + " not found"));

        var department = departmentService
                .retrieve(process.getDepartmentId())
                .orElseThrow(() -> new MessagingException("Department with id " + process.getDepartmentId() + " not found"));

        String title = isReassignment
                ? "Ihnen wurde eine Aufgabe neu zugewiesen"
                : "Ihnen wurde eine Aufgabe zugewiesen";

        List<String> fileNumbers = processInstance
                .getAssignedFileNumbers()
                .stream()
                .filter(value -> value != null && !value.isBlank())
                .toList();

        String startedLabel = DATE_TIME_FORMATTER.format(processInstanceTask.getStarted());
        String deadlineLabel = processInstanceTask.getDeadline() != null
                ? DATE_TIME_FORMATTER.format(processInstanceTask.getDeadline())
                : null;

        var mailData = new HashMap<String, Object>();
        mailData.put("title", title);
        mailData.put("assignmentIntro", isReassignment
                ? "Ihnen wurde in Prosuna eine Aufgabe neu zugewiesen."
                : "Ihnen wurde in Prosuna eine Aufgabe zur Bearbeitung zugewiesen.");
        mailData.put("assignmentSource", triggeringUser != null ? triggeringUser.getFullName() : "System");
        mailData.put("process", process);
        mailData.put("department", department);
        mailData.put("processInstance", processInstance);
        mailData.put("processInstanceTask", processInstanceTask);
        mailData.put("assignedUser", assignedUser);
        mailData.put("triggeringUser", triggeringUser);
        mailData.put("isReassignment", isReassignment);
        mailData.put("taskName", currentNode.resolveName(provider));
        mailData.put("fileNumbersDisplay", fileNumbers.isEmpty() ? "Kein Aktenzeichen hinterlegt" : String.join(", ", fileNumbers));
        mailData.put("startedLabel", startedLabel);
        mailData.put("deadlineLabel", deadlineLabel);
        mailData.put("taskPath", "/staff/tasks/" + processInstance.getId() + "/" + processInstanceTask.getId() + "/edit");

        var theme = departmentService.getDepartmentTheme(department);
        var subjectPrefix = "[Prosuna] " + (processInstance.getCreatedForTestClaimId() != null ? "[Test] " : "");
        var deputyTitle = isReassignment ? "In Vertretung: Aufgabe neu zugewiesen" : "In Vertretung: Aufgabe zugewiesen";
        var deputyIntro = assignedUser.getFullName() + (isReassignment
                ? " wurde in Prosuna eine Aufgabe neu zugewiesen. "
                : " wurde in Prosuna eine Aufgabe zur Bearbeitung zugewiesen. ") + deputyNotice(assignedUser);
        // A failed mail to the assignee still reaches the caller, but must not keep the deputies from their copies.
        try {
            if (!isTriggeringUser(triggeringUser, assignedUser)) {
                mailService.sendMailToUser(theme, assignedUser.getId(), subjectPrefix + title, MailTemplate.ProcessTaskAssigned, mailData);
            }
        } finally {
            sendToDeputies(triggeringUser, assignedUser, theme, subjectPrefix + deputyTitle, MailTemplate.ProcessTaskAssigned,
                    mailData, Map.of("title", deputyTitle, "assignmentIntro", deputyIntro));
        }
    }

    public void sendUnassigned(@Nullable UserEntity triggeringUser,
                               @Nonnull UserEntity previouslyAssignedUser,
                               @Nonnull ProcessInstanceEntity processInstance,
                               @Nonnull ProcessInstanceTaskEntity processInstanceTask,
                               @Nonnull ProcessNodeEntity currentNode,
                               @Nonnull ProcessNodeDefinition provider) throws MessagingException, IOException, ResponseException {
        ProcessEntity process = processService
                .retrieve(processInstanceTask.getProcessId())
                .orElseThrow(() -> new MessagingException("Process with id " + processInstanceTask.getProcessId() + " not found"));

        var department = departmentService
                .retrieve(process.getDepartmentId())
                .orElseThrow(() -> new MessagingException("Department with id " + process.getDepartmentId() + " not found"));

        List<String> fileNumbers = processInstance
                .getAssignedFileNumbers()
                .stream()
                .filter(value -> value != null && !value.isBlank())
                .toList();

        String title = "Zuweisung zur Aufgabe aufgehoben";
        var mailData = new HashMap<String, Object>();
        mailData.put("title", title);
        mailData.put("intro", "Die Ihnen zugewiesene Aufgabe wurde einer anderen Person zugewiesen.");
        mailData.put("process", process);
        mailData.put("processInstanceTask", processInstanceTask);
        mailData.put("taskName", currentNode.resolveName(provider));
        mailData.put("fileNumbersDisplay", fileNumbers.isEmpty() ? null : String.join(", ", fileNumbers));
        mailData.put("changeSource", triggeringUser != null ? triggeringUser.getFullName() : "System");

        var theme = departmentService.getDepartmentTheme(department);
        var subjectPrefix = "[Prosuna] " + (processInstance.getCreatedForTestClaimId() != null ? "[Test] " : "");
        var deputyTitle = "In Vertretung: " + title;
        var deputyIntro = "Die " + previouslyAssignedUser.getFullName() + " zugewiesene Aufgabe wurde einer anderen Person zugewiesen. "
                + deputyNotice(previouslyAssignedUser);
        try {
            if (!isTriggeringUser(triggeringUser, previouslyAssignedUser)) {
                mailService.sendMailToUser(theme, previouslyAssignedUser.getId(), subjectPrefix + title, MailTemplate.ProcessTaskUnassigned, mailData);
            }
        } finally {
            sendToDeputies(triggeringUser, previouslyAssignedUser, theme, subjectPrefix + deputyTitle, MailTemplate.ProcessTaskUnassigned,
                    mailData, Map.of("title", deputyTitle, "intro", deputyIntro));
        }
    }

    @Nonnull
    static String deputyNotice(@Nonnull UserEntity representedUser) {
        return "Sie erhalten diese Benachrichtigung, weil Sie " + representedUser.getFullName() + " derzeit vertreten.";
    }

    /**
     * People are not notified about changes they made themselves. This applies to each recipient on its own: when the
     * assignee changes their own assignment, their deputies are still notified.
     */
    private static boolean isTriggeringUser(@Nullable UserEntity triggeringUser, @Nonnull UserEntity recipient) {
        return triggeringUser != null && recipient.getId().equals(triggeringUser.getId());
    }

    /**
     * Deputies take over the tasks of the person they represent and therefore also receive their task notifications,
     * worded for them. A deputy who triggered the change is not notified, like the assignee in that case. A failed
     * mail to one deputy must not keep the others from being notified.
     */
    private void sendToDeputies(@Nullable UserEntity triggeringUser,
                                @Nonnull UserEntity representedUser,
                                @Nonnull ThemeEntity theme,
                                @Nonnull String subject,
                                @Nonnull MailTemplate template,
                                @Nonnull Map<String, Object> mailData,
                                @Nonnull Map<String, Object> deputyData) {
        var deputies = deputyService.findActiveDeputies(Set.of(representedUser.getId()))
                .getOrDefault(representedUser.getId(), List.of());
        for (var deputy : deputies) {
            if (isTriggeringUser(triggeringUser, deputy)) {
                continue;
            }
            var data = new HashMap<>(mailData);
            data.putAll(deputyData);
            try {
                mailService.sendMailToUser(theme, deputy, subject, template, data);
            } catch (Exception exception) {
                logger.warn("Could not send the deputy notification {} to user {}.", template.getKey(), deputy.getId(), exception);
            }
        }
    }
}
