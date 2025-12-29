package de.aivot.GoverBackend.plugins.core.v1.nodes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.MultiCheckboxInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.MultiCheckboxInputElementOption;
import de.aivot.GoverBackend.elements.models.elements.form.input.TextInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.plugin.models.PluginComponent;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.process.entities.*;
import de.aivot.GoverBackend.process.enums.ProcessHistoryEventType;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.repositories.UserRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class CheckAndUpdateActionNode implements ProcessNodeDefinition, PluginComponent {
    private static final String ACCEPT_PORT_NAME = "accept";
    private static final String REJECT_PORT_NAME = "reject";

    private static final String INSTRUCTIONS_FIELD_ID = "instructions";
    private static final String USER_OPTIONS_FIELD_ID = "userOptions";

    private static final String ACCEPT_EVENT_NAME = "accept";
    private static final String REJECT_EVENT_NAME = "reject";

    private final UserRepository userRepository;

    public CheckAndUpdateActionNode(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public @Nonnull String getKey() {
        return "check-and-update";
    }

    @Nonnull
    @Override
    public Integer getVersion() {
        return 1;
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return Core.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public ProcessNodeType getType() {
        return ProcessNodeType.Action;
    }

    @Nonnull
    @Override
    public String getName() {
        return "Datenprüfung und -korrektur";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Prüft die gegebenen Daten und ermöglicht gegebenenfalls eine Korrektur durch ausgewählte Mitarbeiter:innen.";
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull UserEntity user,
                                                      @Nonnull ProcessEntity processDefinition,
                                                      @Nonnull ProcessVersionEntity processDefinitionVersion,
                                                      @Nullable ProcessNodeEntity thisNode) {
        var layout = new ConfigLayoutElement();
        layout.setId(getKey() + "-config");

        var instructionsField = new TextInputElement();
        instructionsField.setId(INSTRUCTIONS_FIELD_ID);
        instructionsField.setLabel("Anweisungen für die Mitarbeiter:innen");
        instructionsField.setHint("Geben Sie die Anweisungen ein, die den ausgewählten Mitarbeiter:innen angezeigt werden sollen, wenn sie die Daten überprüfen und ggf. korrigieren.");
        instructionsField.setRequired(true);
        instructionsField.setIsMultiline(true);
        layout.addChild(instructionsField);

        var options = userRepository
                .findAll()
                .stream()
                .map(u -> new MultiCheckboxInputElementOption()
                        .setLabel(u.getFullName() + " (" + u.getEmail() + ")")
                        .setValue(u.getId()))
                .toList();

        var method = new MultiCheckboxInputElement();
        method.setId(USER_OPTIONS_FIELD_ID);
        method.setLabel("Mögliche, zuweisbare Mitarbeiter:innen");
        method.setHint("Wählen Sie alle Mitarbeiter:innen aus, die für die Aufgabe in Frage kommen.");
        method.setRequired(true);
        method.setOptions(options);
        layout.addChild(method);

        return layout;
    }

    @Nonnull
    @Override
    public GroupLayoutElement getStaffTaskView(@Nullable UserEntity user,
                                               @Nonnull ProcessNodeEntity thisNode,
                                               @Nonnull ProcessInstanceEntity processInstance,
                                               @Nonnull ProcessInstanceTaskEntity thisTask) throws ResponseException {
        var configuration = thisNode.getConfiguration();

        var instructionsContent = configuration
                .get(INSTRUCTIONS_FIELD_ID)
                .getOptionalValue()
                .orElse("Bitte überprüfen und korrigieren Sie die gegebenen Daten.")
                .toString();

        var layout = new GroupLayoutElement();
        layout.setId(getKey() + "-task-view");

        var instructions = new RichTextContentElement();
        instructions.setId("instructions");
        instructions.setContent(instructionsContent);
        layout.addChild(instructions);

        String json;
        try {
            json = ObjectMapperFactory
                    .getInstance()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(thisTask.getProcessData());
        } catch (JsonProcessingException e) {
            throw ResponseException.internalServerError(e);
        }

        var content = new RichTextContentElement();
        content.setId("content");
        content.setContent(json);
        layout.addChild(content);

        return layout;
    }

    @Nonnull
    @Override
    public List<TaskViewEvent> getStaffTaskViewEvents(@Nullable UserEntity user,
                                                      @Nonnull ProcessNodeEntity thisNode,
                                                      @Nonnull ProcessInstanceEntity processInstance,
                                                      @Nonnull ProcessInstanceTaskEntity thisTask) throws ResponseException {
        return List.of(
                new TaskViewEvent(
                        "Abgeschlossen",
                        ACCEPT_EVENT_NAME
                ),
                new TaskViewEvent(
                        "Abgelehnt",
                        REJECT_EVENT_NAME
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        ACCEPT_PORT_NAME,
                        "Akzeptiert",
                        "Die gegebenen Daten wurden akzeptiert und der Prozess wird fortgesetzt."
                ),
                new ProcessNodePort(
                        REJECT_PORT_NAME,
                        "Abgelehnt",
                        "Die gegebenen Daten wurden abgelehnt und der Prozess wird fortgesetzt."
                )
        );
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessInstanceEntity processInstance,
                                           @Nonnull ProcessNodeEntity thisNode,
                                           @Nonnull Map<String, Object> workingData) throws Exception {
        var configuration = thisNode.getConfiguration();

        var userOptions = configuration
                .get(USER_OPTIONS_FIELD_ID)
                .getOptionalValue()
                .orElse(null);

        if (userOptions instanceof List<?> userIds && !userIds.isEmpty()) {
            var randomUserId = (String) userIds.get((int) (Math.random() * userIds.size()));

            var user = userRepository
                    .findById(randomUserId)
                    .orElse(null);

            if (user == null) {
                return new ProcessNodeExecutionResultError()
                        .setMessage("Der ausgewählte Benutzer mit der ID " + randomUserId + " existiert nicht.");
            }

            return new ProcessNodeExecutionResultTaskAssigned()
                    .setAssignedUserId(randomUserId)
                    .addEvent(ProcessNodeExecutionEvent.of(
                            ProcessHistoryEventType.Assign,
                            "Aufgabe zur Datenprüfung und -korrektur wurde zugewiesen.",
                            "Die Aufgabe wurde der Mitarbeiter:in " + user.getFullName() + " (" + user.getEmail() + ") zugewiesen.",
                            Map.of()
                    ))
                    .setTaskStatusOverride("Zur Prüfung und Korrektur zugewiesen");
        } else {
            return new ProcessNodeExecutionResultError()
                    .setMessage("Keine gültigen Benutzeroptionen konfiguriert.");
        }
    }

    @Override
    public Optional<ProcessNodeExecutionResult> updateStaff(@Nullable UserEntity user,
                                                            @Nonnull ProcessInstanceEntity processInstance,
                                                            @Nonnull ProcessNodeEntity thisNode,
                                                            @Nonnull Map<String, Object> workingData,
                                                            @Nonnull Map<String, Object> updateData,
                                                            @Nonnull String event) throws Exception {
        if (user == null) {
            return Optional.of(ProcessNodeExecutionResultError.of("Dieser Vorgang erfordert einen angemeldeten Benutzer."));
        }

        var res = new ProcessNodeExecutionResultTaskCompleted();

        if (event.equals(ACCEPT_EVENT_NAME)) {
            res.setViaPort(ACCEPT_PORT_NAME);
        } else {
            res.setViaPort(REJECT_PORT_NAME);
        }

        res.setNodeData(Map.of(
                "action", event
        ));

        res.addEvent(ProcessNodeExecutionEvent.of(
                ProcessHistoryEventType.Complete,
                "Datenprüfung und -korrektur abgeschlossen.",
                "Die Mitarbeiter:in " + user.getFullName() + " hat die Datenprüfung und -korrektur abgeschlossen.",
                Map.of(
                        "action", event
                )
        ));

        res.setClearTaskStatusOverride(true);

        return Optional.of(res);
    }
}
