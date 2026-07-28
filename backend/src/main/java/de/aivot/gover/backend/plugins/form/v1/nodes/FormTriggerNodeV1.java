package de.aivot.gover.backend.plugins.form.v1.nodes;

import de.aivot.gover.backend.core.services.ObjectMapperFactory;
import de.aivot.gover.backend.elements.enums.ElementDisplayContext;
import de.aivot.gover.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.gover.backend.elements.models.AuthoredElementValues;
import de.aivot.gover.backend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.gover.backend.elements.models.elements.form.input.FileUploadInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.TextInputElementPattern;
import de.aivot.gover.backend.elements.models.elements.form.input.UiDefinitionInputElement;
import de.aivot.gover.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.gover.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.gover.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.gover.backend.elements.utils.ElementPOJOMapper;
import de.aivot.gover.backend.elements.utils.ElementStreamUtils;
import de.aivot.gover.backend.enums.ElementType;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.plugin.models.PluginComponent;
import de.aivot.gover.backend.plugins.form.FormPlugin;
import de.aivot.gover.backend.plugins.form.v1.services.FormLayoutCleanerService;
import de.aivot.gover.backend.process.entities.ProcessEntity;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.enums.ProcessNodeType;
import de.aivot.gover.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.gover.backend.process.filters.ProcessNodeFilter;
import de.aivot.gover.backend.process.models.ProcessNodeDefinition;
import de.aivot.gover.backend.process.models.ProcessNodeDefinitionMetadata;
import de.aivot.gover.backend.process.models.ProcessNodeOutput;
import de.aivot.gover.backend.process.models.ProcessNodePort;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeDefinitionTestingLayoutContext;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.gover.backend.process.repositories.ProcessNodeRepository;
import de.aivot.gover.backend.process.services.PublicUrlService;
import de.aivot.gover.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class FormTriggerNodeV1 implements ProcessNodeDefinition<FormTriggerConfigV1>, PluginComponent {
    public static final String NODE_KEY = "form";
    private static final String PORT_NAME = "input";
    private static final String COPY_VALUE_TEMPLATE_PATH_SEGMENT = "__copy_value__";

    public static final String DATA_KEY_PAYLOAD = "payload";
    public static final String DATA_KEY_UNMAPPED = "unmapped";
    public static final String DATA_KEY_ATTACHMENTS = "attachments";
    public static final String DATA_KEY_STARTED = "started";

    private final PublicUrlService publicUrlService;
    private final ProcessNodeRepository processNodeRepository;

    public FormTriggerNodeV1(PublicUrlService publicUrlService,
                             ProcessNodeRepository processNodeRepository) {
        this.publicUrlService = publicUrlService;
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
                        "Formular eingereicht",
                        "Der Prozess wird mit den eingereichten Formulardaten gestartet."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        DATA_KEY_PAYLOAD,
                        "Zugeordnete Formulardaten",
                        "Enthält alle Formulardaten welche über einen Datenschlüssel zugeordnet wurden."
                ),
                new ProcessNodeOutput(
                        DATA_KEY_UNMAPPED,
                        "Formular-Rohdaten",
                        "Enthält alle Formulardaten unter der jeweiligen Element-ID des Feldes, unabhängig davon, ob ein Element über einen Datenschlüssel zugewiesen wurde oder nicht."
                ),
                new ProcessNodeOutput(
                        DATA_KEY_ATTACHMENTS,
                        "Anlagen",
                        "Eine Liste aller Anlagen, die über dieses Formular hochgeladen wurden."
                ),
                new ProcessNodeOutput(
                        DATA_KEY_STARTED,
                        "Eingangszeitstempel",
                        "Der Zeitstempel des Dateneingangs an den Auslöser"
                )
        );
    }

    @Nonnull
    @Override
    public ProcessNodeDefinitionMetadata getMetadata(@Nonnull ProcessNodeEntity processNodeEntity,
                                                     @Nonnull FormTriggerConfigV1 configuration,
                                                     @Nonnull ProcessNodeDefinitionMetadata previousMetadata) {
        var pdm = ProcessNodeDefinitionMetadata
                .reuse(previousMetadata)
                .withLayout(configuration.formLayout, processNodeEntity);

        if (configuration.identities != null) {
            for (var identity : configuration.identities) {
                if (identity.getId() == null || identity.getTitle() == null) {
                    continue;
                }

                pdm.addForwardedIdentity(
                        identity.getId(),
                        identity.getTitle(),
                        identity.getDescription(),
                        processNodeEntity
                );
            }
        }

        return pdm;
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
                            .setRegex("^[a-z0-9-]+$")
                            .setMessage("Das URL-Segment des Formulars darf nur aus Kleinbuchstaben, Zahlen und Bindestrichen bestehen.");
                    field.setPattern(pattern);

                    field.setPrefix(publicUrlService.createProcessNamespaceDisplayPrefix());
                    field.setCopyable(true);
                    field.setCopyValueTemplate(createPublicFormCopyValueTemplate(context.processDefinition()));
                });

        config
                .findChild(FormTriggerConfigV1.FORM_LAYOUT, UiDefinitionInputElement.class)
                .ifPresent(uid -> {
                    uid.setElementType(ElementType.FormLayout);
                    uid.setDisplayContext(ElementDisplayContext.CitizenFacing);
                });


        return config;
    }

    @Nonnull
    private String createPublicFormCopyValueTemplate(@Nonnull ProcessEntity process) {
        return publicUrlService
                .createPublicFormUrl(process, COPY_VALUE_TEMPLATE_PATH_SEGMENT)
                .replace(COPY_VALUE_TEMPLATE_PATH_SEGMENT, TextInputElement.COPY_VALUE_TEMPLATE_PLACEHOLDER);
    }

    @Nullable
    @Override
    public GroupLayoutElement getTestingLayout(@Nonnull ProcessNodeDefinitionTestingLayoutContext<FormTriggerConfigV1> context) throws ResponseException {
        var link = publicUrlService.createPublicFormUrl(
                context.processDefinition(),
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
    public Map<String, List<String>> validateConfiguration(@Nonnull ProcessNodeEntity processNodeEntity,
                                                           @Nonnull FormTriggerConfigV1 configuration) throws ResponseException {
        var errors = new LinkedHashMap<String, List<String>>();
        var formSlug = configuration.formSlug;

        if (StringUtils.isNotNullOrEmpty(formSlug)) {
            if (!formSlug.matches("^[a-z0-9-]+$")) {
                addValidationError(
                        errors,
                        FormTriggerConfigV1.FORM_SLUG,
                        "Das URL-Segment des Formulars darf nur aus Kleinbuchstaben, Zahlen und Bindestrichen bestehen."
                );
            }

            var duplicateNodeFilter = ProcessNodeFilter
                    .create()
                    .setNotId(processNodeEntity.getId())
                    .setProcessId(processNodeEntity.getProcessId())
                    .setProcessVersion(processNodeEntity.getProcessVersion())
                    .setProcessNodeDefinitionKey(processNodeEntity.getProcessNodeDefinitionKey())
                    .addConfigEquals(FormTriggerConfigV1.FORM_SLUG, formSlug);

            if (processNodeRepository.exists(duplicateNodeFilter.build())) {
                addValidationError(
                        errors,
                        FormTriggerConfigV1.FORM_SLUG,
                        "Das URL-Segment des Formulars wird in dieser Prozessversion bereits von einem anderen Formulareingang verwendet."
                );
            }
        }

        var layoutErrors = validateLegacyPublishChecklistFields(configuration.formLayout);
        if (!layoutErrors.isEmpty()) {
            errors.put(FormTriggerConfigV1.FORM_LAYOUT, layoutErrors);
        }

        return errors.isEmpty() ? null : errors;
    }

    private static void addValidationError(@Nonnull Map<String, List<String>> errors,
                                           @Nonnull String fieldId,
                                           @Nonnull String message) {
        errors
                .computeIfAbsent(fieldId, ignored -> new LinkedList<>())
                .add(message);
    }

    @Nonnull
    private List<String> validateLegacyPublishChecklistFields(@Nullable FormLayoutElement formLayout) {
        if (formLayout == null) {
            return List.of();
        }

        var errors = new LinkedList<String>();

        if (StringUtils.isNullOrEmpty(formLayout.getPublicTitle())) {
            errors.add("Der öffentliche Titel muss hinterlegt sein.");
        }
        if (formLayout.getLegalSupportDepartmentId() == null) {
            errors.add("Der fachliche Support muss eingerichtet sein.");
        }
        if (formLayout.getTechnicalSupportDepartmentId() == null) {
            errors.add("Der technische Support muss eingerichtet sein.");
        }
        if (formLayout.getImprintDepartmentId() == null) {
            errors.add("Das Impressum muss eingerichtet sein.");
        }
        if (formLayout.getPrivacyDepartmentId() == null) {
            errors.add("Die Datenschutzerklärung muss eingerichtet sein.");
        }
        if (formLayout.getAccessibilityDepartmentId() == null) {
            errors.add("Die Barrierefreiheitserklärung muss eingerichtet sein.");
        }
        ElementStreamUtils.applyAction(formLayout, element -> {
            if (element instanceof FileUploadInputElement uploadElement && StringUtils.isNullOrEmpty(uploadElement.getSubmittedFileName())) {
                errors.add("Für das Upload-Feld %s muss ein Dateiname bei Einreichung hinterlegt sein.".formatted(StringUtils.quote(describeUploadElement(uploadElement))));
            }
        });

        return errors;
    }

    @Nonnull
    private String describeUploadElement(@Nonnull FileUploadInputElement uploadElement) {
        var label = StringUtils.toNullableTrimmedString(uploadElement.getLabel());
        return label == null ? Objects.toString(uploadElement.getId(), "Unbenanntes Upload-Feld") : label;
    }

    @Nonnull
    @Override
    public AuthoredElementValues cleanConfigurationForExport(@Nonnull AuthoredElementValues configuration) {
        // Clean the form layout because it has references to system specific resources like department ids.
        var rawLayout = configuration.get(FormTriggerConfigV1.FORM_LAYOUT);
        var layout = ObjectMapperFactory
                .getInstance()
                .convertValue(rawLayout, FormLayoutElement.class);
        var cleanedLayout = FormLayoutCleanerService.clean(layout);
        configuration.put(FormTriggerConfigV1.FORM_LAYOUT, cleanedLayout);

        // Clean the identities for they are not the same on every system.
        configuration.remove(FormTriggerConfigV1.IDENTITIES);

        // Return the cleaned config.
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
