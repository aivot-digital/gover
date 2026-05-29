package de.aivot.GoverBackend.plugins.form.v1.nodes;

import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.enums.ElementDisplayContext;
import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.TextInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.TextInputElementPattern;
import de.aivot.GoverBackend.elements.models.elements.form.input.UiDefinitionInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.elements.utils.ElementStreamUtils;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.plugin.models.PluginComponent;
import de.aivot.GoverBackend.plugins.form.FormPlugin;
import de.aivot.GoverBackend.plugins.form.v1.services.FormLayoutCleanerService;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.filters.ProcessNodeFilter;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeDefinitionTestingLayoutContext;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.GoverBackend.process.repositories.ProcessNodeRepository;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Component
public class FormTriggerNodeV1 implements ProcessNodeDefinition<FormTriggerConfigV1>, PluginComponent {
    public static final String NODE_KEY = "form";
    private static final String PORT_NAME = "input";

    public static final String DATA_KEY_PAYLOAD = "payload";
    public static final String DATA_KEY_UNMAPPED = "unmapped";
    public static final String DATA_KEY_ATTACHMENTS = "attachments";

    private final GoverConfig goverConfig;
    private final ProcessNodeRepository processNodeRepository;

    public FormTriggerNodeV1(GoverConfig goverConfig,
                             ProcessNodeRepository processNodeRepository) {
        this.goverConfig = goverConfig;
        this.processNodeRepository = processNodeRepository;
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return FormPlugin.PLUGIN_KEY;
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
    public String getName() {
        return "Formulareingang";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Wird durch einen Formulareingang ausgelöst";
    }

    @Nonnull
    @Override
    public ProcessNodeType getType() {
        return ProcessNodeType.Trigger;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME,
                        "Dateneingang",
                        "Es wurden Daten von einem Formular empfangen."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        "payload",
                        "Zugewiesene Formulardaten",
                        "Alle Formulardaten, welche über einen Datenschlüssel zugewiesen wurden"
                ),
                new ProcessNodeOutput(
                        "unmapped",
                        "Alle Formulardaten",
                        "Alle Formulardaten, unabhängig, ob diese über einen Datenschlüssel zugewiesen wurden oder nicht"
                ),
                new ProcessNodeOutput(
                        "attachments",
                        "Anlagen",
                        "Eine Liste aller Anlagen, die über dieses Formular hochgeladen wurden."
                )
        );
    }

    @Override
    public List<ProcessDataKeyHint> calculateProcessDataKeyHints(@Nonnull ProcessNodeEntity processNodeEntity,
                                                                 @Nonnull FormTriggerConfigV1 configuration,
                                                                 @Nonnull List<ProcessDataKeyHint> previousDataKeyHints) {
        var res = new LinkedList<ProcessDataKeyHint>();
        ElementStreamUtils.applyAction(configuration.formLayout, (e) -> {
            if (e instanceof BaseInputElement<?> i && StringUtils.isNotNullOrEmpty(i.getDestinationKey())) {
                res.add(new ProcessDataKeyHint(
                        i.getDestinationKey(),
                        ProcessDataKeyHintType.ProcessData
                ));
            }
        });
        return res;
    }

    @Nonnull
    @Override
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        ConfigLayoutElement config;
        try {
            config = ElementPOJOMapper
                    .createFromPOJO(FormTriggerConfigV1.class);
        } catch (ElementDataConversionException e) {
            throw new RuntimeException(e);
        }

        config
                .findChild(FormTriggerConfigV1.FORM_SLUG, TextInputElement.class)
                .ifPresent(field -> {
                    var pattern = new TextInputElementPattern()
                            .setRegex("^[a-zA-Z0-9-]+$")
                            .setMessage("Der Webhook-Slug darf nur aus Buchstaben, Zahlen und Bindestrichen bestehen.");
                    field.setPattern(pattern);

                    field.setPrefix(goverConfig.createUrlWithTrailingSlash("…/forms/…"));
                });

        config
                .findChild(FormTriggerConfigV1.FORM_LAYOUT, UiDefinitionInputElement.class)
                .ifPresent(uid -> {
                    uid.setElementType(ElementType.FormLayout);
                    uid.setDisplayContext(ElementDisplayContext.CitizenFacing);
                });


        return config;
    }

    @Nullable
    @Override
    public GroupLayoutElement getTestingLayout(@Nonnull ProcessNodeDefinitionTestingLayoutContext<FormTriggerConfigV1> context) throws ResponseException {
        var link = goverConfig
                .createUrl(
                        "/forms/v1/",
                        context.processDefinition().getAccessKey().toString(),
                        context.configuration().formSlug
                ) + "?" + FormTriggerControllerV1.TEST_CLAIM_QUERY_PARAM + "=" + context.testClaim().getAccessKey();

        var layout = new GroupLayoutElement();
        layout.setId("testing");

        var rtx = new RichTextContentElement();
        rtx.setId("rtx");
        rtx.setContent(String.format("""
                Sie können das Formular abrufen unter [%s](%s).
                """, link, link));

        layout.addChild(rtx);

        return layout;
    }

    @Nullable
    @Override
    public Map<String, String> validateConfiguration(@Nonnull ProcessNodeEntity processNodeEntity,
                                                     @Nonnull FormTriggerConfigV1 configuration) throws ResponseException {
        var formSlug = configuration.formSlug;

        if (StringUtils.isNullOrEmpty(formSlug)) {
            return null;
        }

        var duplicateNodeFilter = ProcessNodeFilter
                .create()
                .setNotId(processNodeEntity.getId())
                .setProcessId(processNodeEntity.getProcessId())
                .setProcessVersion(processNodeEntity.getProcessVersion())
                .setProcessNodeDefinitionKey(processNodeEntity.getProcessNodeDefinitionKey())
                .addConfigEquals(FormTriggerConfigV1.FORM_SLUG, formSlug);

        if (!processNodeRepository.exists(duplicateNodeFilter.build())) {
            return null;
        }

        return Map.of(
                FormTriggerConfigV1.FORM_SLUG,
                "Die Formular-URL wird in dieser Prozessversion bereits von einem anderen Formulareingang verwendet."
        );
    }

    @Nonnull
    @Override
    public AuthoredElementValues cleanConfigurationForExport(@Nonnull AuthoredElementValues configuration) {
        var rawLayout = configuration.get(FormTriggerConfigV1.FORM_LAYOUT);
        var layout = ObjectMapperFactory
                .getInstance()
                .convertValue(rawLayout, FormLayoutElement.class);
        var cleanedLayout = FormLayoutCleanerService.clean(layout);
        configuration.put(FormTriggerConfigV1.FORM_LAYOUT, cleanedLayout);
        return configuration;
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<FormTriggerConfigV1> context) throws ProcessNodeExecutionException {
        var processInstanceInitialPayload = context
                .getThisProcessInstance()
                .getInitialPayload();

        var nodeInitialPayloadRaw = processInstanceInitialPayload
                .get(DATA_KEY_PAYLOAD);

        var nodeInitialPayload = new HashMap<String, Object>();
        if (nodeInitialPayloadRaw instanceof Map<?, ?> mInitialPayload) {
            for (var key : mInitialPayload.keySet()) {
                nodeInitialPayload.put(String.valueOf(key), mInitialPayload.get(key));
            }
        }

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_NAME)
                .setNodeData(processInstanceInitialPayload)
                .setProcessData(nodeInitialPayload);
    }

    @Nonnull
    @Override
    public Class<FormTriggerConfigV1> getNodeConfigurationClass() {
        return FormTriggerConfigV1.class;
    }
}
