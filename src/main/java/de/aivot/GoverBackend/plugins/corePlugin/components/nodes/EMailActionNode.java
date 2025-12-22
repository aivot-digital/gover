package de.aivot.GoverBackend.plugins.corePlugin.components.nodes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.models.elements.form.input.RichTextInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.TextInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.plugin.models.PluginComponent;
import de.aivot.GoverBackend.plugins.corePlugin.Core;
import de.aivot.GoverBackend.process.entities.ProcessDefinitionEntity;
import de.aivot.GoverBackend.process.entities.ProcessDefinitionNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessDefinitionVersionEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.enums.ProcessHistoryEventType;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.process.services.ProcessDataService;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class EMailActionNode implements ProcessNodeProvider, PluginComponent {
    private static final String RECIPIENT_FIELD_ID = "to";
    private static final String SUBJECT_FIELD_ID = "subject";
    private static final String CONTENT_FIELD_ID = "content";

    private static final String SUCCESS_PORT_NAME = "output";

    private final GoverConfig goverConfig;
    private final ProcessDataService processDataService;
    private final JavaMailSenderImpl mailSender;

    public EMailActionNode(GoverConfig goverConfig,
                           ProcessDataService processDataService,
                           JavaMailSenderImpl mailSender) {
        this.goverConfig = goverConfig;
        this.processDataService = processDataService;
        this.mailSender = mailSender;
    }

    @Nonnull
    @Override
    public String getKey() {
        return "mail";
    }

    @Override
    public String getParentPluginKey() {
        return Core.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull UserEntity user,
                                                      @Nonnull ProcessDefinitionEntity processDefinition,
                                                      @Nonnull ProcessDefinitionVersionEntity processDefinitionVersion,
                                                      @Nullable ProcessDefinitionNodeEntity thisNode) {
        var layout = new ConfigLayoutElement();
        layout.setId(getKey() + "-config");

        var recipient = new TextInputElement();
        recipient.setId(RECIPIENT_FIELD_ID);
        recipient.setLabel("Adresse der Empfänger:in");
        recipient.setHint("Geben Sie die E-Mail-Adresse der Empfänger:in ein.");
        recipient.setRequired(true);
        layout.addChild(recipient);

        var subject = new TextInputElement();
        subject.setId(SUBJECT_FIELD_ID);
        subject.setLabel("Betreff der E-Mail");
        subject.setHint("Geben Sie den Betreff der E-Mail ein.");
        subject.setRequired(true);
        layout.addChild(subject);

        var content = new RichTextInputElement();
        content.setId(CONTENT_FIELD_ID);
        content.setLabel("Inhalt der E-Mail");
        content.setHint("Geben Sie den Inhalt der E-Mail ein.");
        content.setRequired(true);
        layout.addChild(content);

        return layout;
    }

    @Nonnull
    @Override
    public ProcessNodeType getType() {
        return ProcessNodeType.Action;
    }

    @Nonnull
    @Override
    public String getName() {
        return "E-Mail-Versand";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Versendet eine E-Mail an eine:n Empfänger:in.";
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        SUCCESS_PORT_NAME,
                        "Versendet",
                        "Der Prozess wird hier fortgesetzt, nachdem die E-Mail versendet wurde."
                )
        );
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessInstanceEntity processInstance,
                                           @Nonnull ProcessDefinitionNodeEntity thisNode,
                                           @Nonnull Map<String, Object> workingData) throws Exception {
        var configuration = thisNode.getConfiguration();

        var recipient =
                processDataService
                        .interpolate(
                                workingData,
                                configuration
                                        .get(RECIPIENT_FIELD_ID)
                                        .getOptionalValue()
                                        .orElse("")
                                        .toString()
                        );

        if (StringUtils.isNullOrEmpty(recipient)) {
            return ProcessNodeExecutionResultError.of("Die Empfänger:in darf nicht leer sein.");
        }

        var subject =
                processDataService
                        .interpolate(
                                workingData,
                                configuration
                                        .get(SUBJECT_FIELD_ID)
                                        .getOptionalValue()
                                        .orElse("")
                                        .toString()
                        );

        if (StringUtils.isNullOrEmpty(subject)) {
            return ProcessNodeExecutionResultError.of("Der Betreff darf nicht leer sein.");
        }

        var contentMarkdown = configuration
                .get(CONTENT_FIELD_ID)
                .getOptionalValue()
                .orElse("")
                .toString();

        Parser parser = Parser.builder().build();
        Node document = parser.parse(contentMarkdown);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        var contentHtml = renderer.render(document);

        var content =
                processDataService
                        .interpolate(
                                workingData,
                                contentHtml
                        );

        if (StringUtils.isNullOrEmpty(content)) {
            return ProcessNodeExecutionResultError.of("Der Inhalt darf nicht leer sein.");
        }

        var mimeMessage = mailSender.createMimeMessage();
        var helper = new MimeMessageHelper(mimeMessage, "utf-8");
        helper.setFrom(goverConfig.getFromMail());
        helper.setTo(recipient);
        helper.setSubject(subject);
        helper.setText(content, true);

        try {
            mailSender
                    .send(mimeMessage);
        } catch (Exception e) {
            return ProcessNodeExecutionResultError.of(e);
        }

        var metadata = new HashMap<String, Object>();
        metadata.put("to", recipient);
        metadata.put("subject", subject);
        metadata.put("content", content);

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(SUCCESS_PORT_NAME)
                .setNodeData(metadata)
                .addEvent(ProcessNodeExecutionEvent.of(
                        ProcessHistoryEventType.Complete,
                        "E-Mail versendet an " + recipient,
                        "Die E-Mail wurde erfolgreich versendet."
                ));
    }
}
