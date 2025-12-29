package de.aivot.GoverBackend.plugins.core.v1.nodes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.models.ElementDataObject;
import de.aivot.GoverBackend.elements.models.elements.ElementValueFunctions;
import de.aivot.GoverBackend.elements.models.elements.form.input.TextInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.plugin.models.PluginComponent;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.process.entities.ProcessEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessVersionEntity;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinition;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionResult;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.GoverBackend.process.models.ProcessNodePort;
import de.aivot.GoverBackend.process.repositories.ProcessNodeRepository;
import de.aivot.GoverBackend.process.services.ProcessInstanceService;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.utils.StringUtils;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
public class WebhookTriggerNode implements ProcessNodeDefinition, PluginComponent {
    private static final String NODE_KEY = "webhook";
    private static final String PORT_NAME = "input";

    private final static String SLUG_CONFIG_KEY = "slug";

    private final GoverConfig goverConfig;
    private final ProcessInstanceService processInstanceService;
    private final ProcessNodeRepository processDefinitionNodeRepository;

    @Autowired
    public WebhookTriggerNode(GoverConfig goverConfig,
                              ProcessInstanceService processInstanceService,
                              ProcessNodeRepository processDefinitionNodeRepository) {
        this.goverConfig = goverConfig;
        this.processInstanceService = processInstanceService;
        this.processDefinitionNodeRepository = processDefinitionNodeRepository;
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return Core.PLUGIN_KEY;
    }

    @Override
    public @Nonnull String getKey() {
        return NODE_KEY;
    }

    @Nonnull
    @Override
    public Integer getVersion() {
        return 1;
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

        var slugField = new TextInputElement();
        slugField.setId(SLUG_CONFIG_KEY);
        slugField.setLabel("Webhook-Slug");
        slugField.setHint("Der eindeutige Slug, über den der Webhook angesprochen wird.");
        slugField.setRequired(true);
        layout.addChild(slugField);

        var computeLink = new ElementValueFunctions();
        var jsCode = JavascriptCode.of(
                "const url = `%s${ctx.%s.inputValue}/`\nurl;",
                goverConfig.createUrl("/api/public/webhooks/"),
                SLUG_CONFIG_KEY
        );
        computeLink.setJavascriptCode(jsCode);
        computeLink.setReferencedIds(List.of(SLUG_CONFIG_KEY));

        var linkField = new TextInputElement();
        linkField.setId("webhook-link");
        linkField.setLabel("Webhook-URL");
        linkField.setHint("Die URL, über die der Webhook angesprochen werden kann.");
        linkField.setDisabled(true);
        linkField.setValue(computeLink);
        layout.addChild(linkField);

        return layout;
    }

    @Nonnull
    @Override
    public ProcessNodeType getType() {
        return ProcessNodeType.Trigger;
    }

    @Nonnull
    @Override
    public String getName() {
        return "Webhook";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Löst den Prozess aus, wenn Daten über einen Webhook empfangen werden.";
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME,
                        "Dateneingang",
                        "Es wurden Daten über den Webhook empfangen."
                )
        );
    }

    @Override
    public void validateConfiguration(@Nonnull ProcessNodeEntity entity) throws ResponseException {
        var configuration = entity
                .getConfiguration();

        var slugEDO = configuration
                .getOrDefault(SLUG_CONFIG_KEY, null);

        if (slugEDO == null) {
            slugEDO = new ElementDataObject(ElementType.Text);
            configuration.put(SLUG_CONFIG_KEY, slugEDO);
        }

        var slugObj = slugEDO
                .getValue();

        if (slugObj instanceof String slug && StringUtils.isNotNullOrEmpty(slug)) {
            var spec = SpecificationBuilder
                    .create(ProcessNodeEntity.class)
                    .withNotEquals("id", entity.getId())
                    .withJsonEquals("configuration", List.of(SLUG_CONFIG_KEY), slug)
                    .build();
            var otherExists = processDefinitionNodeRepository
                    .exists(spec);
            if (otherExists) {
                slugEDO.setComputedError("Der Webhook-Slug '" + slug + "' wird bereits von einem anderen Webhook verwendet. Bitte wählen Sie einen eindeutigen Slug.");
                throw ResponseException.badRequest(configuration);
            }
        } else {
            slugEDO.setComputedError("Der Webhook-Slug ist ein Pflichtfeld und darf nicht leer sein.");
            throw ResponseException.badRequest(configuration);
        }
    }

    @PostMapping("/api/public/webhooks/{slug}/")
    public void handleWebhook(
            @Nonnull @PathVariable String slug,
            @Nonnull @RequestBody Map<String, Object> payload
    ) throws ResponseException {
        var spec = SpecificationBuilder
                .create(ProcessNodeEntity.class)
                .withEquals("processNodeDefinitionKey", NODE_KEY)
                .withJsonEquals("configuration", List.of(SLUG_CONFIG_KEY, "inputValue"), slug)
                .build();

        var entities = processDefinitionNodeRepository
                .findAll(spec);

        for (var entity : entities) {
            var instance = new ProcessInstanceEntity(
                    null,
                    null,
                    entity.getProcessId(),
                    entity.getProcessVersion(),
                    ProcessInstanceStatus.Created,
                    null,
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    null,
                    null,
                    payload,
                    entity.getId(),
                    null
            );

            processInstanceService.create(instance);
        }
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessInstanceEntity processInstance,
                                           @Nonnull ProcessNodeEntity thisNode,
                                           @Nonnull Map<String, Object> workingData) throws Exception {
        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_NAME)
                .setProcessData(processInstance.getInitialPayload());
    }
}
