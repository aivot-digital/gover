package de.aivot.gover.backend.mail.services;

import de.aivot.gover.backend.department.services.DepartmentService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.mail.enums.MailTemplate;
import de.aivot.gover.backend.process.entities.ProcessEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.models.ProcessNodeDefinition;
import de.aivot.gover.backend.process.services.ProcessService;
import de.aivot.gover.backend.user.entities.UserEntity;
import de.aivot.gover.backend.utils.ApplicationTimeZone;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;

@Component
public class ProcessTaskMailService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ApplicationTimeZone.getZoneId());

    private final MailService mailService;
    private final ProcessService processService;
    private final DepartmentService departmentService;

    @Autowired
    public ProcessTaskMailService(MailService mailService,
                                  ProcessService processService,
                                  DepartmentService departmentService) {
        this.mailService = mailService;
        this.processService = processService;
        this.departmentService = departmentService;
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
                ? "Ihnen wurde in Gover eine Aufgabe neu zugewiesen."
                : "Ihnen wurde in Gover eine Aufgabe zur Bearbeitung zugewiesen.");
        mailData.put("assignmentSource", triggeringUser != null ? triggeringUser.getFullName() : "System");
        mailData.put("process", process);
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

        mailService.sendMailToUser(
                departmentService.getDepartmentTheme(department),
                assignedUser.getId(),
                "[Gover] " + (processInstance.getCreatedForTestClaimId() != null ? "[Test] " : "") + title,
                MailTemplate.ProcessTaskAssigned,
                mailData
        );
    }
}
