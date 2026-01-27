package de.aivot.GoverBackend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.elements.ElementVisibilityFunctions;
import de.aivot.GoverBackend.elements.models.elements.form.input.RadioInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.RadioInputElementOption;
import de.aivot.GoverBackend.elements.models.elements.form.input.RichTextInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.TextInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.nocode.models.NoCodeExpression;
import de.aivot.GoverBackend.nocode.models.NoCodeReference;
import de.aivot.GoverBackend.nocode.models.NoCodeStaticValue;
import de.aivot.GoverBackend.plugin.models.PluginComponent;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.plugins.core.v1.operators.common.NoCodeEqualsOperator;
import de.aivot.GoverBackend.process.entities.ProcessEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessVersionEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.enums.ProcessHistoryEventType;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionMissingValue;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.process.services.ProcessDataService;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.mail.MessagingException;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class EMailActionNode implements ProcessNodeDefinition, PluginComponent {
    private static final String RECIPIENT_FIELD_ID = "to";
    private static final String BCC_RECIPIENT_FIELD_ID = "bcc";
    private static final String EXECUTION_TYPE_FIELD_ID = "executionType";
    private static final String SUBJECT_FIELD_ID = "subject";
    private static final String CONTENT_FIELD_ID = "content";

    private static final String EXECUTION_TYPE_MANUAL = "manual";
    private static final String EXECUTION_TYPE_AUTOMATIC = "automatic";

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

    @Override
    public @Nonnull String getKey() {
        return "mail";
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
        return "E-Mail versenden";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Sendet automatisiert oder manuell E-Mails an ausgewählte Empfänger:innen.";
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionContextConfig context) {
        var layout = new ConfigLayoutElement();
        layout.setId(getKey() + "-config");

        var recipient = new TextInputElement();
        recipient.setId(RECIPIENT_FIELD_ID);
        recipient.setLabel("Empfänger:innen");
        recipient.setHint("Kommaseparierte Angabe der Empfänger:innen");
        recipient.setRequired(true);
        layout.addChild(recipient);

        var bccRecipient = new TextInputElement();
        bccRecipient.setId(BCC_RECIPIENT_FIELD_ID);
        bccRecipient.setLabel("BCC-Empfänger:innen");
        bccRecipient.setHint("Angabe weiterer Empfänger:innen als Blind Carbon Copy (BCC)");
        bccRecipient.setRequired(false);
        layout.addChild(bccRecipient);

        var selectType = new RadioInputElement();
        selectType.setId(EXECUTION_TYPE_FIELD_ID);
        selectType
                .setLabel("Ausführungsart")
                .setHint("Auswahl, ob Nachricht automatisch versendet oder vorher durch eine Sachbearbeiter:in editiert werden soll")
                .setRequired(true);
        selectType.setOptions(List.of(
                RadioInputElementOption.of(EXECUTION_TYPE_AUTOMATIC, "Automatisch versenden"),
                RadioInputElementOption.of(EXECUTION_TYPE_MANUAL, "Vor dem Versand bearbeiten")
        ));
        layout.addChild(selectType);

        layout.addChild(getAutomaticConfigGroup());
        layout.addChild(getManualConfiguration());

        // TODO: Add signature select and attachment select

        return layout;
    }

    private GroupLayoutElement getAutomaticConfigGroup() {
        var visibilityExpression = new NoCodeExpression(
                NoCodeEqualsOperator.OPERATOR_ID,
                new NoCodeReference(EXECUTION_TYPE_FIELD_ID),
                new NoCodeStaticValue(EXECUTION_TYPE_AUTOMATIC)
        );
        var visibilityFunc = new ElementVisibilityFunctions()
                .setNoCode(visibilityExpression)
                .setReferencedIds(List.of(
                        EXECUTION_TYPE_FIELD_ID
                ));

        var automaticGroup = new GroupLayoutElement();
        automaticGroup
                .setId("automatic-group")
                .setVisibility(visibilityFunc);

        var subject = new TextInputElement();
        subject.setId(SUBJECT_FIELD_ID);
        subject
                .setLabel("Betreff der E-Mail")
                .setHint("Geben Sie den Betreff der E-Mail ein.")
                .setRequired(true);
        automaticGroup.addChild(subject);

        var content = new RichTextInputElement();
        content.setId(CONTENT_FIELD_ID);
        content
                .setLabel("Nachrichtentext")
                .setHint("Geben Sie den Inhalt der E-Mail ein.")
                .setRequired(true);
        automaticGroup.addChild(content);

        return automaticGroup;
    }

    private GroupLayoutElement getManualConfiguration() {
        var visibilityExpression = new NoCodeExpression(
                NoCodeEqualsOperator.OPERATOR_ID,
                new NoCodeReference(EXECUTION_TYPE_FIELD_ID),
                new NoCodeStaticValue(EXECUTION_TYPE_MANUAL)
        );
        var visibilityFunc = new ElementVisibilityFunctions()
                .setNoCode(visibilityExpression)
                .setReferencedIds(List.of(
                        EXECUTION_TYPE_FIELD_ID
                ));

        var manualGroup = new GroupLayoutElement();
        manualGroup
                .setId("manual-group")
                .setVisibility(visibilityFunc);

        var subject = new TextInputElement();
        subject.setId(SUBJECT_FIELD_ID);
        subject
                .setLabel("Vorlage Betreff der E-Mail")
                .setHint("Geben Sie den Betreff der E-Mail ein.")
                .setRequired(true);
        manualGroup.addChild(subject);

        var content = new RichTextInputElement();
        content.setId(CONTENT_FIELD_ID);
        content
                .setLabel("Vorlage Nachrichtentext")
                .setHint("Geben Sie den Inhalt der E-Mail ein.")
                .setRequired(true);
        manualGroup.addChild(content);

        // TODO

        return manualGroup;
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
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionContextInit context) throws ProcessNodeExecutionException {
        var configuration = context
                .getThisNode()
                .getConfiguration();

        var executionType =
                configuration
                        .get(EXECUTION_TYPE_FIELD_ID)
                        .getOptionalValue()
                        .orElse(EXECUTION_TYPE_AUTOMATIC)
                        .toString();

        switch (executionType) {
            case EXECUTION_TYPE_AUTOMATIC -> {
                return initAutomatic(context, configuration);
            }
            case EXECUTION_TYPE_MANUAL -> {
                return initManual(context, configuration);
            }
            default -> throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Ungültige Ausführungsart für den E-Mail-Versand. Erwartet werden entweder %s oder %s. Übergeben wurde: %s",
                    StringUtils.quote(EXECUTION_TYPE_AUTOMATIC),
                    StringUtils.quote(EXECUTION_TYPE_MANUAL),
                    StringUtils.quote(executionType)
            );
        }
    }

    private ProcessNodeExecutionResult initAutomatic(@Nonnull ProcessNodeExecutionContextInit context,
                                                     @Nonnull ElementData config) throws ProcessNodeExecutionException {
        var recipientsStr = processDataService
                .interpolate(
                        context.getProcessData(),
                        config
                                .get(RECIPIENT_FIELD_ID)
                                .getOptionalValue()
                                .orElse("")
                                .toString()
                );

        if (StringUtils.isNullOrEmpty(recipientsStr)) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Die Empfänger:in für die E-Mail wurde nicht angegeben."
            );
        }
        var recipients = recipientsStr.split(",");

        var recipientsBccStr = processDataService
                .interpolate(
                        context.getProcessData(),
                        config
                                .get(BCC_RECIPIENT_FIELD_ID)
                                .getOptionalValue()
                                .orElse("")
                                .toString()
                );
        var recipientsBCC = StringUtils.isNullOrEmpty(recipientsBccStr) ? null : recipientsBccStr.split(",");

        var subject = processDataService
                .interpolate(
                        context.getProcessData(),
                        config
                                .get(SUBJECT_FIELD_ID)
                                .getOptionalValue()
                                .orElse("")
                                .toString()
                );

        if (StringUtils.isNullOrEmpty(subject)) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der Betreff für die E-Mail wurde nicht angegeben."
            );
        }

        var contentMarkdown = config
                .get(CONTENT_FIELD_ID)
                .getOptionalValue()
                .orElse("")
                .toString();

        if (StringUtils.isNullOrEmpty(contentMarkdown)) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der Inhalt für die E-Mail wurde nicht angegeben."
            );
        }

        Parser parser = Parser.builder().build();
        Node document = parser.parse(contentMarkdown);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        var contentHtml = renderer.render(document);

        var content =
                processDataService
                        .interpolate(
                                context.getProcessData(),
                                contentHtml
                        );

        if (StringUtils.isNullOrEmpty(content)) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der Inhalt für die E-Mail wurde nicht angegeben."
            );
        }

        var mimeMessage = mailSender.createMimeMessage();
        var helper = new MimeMessageHelper(mimeMessage, "utf-8");

        try {
            helper.setFrom(goverConfig.getFromMail());
            helper.setTo(recipients);
            if (recipientsBCC != null) {
                helper.setBcc(recipientsBCC);
            }
            helper.setSubject(subject);
            helper.setText(content, true);
        } catch (MessagingException exception) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    exception,
                    "Beim Erstellen der E-Mail ist ein Fehler aufgetreten: %s",
                    exception.getMessage()
            );
        }

        try {
            mailSender
                    .send(mimeMessage);
        } catch (MailException exception) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    exception,
                    "Beim Versenden der E-Mail ist ein Fehler aufgetreten: %s",
                    exception.getMessage()
            );
        }

        var metadata = new HashMap<String, Object>();
        metadata.put("to", recipients);
        metadata.put("bcc", recipientsBCC);
        metadata.put("subject", subject);
        metadata.put("content", content);

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(SUCCESS_PORT_NAME)
                .setNodeData(metadata);
    }

    private ProcessNodeExecutionResult initManual(@Nonnull ProcessNodeExecutionContextInit context,
                                                  @Nonnull ElementData config) throws ProcessNodeExecutionException {
        throw new ProcessNodeExecutionExceptionUnknown(
                "This functionality is not yet implemented."
        );
    }
}
