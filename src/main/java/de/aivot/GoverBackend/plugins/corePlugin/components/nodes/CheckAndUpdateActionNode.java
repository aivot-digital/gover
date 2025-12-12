package de.aivot.GoverBackend.plugins.corePlugin.components.nodes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.models.elements.form.content.RichText;
import de.aivot.GoverBackend.elements.models.elements.form.input.MultiCheckboxField;
import de.aivot.GoverBackend.elements.models.elements.form.input.MultiCheckboxFieldOption;
import de.aivot.GoverBackend.elements.models.elements.form.input.TextField;
import de.aivot.GoverBackend.elements.models.elements.form.layout.GroupLayout;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.plugin.models.PluginComponent;
import de.aivot.GoverBackend.plugins.corePlugin.Core;
import de.aivot.GoverBackend.process.entities.*;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.repositories.UserRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class CheckAndUpdateActionNode implements ProcessNodeProvider, PluginComponent {
    private static final String ACCEPT_PORT_NAME = "accept";
    private static final String REJECT_PORT_NAME = "reject";

    private static final String INSTRUCTIONS_FIELD_ID = "instructions";
    private static final String USER_OPTIONS_FIELD_ID = "userOptions";

    private static final String ACCEPT_EVENT_NAME = "accept";
    ;
    private static final String REJECT_EVENT_NAME = "reject";

    private final UserRepository userRepository;

    public CheckAndUpdateActionNode(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Nonnull
    @Override
    public String getKey() {
        return "check-and-update";
    }

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
    public GroupLayout getConfigurationLayout(@Nonnull UserEntity user,
                                              @Nonnull ProcessDefinitionEntity processDefinition,
                                              @Nonnull ProcessDefinitionVersionEntity processDefinitionVersion,
                                              @Nullable ProcessDefinitionNodeEntity thisNode) {
        var layout = new GroupLayout();
        layout.setId(getKey() + "-config");

        var instructionsField = new TextField();
        instructionsField.setId(INSTRUCTIONS_FIELD_ID);
        instructionsField.setLabel("Anweisungen für die Mitarbeiter:innen");
        instructionsField.setHint("Geben Sie die Anweisungen ein, die den ausgewählten Mitarbeiter:innen angezeigt werden sollen, wenn sie die Daten überprüfen und ggf. korrigieren.");
        instructionsField.setRequired(true);
        instructionsField.setIsMultiline(true);
        layout.addChild(instructionsField);

        var options = userRepository
                .findAll()
                .stream()
                .map(u -> new MultiCheckboxFieldOption()
                        .setLabel(u.getFullName() + " (" + u.getEmail() + ")")
                        .setValue(u.getId()))
                .toList();

        var method = new MultiCheckboxField();
        method.setId(USER_OPTIONS_FIELD_ID);
        method.setLabel("Mögliche, zuweisbare Mitarbeiter:innen");
        method.setHint("Wählen Sie alle Mitarbeiter:innen aus, die für die Aufgabe in Frage kommen.");
        method.setRequired(true);
        method.setOptions(options);
        layout.addChild(method);

        return layout;
    }

    @Override
    public boolean canGetTaskViewLayout(@Nullable UserEntity user,
                                        @Nonnull ProcessDefinitionNodeEntity thisNode,
                                        @Nonnull ProcessInstanceEntity processInstance,
                                        @Nonnull ProcessInstanceTaskEntity thisTask) throws ResponseException {
        return user != null && Objects.equals(thisTask.getAssignedUserId(), user.getId());
    }

    @Nonnull
    @Override
    public GroupLayout getTaskViewLayout(@Nullable UserEntity user,
                                         @Nonnull ProcessDefinitionNodeEntity thisNode,
                                         @Nonnull ProcessInstanceEntity processInstance,
                                         @Nonnull ProcessInstanceTaskEntity thisTask) throws ResponseException {
        var configuration = thisNode.getConfiguration();

        var instructionsContent = configuration
                .get(INSTRUCTIONS_FIELD_ID)
                .getOptionalValue()
                .orElse("Bitte überprüfen und korrigieren Sie die gegebenen Daten.")
                .toString();

        var layout = new GroupLayout();
        layout.setId(getKey() + "-task-view");

        var instructions = new RichText();
        instructions.setId("instructions");
        instructions.setContent(instructionsContent);
        layout.addChild(instructions);

        String json;
        try {
            json = ObjectMapperFactory
                    .getInstance()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(thisTask.getWorkingData());
        } catch (JsonProcessingException e) {
            throw ResponseException.internalServerError(e);
        }

        var content = new RichText();
        content.setId("content");
        content.setContent(json);
        layout.addChild(content);

        return layout;
    }

    @Nonnull
    @Override
    public List<TaskViewEvent> getTaskViewEvents(@Nullable UserEntity user,
                                                 @Nonnull ProcessDefinitionNodeEntity thisNode,
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
                                           @Nonnull ProcessDefinitionNodeEntity thisNode,
                                           @Nonnull Map<String, Object> workingData) throws Exception {
        var configuration = thisNode.getConfiguration();

        var userOptions = configuration
                .get(USER_OPTIONS_FIELD_ID)
                .getOptionalValue()
                .orElse(null);

        if (userOptions instanceof List<?> userIds && !userIds.isEmpty()) {
            var randomUserId = (String) userIds.get((int) (Math.random() * userIds.size()));

            return new ProcessNodeExecutionResultTaskAssigned()
                    .setAssignedUserId(randomUserId);
        } else {
            return new ProcessNodeExecutionResultError()
                    .setMessage("Keine gültigen Benutzeroptionen konfiguriert.");
        }
    }

    @Override
    public Optional<ProcessNodeExecutionResult> update(@Nullable UserEntity user,
                                                       @Nonnull ProcessInstanceEntity processInstance,
                                                       @Nonnull ProcessDefinitionNodeEntity thisNode,
                                                       @Nonnull Map<String, Object> workingData,
                                                       @Nonnull Map<String, Object> updateData,
                                                       @Nonnull String event) throws Exception {

        if (event.equals(ACCEPT_EVENT_NAME)) {
            return Optional.of(ProcessNodeExecutionResultTaskCompleted.of(ACCEPT_PORT_NAME));
        } else {
            return Optional.of(ProcessNodeExecutionResultTaskCompleted.of(REJECT_PORT_NAME));
        }
    }
}
