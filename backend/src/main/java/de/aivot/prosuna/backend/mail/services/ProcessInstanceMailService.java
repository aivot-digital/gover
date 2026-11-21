package de.aivot.prosuna.backend.mail.services;

import de.aivot.prosuna.backend.department.services.DepartmentService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.mail.enums.MailTemplate;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.services.ProcessService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserDeputyService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@Component
public class ProcessInstanceMailService {
    private static final Logger logger = LoggerFactory.getLogger(ProcessInstanceMailService.class);

    private final MailService mailService;
    private final ProcessService processService;
    private final DepartmentService departmentService;
    private final UserDeputyService deputyService;

    @Autowired
    public ProcessInstanceMailService(MailService mailService,
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
                             boolean isReassignment) throws MessagingException, IOException, ResponseException {
        String title = isReassignment
                ? "Vorgang neu zugewiesen"
                : "Vorgang zugewiesen";
        String intro = isReassignment
                ? "Ihnen wurde in Prosuna ein Vorgang neu zugewiesen."
                : "Ihnen wurde in Prosuna ein Vorgang zugewiesen.";

        var deputyIntro = assignedUser.getFullName() + (isReassignment
                ? " wurde in Prosuna ein Vorgang neu zugewiesen. "
                : " wurde in Prosuna ein Vorgang zugewiesen. ") + ProcessTaskMailService.deputyNotice(assignedUser);
        // A failed mail to the assignee still reaches the caller, but must not keep the deputies from their copies.
        try {
            sendNotification(triggeringUser, assignedUser, processInstance,
                    MailTemplate.ProcessInstanceAssigned, title, intro, true);
        } finally {
            sendToDeputies(triggeringUser, assignedUser, processInstance,
                    MailTemplate.ProcessInstanceAssigned, "In Vertretung: " + title, deputyIntro, true);
        }
    }

    public void sendUnassigned(@Nullable UserEntity triggeringUser,
                               @Nonnull UserEntity previouslyAssignedUser,
                               @Nonnull ProcessInstanceEntity processInstance,
                               boolean isReassignment) throws MessagingException, IOException, ResponseException {
        String intro = isReassignment
                ? "Der Ihnen zugewiesene Vorgang wurde einer anderen Person zugewiesen."
                : "Die Zuweisung dieses Vorgangs an Sie wurde aufgehoben.";

        var name = previouslyAssignedUser.getFullName();
        var deputyIntro = (isReassignment
                ? "Der " + name + " zugewiesene Vorgang wurde einer anderen Person zugewiesen. "
                : "Die Zuweisung dieses Vorgangs an " + name + " wurde aufgehoben. ")
                + ProcessTaskMailService.deputyNotice(previouslyAssignedUser);
        try {
            sendNotification(triggeringUser, previouslyAssignedUser, processInstance,
                    MailTemplate.ProcessInstanceUnassigned, "Zuweisung zum Vorgang aufgehoben", intro, false);
        } finally {
            sendToDeputies(triggeringUser, previouslyAssignedUser, processInstance,
                    MailTemplate.ProcessInstanceUnassigned, "In Vertretung: Zuweisung zum Vorgang aufgehoben", deputyIntro, false);
        }
    }

    /**
     * Deputies also receive the case notifications of the person they represent, worded for them. A deputy who
     * triggered the change is not notified, and a failed mail to one deputy must not keep the others from it.
     */
    private void sendToDeputies(@Nullable UserEntity triggeringUser,
                                @Nonnull UserEntity representedUser,
                                @Nonnull ProcessInstanceEntity processInstance,
                                @Nonnull MailTemplate template,
                                @Nonnull String title,
                                @Nonnull String intro,
                                boolean includeInstanceLink) {
        var deputies = deputyService.findActiveDeputies(Set.of(representedUser.getId()))
                .getOrDefault(representedUser.getId(), List.of());
        for (var deputy : deputies) {
            if (triggeringUser != null && deputy.getId().equals(triggeringUser.getId())) {
                continue;
            }
            try {
                sendNotification(triggeringUser, deputy, processInstance, template, title, intro, includeInstanceLink);
            } catch (Exception exception) {
                logger.warn("Could not send the deputy notification {} to user {}.", template.getKey(), deputy.getId(), exception);
            }
        }
    }

    private void sendNotification(@Nullable UserEntity triggeringUser,
                                  @Nonnull UserEntity recipient,
                                  @Nonnull ProcessInstanceEntity processInstance,
                                  @Nonnull MailTemplate template,
                                  @Nonnull String title,
                                  @Nonnull String intro,
                                  boolean includeInstanceLink) throws MessagingException, IOException, ResponseException {
        ProcessEntity process = processService
                .retrieve(processInstance.getProcessId())
                .orElseThrow(() -> new MessagingException("Process with id " + processInstance.getProcessId() + " not found"));

        var department = departmentService
                .retrieve(process.getDepartmentId())
                .orElseThrow(() -> new MessagingException("Department with id " + process.getDepartmentId() + " not found"));

        List<String> fileNumbers = processInstance
                .getAssignedFileNumbers()
                .stream()
                .filter(value -> value != null && !value.isBlank())
                .toList();

        var mailData = new HashMap<String, Object>();
        mailData.put("title", title);
        mailData.put("intro", intro);
        mailData.put("changeSource", triggeringUser != null ? triggeringUser.getFullName() : "System");
        mailData.put("process", process);
        mailData.put("processInstance", processInstance);
        mailData.put("fileNumbersDisplay", fileNumbers.isEmpty() ? null : String.join(", ", fileNumbers));
        if (includeInstanceLink) {
            mailData.put("instancePath", "/staff/processes/" + process.getId()
                    + "/versions/" + processInstance.getInitialProcessVersion()
                    + "?instanceId=" + processInstance.getId());
        }

        mailService.sendMailToUser(
                departmentService.getDepartmentTheme(department),
                recipient.getId(),
                "[Prosuna] " + (processInstance.getCreatedForTestClaimId() != null ? "[Test] " : "") + title,
                template,
                mailData
        );
    }
}
