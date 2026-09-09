package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.communication.models.ByteArrayCommunicationMessageAttachment;
import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.communication.models.CommunicationMessageAttachment;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.ComputedElementState;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RichTextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.uiPresets.SemiAutomaticMessageConfig;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidAssignment;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionMissingValue;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.prosuna.backend.process.models.*;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultCommunicationRequest;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskAssigned;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUIStaff;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.services.AssignmentContextAssigneeResolverService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentSetService;
import de.aivot.prosuna.backend.process.services.TemplateRenderService;
import de.aivot.prosuna.backend.storage.services.StorageService;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLConnection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Sends one synchronous message through the communication provider selected for an identity.
 */
@Component
public class CommunicationMessageActionNodeV1 implements ProcessNodeDefinition<CommunicationMessageActionNodeV1.Configuration> {
    public static final String NODE_KEY = "communication_message";
    private static final String PORT_OUTPUT = "output";
    private static final String OUTPUT_IDENTITY_ID = "identityId";
    private static final String OUTPUT_BINDING_ID = "communicationProviderBindingId";
    private static final String OUTPUT_SUBJECT = "subject";
    private static final String OUTPUT_BODY = "body";
    private static final String OUTPUT_ATTACHMENT_SET_DATA_KEYS = "attachmentSetDataKeys";
    private static final String OUTPUT_SENT_AT = "sentAt";
    private static final String OUTPUT_SEND_RESULT = "sendResult";

    private static final String STAFF_TASK_ROOT_ID = "root";
    private static final String STAFF_TASK_SUBJECT_FIELD_ID = "subject";
    private static final String STAFF_TASK_CONTENT_FIELD_ID = "body";
    private static final String STAFF_TASK_SEND_EVENT = "send";

    private final TemplateRenderService templateRenderService;
    private final ProcessInstanceAttachmentSetService attachmentSetService;
    private final ProcessInstanceAttachmentService attachmentService;
    private final StorageService storageService;
    private final AssignmentContextAssigneeResolverService assignmentContextAssigneeResolverService;

    public CommunicationMessageActionNodeV1(
            TemplateRenderService templateRenderService,
            ProcessInstanceAttachmentSetService attachmentSetService,
            ProcessInstanceAttachmentService attachmentService,
            StorageService storageService,
            AssignmentContextAssigneeResolverService assignmentContextAssigneeResolverService
    ) {
        this.templateRenderService = templateRenderService;
        this.attachmentSetService = attachmentSetService;
        this.attachmentService = attachmentService;
        this.storageService = storageService;
        this.assignmentContextAssigneeResolverService = assignmentContextAssigneeResolverService;
    }

    @Nonnull
    @Override
    public String getComponentKey() {
        return NODE_KEY;
    }

