package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import de.aivot.prosuna.backend.communication.models.ByteArrayCommunicationMessageAttachment;
import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.communication.models.CommunicationMessageAttachment;
import de.aivot.prosuna.backend.communication.services.CommunicationService;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionMissingValue;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeOutput;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
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
import java.util.List;

/** Sends one synchronous message through the communication provider selected for an identity. */
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

    private final CommunicationService communicationService;
    private final TemplateRenderService templateRenderService;
    private final ProcessInstanceAttachmentSetService attachmentSetService;
    private final ProcessInstanceAttachmentService attachmentService;
    private final StorageService storageService;

    public CommunicationMessageActionNodeV1(CommunicationService communicationService,
                                            TemplateRenderService templateRenderService,
                                            ProcessInstanceAttachmentSetService attachmentSetService,
                                            ProcessInstanceAttachmentService attachmentService,
                                            StorageService storageService) {
        this.communicationService = communicationService;
        this.templateRenderService = templateRenderService;
        this.attachmentSetService = attachmentSetService;
        this.attachmentService = attachmentService;
        this.storageService = storageService;
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
    public String getName() {
        return "Nachricht an Identität senden";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Sendet eine Nachricht über den Kommunikationsweg, den die Kund:in für eine Identität ausgewählt hat.";
    }

    @Nonnull
    @Override
    public Class<Configuration> getNodeConfigurationClass() {
        return Configuration.class;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        try {
            return ElementPOJOMapper.createFromPOJO(Configuration.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e, "Das Konfigurationslayout für den Nachrichtenversand konnte nicht erstellt werden: %s", e.getMessage());
        }
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
                new ProcessNodeOutput(OUTPUT_IDENTITY_ID, "Identität", "ID der adressierten Prozessidentität."),
                new ProcessNodeOutput(OUTPUT_BINDING_ID, "Kommunikationsanbindung", "ID der verwendeten Kommunikationsanbindung."),
                new ProcessNodeOutput(OUTPUT_SUBJECT, "Betreff", "Gerenderter Betreff der Nachricht."),
                new ProcessNodeOutput(OUTPUT_BODY, "Inhalt", "Gerenderter Inhalt der Nachricht."),
                new ProcessNodeOutput(OUTPUT_ATTACHMENT_SET_DATA_KEYS, "Anlagensätze", "Datenschlüssel der angehängten Anlagensätze."),
                new ProcessNodeOutput(OUTPUT_SENT_AT, "Versandzeitpunkt", "Zeitpunkt des erfolgreichen Versands.")
        );
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<Configuration> context) throws ProcessNodeExecutionException {
        var configuration = context.getConfigurationOfExecutingNode();
        var identityId = StringUtils.toNullableTrimmedString(configuration.identityId);
        if (identityId == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Es wurde keine Prozessidentität ausgewählt.");
        }

        var identity = context.getThisProcessInstance()
                .getIdentities()
                .get(identityId);
        if (identity == null) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Die konfigurierte Identität %s ist in der Prozessinstanz nicht vorhanden.",
                    StringUtils.quote(identityId)
            );
        }

        final String subject;
        final String body;
        try {
            subject = StringUtils.toNullableTrimmedString(templateRenderService.interpolate(
                    context.getCurrentProcessExecutionData(), configuration.subject));
            body = StringUtils.toNullableTrimmedString(templateRenderService.interpolate(
                    context.getCurrentProcessExecutionData(), configuration.body));
        } catch (RuntimeException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die Nachrichtenvorlage konnte nicht verarbeitet werden: %s",
                    e.getMessage()
            );
        }
        if (subject == null) {
            throw new ProcessNodeExecutionExceptionMissingValue("Der gerenderte Betreff der Nachricht ist leer.");
        }
        if (body == null) {
            throw new ProcessNodeExecutionExceptionMissingValue("Der gerenderte Inhalt der Nachricht ist leer.");
        }

        var attachmentSetDataKeys = configuration.attachmentSetDataKeys == null
                ? List.<String>of()
                : configuration.attachmentSetDataKeys;
        var attachments = resolveAttachments(context, attachmentSetDataKeys);
        var sentAt = Instant.now();

        try {
            communicationService.sendMessage(identity, new CommunicationMessage(
                    subject,
                    body,
                    sentAt,
                    attachments
            ));
        } catch (CommunicationException e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Die Nachricht an die Identität %s konnte nicht versendet werden: %s",
                    StringUtils.quote(identityId),
                    e.getMessage()
            );
        }

        var nodeData = new LinkedHashMap<String, Object>();
        nodeData.put(OUTPUT_IDENTITY_ID, identityId);
        nodeData.put(OUTPUT_BINDING_ID, identity.communicationProviderBindingId());
        nodeData.put(OUTPUT_SUBJECT, subject);
        nodeData.put(OUTPUT_BODY, body);
        nodeData.put(OUTPUT_ATTACHMENT_SET_DATA_KEYS, attachmentSetDataKeys);
        nodeData.put(OUTPUT_SENT_AT, sentAt);

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_OUTPUT)
                .setProcessData(context.getCurrentProcessExecutionData().getProcessData())
                .setNodeData(nodeData);
    }

    @Nonnull
    private List<CommunicationMessageAttachment> resolveAttachments(
            @Nonnull ProcessNodeExecutionInitContext<Configuration> context,
            @Nonnull List<String> attachmentSetDataKeys
    ) throws ProcessNodeExecutionException {
        var resolved = new ArrayList<CommunicationMessageAttachment>();
        for (var rawDataKey : attachmentSetDataKeys) {
            var dataKey = StringUtils.toNullableTrimmedString(rawDataKey);
            if (dataKey == null) continue;

            var sets = attachmentSetService.findAllByProcessInstanceIdAndDataKey(
                    context.getThisProcessInstance().getId(), dataKey);
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
                try (var content = storageService.getDocumentContent(
                        entity.getStorageProviderId(), entity.getStoragePathFromRoot())) {
                    resolved.add(new ByteArrayCommunicationMessageAttachment(
                            entity.getFileName(),
                            URLConnection.guessContentTypeFromName(entity.getFileName()),
                            content.readAllBytes()
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

    /** Configuration shown in the process-node editor. */
    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class Configuration {
        public static final String IDENTITY_ID_FIELD_ID = "identityId";
        public static final String SUBJECT_FIELD_ID = "subject";
        public static final String BODY_FIELD_ID = "body";
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
         * Template for the subject. Process-data expressions are rendered immediately before dispatch;
         * a null, blank, or blank-rendering value prevents dispatch.
         */
        @InputElementPOJOBinding(id = SUBJECT_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Betreff"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Vorlage für den Betreff der Nachricht."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        @Nullable
        public String subject;

        /**
         * Markdown-capable message template rendered against the current process data. A null, blank,
         * or blank-rendering value prevents dispatch.
         */
        @InputElementPOJOBinding(id = BODY_FIELD_ID, type = ElementType.RichTextInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Nachrichteninhalt"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Vorlage für den Inhalt der Nachricht."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        @Nullable
        public String body;

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
    }
}