    @Nonnull
    @Override
    public String getComponentVersion() {
        return "1.0.0";
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return CorePlugin.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public ProcessNodeType getType() {
        return ProcessNodeType.Action;
    }

    @Nonnull
    @Override
    public ProcessNodeExecutionType[] getExecutionTypes() {
        return new ProcessNodeExecutionType[]{
                ProcessNodeExecutionType.Automatic,
                ProcessNodeExecutionType.SemiAutomatic
        };
    }

    @Nonnull
    @Override
    public String getName() {
        return "Nachricht an Identität senden";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Sendet eine Nachricht über den für eine Prozessidentität ausgewählten Kommunikationsweg.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                Sendet eine Nachricht über den Kommunikationsweg, den die Kund:in für eine Identität ausgewählt hat.

                Betreff und Inhalt werden entweder automatisch aus Vorlagen erzeugt oder vor dem Versand durch eine Mitarbeiter:in bearbeitet. Optional können vollständige Anlagensätze angehängt werden. Nach dem synchronen Versand stehen die verwendete Identität, die Kommunikationsanbindung, die Nachrichtendaten und das Ergebnis des Kommunikationsanbieters als Ausgänge bereit.
                """;
    }

    @Nonnull
    @Override
    public Class<Configuration> getNodeConfigurationClass() {
        return Configuration.class;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(
            @Nonnull ProcessNodeDefinitionConfigurationLayoutContext context
    ) throws ResponseException {
        final ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper.createFromPOJO(Configuration.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(
                    e,
                    "Das Konfigurationslayout für den Nachrichtenversand konnte nicht erstellt werden: %s",
                    e.getMessage()
            );
        }

        layout.findChild(SemiAutomaticMessageConfig.GROUP_ID, GroupLayoutElement.class)
                .ifPresent(group -> SemiAutomaticMessageConfig.initConfigurationLayout(
                        group,
                        context.thisNode().getProcessId(),
                        context.thisNode().getProcessVersion()
                ));

        return layout;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(new ProcessNodePort(
                PORT_OUTPUT,
                "Nachricht versendet",
                "Der Prozess wird fortgesetzt, nachdem die Nachricht synchron versendet wurde."
        ));
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(OUTPUT_IDENTITY_ID, "Identität", "ID der adressierten Prozessidentität.", "string"),
                new ProcessNodeOutput(OUTPUT_BINDING_ID, "Kommunikationsanbindung", "ID der verwendeten Kommunikationsanbindung.", "number | null"),
                new ProcessNodeOutput(OUTPUT_SUBJECT, "Betreff", "Betreff der versendeten Nachricht.", "string"),
                new ProcessNodeOutput(OUTPUT_BODY, "Inhalt", "Inhalt der versendeten Nachricht.", "string"),
                new ProcessNodeOutput(OUTPUT_ATTACHMENT_SET_DATA_KEYS, "Anlagensätze", "Datenschlüssel der angehängten Anlagensätze.", "Array<string>"),
                new ProcessNodeOutput(OUTPUT_SENT_AT, "Versandzeitpunkt", "Zeitpunkt des erfolgreichen Versands.", "string"),
                new ProcessNodeOutput(OUTPUT_SEND_RESULT, "Versandresultat", "Resultat des Versandvorgangs, wie es vom Kommunikationsanbieter zurückgegeben wurde.", "Record<string, unknown>")
        );
    }

    @Override
    public ProcessNodeExecutionResult init(
            @Nonnull ProcessNodeExecutionInitContext<Configuration> context
    ) throws ProcessNodeExecutionException {
        var configuration = context.getConfigurationOfExecutingNode();

        if (SemiAutomaticMessageConfig.isAutomatic(configuration.messageConfig)) {
            return initAutomatic(context, configuration);
        }
        if (SemiAutomaticMessageConfig.isManual(configuration.messageConfig)) {
            return initManual(context, configuration);
        }

        var executionType = configuration.messageConfig == null
                ? null
                : StringUtils.toNullableTrimmedString(configuration.messageConfig.executionType);
        throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                "Ungültige Ausführungsart für den Nachrichtenversand. Erwartet werden entweder %s oder %s. Übergeben wurde: %s",
                StringUtils.quote(SemiAutomaticMessageConfig.LayoutConfig.EXECUTION_TYPE_AUTOMATIC),
                StringUtils.quote(SemiAutomaticMessageConfig.LayoutConfig.EXECUTION_TYPE_MANUAL),
                StringUtils.quote(executionType)
        );
    }

    @Nonnull
    private ProcessNodeExecutionResult initAutomatic(
            @Nonnull ProcessNodeExecutionInitContext<Configuration> context,
            @Nonnull Configuration configuration
    ) throws ProcessNodeExecutionException {
        var automaticContent = requireAutomaticContent(configuration);
        var subject = renderRequiredTemplate(
                context.getCurrentProcessExecutionData(),
                automaticContent.subject,
                "Betreff"
        );
        var content = renderRequiredTemplate(
                context.getCurrentProcessExecutionData(),
                automaticContent.content,
                "Nachrichtentext"
        );

        return createCommunicationResult(
                configuration,
                context.getCurrentProcessExecutionData(),
                context.getThisProcessInstance(),
                subject,
                content
        );
    }

    @Nonnull
    private ProcessNodeExecutionResult initManual(
            @Nonnull ProcessNodeExecutionInitContext<Configuration> context,
            @Nonnull Configuration configuration
    ) throws ProcessNodeExecutionException {
        var manualContent = requireManualContent(configuration);
        var assigneeUserId = assignmentContextAssigneeResolverService
                .resolveAssignee(
                        context.getThisNode().getProcessId(),
                        context.getThisNode().getProcessVersion(),
                        context.getThisProcessInstance().getId(),
                        context.getThisNode().getId(),
                        context.getThisTask().getId(),
                        context.getThisTask().getPreviousProcessNodeId(),
                        context.getThisProcessInstance().getAssignedUserId(),
                        manualContent.assignmentContext,
                        List.of(ProcessPermissionProvider.PROCESS_INSTANCE_EDIT_TASK)
                )
                .orElseThrow(() -> new ProcessNodeExecutionExceptionInvalidAssignment(
                        "Für das Prozesselement %s konnte keine geeignete Bearbeiter:in im konfigurierten Personenkreis ermittelt werden.",
                        StringUtils.quote(context.getThisNode().resolveName(this))
                ));

        return ProcessNodeExecutionResultTaskAssigned
                .of(assigneeUserId)
                .setRuntimeData(new LinkedHashMap<>(context.getThisTask().getRuntimeData()))
                .setProcessData(context.getCurrentProcessExecutionData().getProcessData());
    }

    @Nonnull
    @Override
    public ProcessNodeStaffView getStaffTaskView(
            @Nonnull ProcessNodeExecutionContextUIStaff<Configuration> context
    ) throws ResponseException {
        var subjectField = new TextInputElement();
        subjectField.setId(STAFF_TASK_SUBJECT_FIELD_ID);
        subjectField.setLabel("Betreff der Nachricht");
        subjectField.setRequired(true);

        var contentField = new RichTextInputElement();
        contentField.setId(STAFF_TASK_CONTENT_FIELD_ID);
        contentField.setLabel("Inhalt der Nachricht");
        contentField.setRequired(true);

        var root = new GroupLayoutElement();
        root.setId(STAFF_TASK_ROOT_ID);
        root.setChildren(new LinkedList<>(List.of(subjectField, contentField)));

        var manualContent = requireManualContentForStaffView(context.getConfigurationOfExecutingNode());
        var taskViewData = new AuthoredElementValues();

        try {
            taskViewData.put(
                    STAFF_TASK_SUBJECT_FIELD_ID,
                    templateRenderService.interpolate(context.getCurrentProcessExecutionData(), manualContent.subject)
            );
            taskViewData.put(
                    STAFF_TASK_CONTENT_FIELD_ID,
                    templateRenderService.interpolate(context.getCurrentProcessExecutionData(), manualContent.content)
            );
        } catch (RuntimeException e) {
            throw ResponseException.internalServerError(
                    e,
                    "Die Nachrichtenvorlage konnte nicht gerendert werden: %s",
                    e.getMessage()
            );
        }

        return ProcessNodeStaffView.of(
                context,
                root,
                List.of(new TaskViewEvent("Nachricht versenden", STAFF_TASK_SEND_EVENT)),
                taskViewData
        );
    }

    @Nonnull
    @Override
    public Optional<ProcessNodeExecutionResult> onEventFromStaffTaskView(
            @Nonnull ProcessNodeExecutionContextUIStaff<Configuration> context,
            @Nonnull AuthoredElementValues update,
            @Nonnull String event
    ) throws ResponseException, ProcessNodeExecutionException {
        if (!STAFF_TASK_SEND_EVENT.equals(event)) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    "Das Event %s wird von diesem Prozesselement nicht unterstützt.",
                    StringUtils.quote(event)
            );
        }

        var configuration = context.getConfigurationOfExecutingNode();
        if (!SemiAutomaticMessageConfig.isManual(configuration.messageConfig)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Nachricht kann nur im manuellen Ausführungsmodus über eine Aufgabe versendet werden."
            );
        }

        var subject = StringUtils.toNullableTrimmedString(update.get(STAFF_TASK_SUBJECT_FIELD_ID));
        var content = StringUtils.toNullableTrimmedString(update.get(STAFF_TASK_CONTENT_FIELD_ID));
        validateStaffMessage(subject, content);

        return Optional.of(createCommunicationResult(
                configuration,
                context.getCurrentProcessExecutionData(),
                context.getThisProcessInstance(),
                subject,
                content
        ));
    }

    @Nonnull
    private ProcessNodeExecutionResult createCommunicationResult(
            @Nonnull Configuration configuration,
            @Nonnull ProcessExecutionData processExecutionData,
            @Nonnull ProcessInstanceEntity processInstance,
            @Nonnull String subject,
            @Nonnull String content
    ) throws ProcessNodeExecutionException {
        var identityId = requireIdentityId(configuration.identityId);
        var identity = processInstance.getIdentities().get(identityId);
        if (identity == null) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Die konfigurierte Identität %s ist in der Prozessinstanz nicht vorhanden.",
                    StringUtils.quote(identityId)
            );
        }

        var attachmentSetDataKeys = configuration.attachmentSetDataKeys == null
                ? List.<String>of()
                : configuration.attachmentSetDataKeys;
        var attachments = resolveAttachments(processInstance, attachmentSetDataKeys);
        var sentAt = Instant.now();

        var nodeData = new LinkedHashMap<String, Object>();
        nodeData.put(OUTPUT_IDENTITY_ID, identityId);
        nodeData.put(OUTPUT_BINDING_ID, identity.communicationProviderBindingId());
        nodeData.put(OUTPUT_SUBJECT, subject);
        nodeData.put(OUTPUT_BODY, content);
        nodeData.put(OUTPUT_ATTACHMENT_SET_DATA_KEYS, attachmentSetDataKeys);
        nodeData.put(OUTPUT_SENT_AT, sentAt);

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_OUTPUT)
                .setProcessData(processExecutionData.getProcessData())
                .setNodeData(nodeData)
                .setCommunicationRequest(new ProcessNodeExecutionResultCommunicationRequest(
                        identityId,
                        new CommunicationMessage(
                                subject,
                                content,
                                content,
                                List.of(),
                                sentAt,
                                attachments,
                                null,
                                null
                        ),
                        OUTPUT_SEND_RESULT
                ));
    }

    @Nonnull
    private String requireIdentityId(@Nullable String rawIdentityId)
            throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var identityId = StringUtils.toNullableTrimmedString(rawIdentityId);
        if (identityId == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Für den Nachrichtenversand muss eine Prozessidentität konfiguriert sein."
            );
        }
        return identityId;
    }

    @Nonnull
    private SemiAutomaticMessageConfig.AutomaticContent requireAutomaticContent(
            @Nonnull Configuration configuration
    ) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var content = configuration.messageConfig == null ? null : configuration.messageConfig.automaticContent;
        if (content == null
                || StringUtils.toNullableTrimmedString(content.subject) == null
                || StringUtils.toNullableTrimmedString(content.content) == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Für den automatischen Versand müssen Betreff und Nachrichtentext konfiguriert sein."
            );
        }
        return content;
    }

    @Nonnull
    private SemiAutomaticMessageConfig.ManualContent requireManualContent(
            @Nonnull Configuration configuration
    ) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var content = configuration.messageConfig == null ? null : configuration.messageConfig.manualContent;
        if (content == null
                || StringUtils.toNullableTrimmedString(content.subject) == null
                || StringUtils.toNullableTrimmedString(content.content) == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Für den manuellen Versand müssen Vorlagen für Betreff und Nachrichtentext konfiguriert sein."
            );
        }
        return content;
    }

    @Nonnull
    private SemiAutomaticMessageConfig.ManualContent requireManualContentForStaffView(
            @Nonnull Configuration configuration
    ) throws ResponseException {
        try {
            return requireManualContent(configuration);
        } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
            throw ResponseException.internalServerError(e, e.getMessage());
        }
    }

    @Nonnull
    private String renderRequiredTemplate(
            @Nonnull ProcessExecutionData processExecutionData,
            @Nonnull String template,
            @Nonnull String fieldName
    ) throws ProcessNodeExecutionException {
        final String rendered;
        try {
            rendered = StringUtils.toNullableTrimmedString(
                    templateRenderService.interpolate(processExecutionData, template)
            );
        } catch (RuntimeException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die Vorlage für %s konnte nicht gerendert werden: %s",
                    fieldName,
                    e.getMessage()
            );
        }

        if (rendered == null) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der gerenderte Wert für %s ist leer.",
                    fieldName
            );
        }
        return rendered;
    }

    private static void validateStaffMessage(
            @Nullable String subject,
            @Nullable String content
    ) throws ResponseException {
        var derivedRuntimeData = new DerivedRuntimeElementData();
        if (subject == null) {
            derivedRuntimeData.getElementStates().put(
                    STAFF_TASK_SUBJECT_FIELD_ID,
                    new ComputedElementState().setError("Der Betreff der Nachricht darf nicht leer sein.")
            );
        }
        if (content == null) {
            derivedRuntimeData.getElementStates().put(
                    STAFF_TASK_CONTENT_FIELD_ID,
                    new ComputedElementState().setError("Der Inhalt der Nachricht darf nicht leer sein.")
            );
        }
        if (derivedRuntimeData.hasAnyError()) {
            throw ResponseException.badRequest(derivedRuntimeData);
        }
    }

    @Nonnull
    private List<CommunicationMessageAttachment> resolveAttachments(
            @Nonnull ProcessInstanceEntity processInstance,
            @Nonnull List<String> attachmentSetDataKeys
    ) throws ProcessNodeExecutionException {
        var resolved = new ArrayList<CommunicationMessageAttachment>();
        for (var rawDataKey : attachmentSetDataKeys) {
            var dataKey = StringUtils.toNullableTrimmedString(rawDataKey);
            if (dataKey == null) {
                continue;
            }

            var sets = attachmentSetService.findAllByProcessInstanceIdAndDataKey(processInstance.getId(), dataKey);
            if (sets.isEmpty()) {
                throw new ProcessNodeExecutionExceptionMissingValue(
                        "Der konfigurierte Anlagensatz %s wurde nicht gefunden.",
                        StringUtils.quote(dataKey)
                );
            }

            var entities = new ArrayList<ProcessInstanceAttachmentEntity>();
            for (var set : sets) {
                entities.addAll(attachmentService.findAllByAttachmentSetId(set.getId()));
            }
            if (entities.isEmpty()) {
                throw new ProcessNodeExecutionExceptionMissingValue(
                        "Der konfigurierte Anlagensatz %s enthält keine Anhänge.",
                        StringUtils.quote(dataKey)
                );
            }

            for (var entity : entities) {
                try (var attachmentContent = storageService.getDocumentContent(
                        entity.getStorageProviderId(),
                        entity.getStoragePathFromRoot()
                )) {
                    resolved.add(new ByteArrayCommunicationMessageAttachment(
                            entity.getFileName(),
                            URLConnection.guessContentTypeFromName(entity.getFileName()),
                            attachmentContent.readAllBytes()
                    ));
                } catch (IOException | ResponseException e) {
                    throw new ProcessNodeExecutionExceptionUnknown(
                            e,
                            "Der Anhang %s konnte nicht geladen werden: %s",
                            StringUtils.quote(entity.getFileName()),
                            e.getMessage()
                    );
                }
            }
        }
        return resolved;
    }

    @Nonnull
    @Override
    public AuthoredElementValues cleanConfigurationForExport(@Nonnull AuthoredElementValues configuration) {
        configuration.remove(SemiAutomaticMessageConfig.ManualContent.ASSIGNMENT_FIELD_ID);
        return configuration;
    }

    /**
     * Configuration shown in the process-node editor.
     */
    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class Configuration {
        public static final String IDENTITY_ID_FIELD_ID = "identityId";
        public static final String ATTACHMENTS_FIELD_ID = "attachmentSetDataKeys";

        /**
         * Logical process identity receiving the message. Options come from incoming node metadata;
         * a null or blank value makes the node configuration invalid at execution time.
         */
        @InputElementPOJOBinding(id = IDENTITY_ID_FIELD_ID, type = ElementType.ProcessIdentityIdInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Identität"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Identität, an die die Nachricht über den bei der Anmeldung gewählten Kommunikationsweg gesendet wird."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        @Nullable
        public String identityId;

        /**
         * Optional process attachment sets whose files are included in the outgoing message. A null list
         * is normalized to an empty list; configured missing or empty sets fail execution.
         */
        @InputElementPOJOBinding(id = ATTACHMENTS_FIELD_ID, type = ElementType.ProcessInstanceAttachmentSetSelect, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Anlagensätze"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Optionale Anlagensätze, deren Dateien mit der Nachricht versendet werden."),
                @ElementPOJOBindingProperty(key = "required", boolValue = false)
        })
        @Nullable
        public List<String> attachmentSetDataKeys;

        /**
         * Dispatch mode, message templates and optional staff assignment.
         */
        @Nullable
        public SemiAutomaticMessageConfig.LayoutConfig messageConfig;
    }
}
