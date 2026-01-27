package de.aivot.GoverBackend.plugins.core.v1.nodes.triggers.webhook;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.ElementDataObject;
import de.aivot.GoverBackend.elements.models.elements.ElementVisibilityFunctions;
import de.aivot.GoverBackend.elements.models.elements.form.input.RadioInputElementOption;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.nocode.models.NoCodeExpression;
import de.aivot.GoverBackend.nocode.models.NoCodeReference;
import de.aivot.GoverBackend.nocode.models.NoCodeStaticValue;
import de.aivot.GoverBackend.plugin.models.PluginComponent;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.plugins.core.v1.operators.bool.NoCodeOrOperator;
import de.aivot.GoverBackend.plugins.core.v1.operators.common.NoCodeEqualsOperator;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.process.repositories.ProcessNodeRepository;
import de.aivot.GoverBackend.utils.StringUtils;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WebhookTriggerNode implements ProcessNodeDefinition, PluginComponent {
    public static final String NODE_KEY = "webhook";
    private static final String PORT_NAME = "input";

    private final ProcessNodeRepository processDefinitionNodeRepository;

    @Autowired
    public WebhookTriggerNode(ProcessNodeRepository processDefinitionNodeRepository) {
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

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionContextConfig context) {
        ConfigLayoutElement configLayout;
        try {
            configLayout = ElementPOJOMapper
                    .createFromPOJO(WebhookTriggerConfig.class);
        } catch (ElementDataConversionException e) {
            throw new RuntimeException(e);
        }

        // Add request method select options
        configLayout
                .findChild(WebhookTriggerConfig.REQUEST_METHOD_CONFIG_KEY, SelectInputElement.class)
                .ifPresent(field -> {
                    var options = List.of(
                            RadioInputElementOption.of(WebhookTriggerConfig.REQUEST_METHOD_OPTION_GET, WebhookTriggerConfig.REQUEST_METHOD_OPTION_GET),
                            RadioInputElementOption.of(WebhookTriggerConfig.REQUEST_METHOD_OPTION_POST, WebhookTriggerConfig.REQUEST_METHOD_OPTION_POST),
                            RadioInputElementOption.of(WebhookTriggerConfig.REQUEST_METHOD_OPTION_PATCH, WebhookTriggerConfig.REQUEST_METHOD_OPTION_PATCH),
                            RadioInputElementOption.of(WebhookTriggerConfig.REQUEST_METHOD_OPTION_PUT, WebhookTriggerConfig.REQUEST_METHOD_OPTION_PUT),
                            RadioInputElementOption.of(WebhookTriggerConfig.REQUEST_METHOD_OPTION_DELETE, WebhookTriggerConfig.REQUEST_METHOD_OPTION_DELETE)
                    );

                    field.setOptions(options);
                });

        // Add authentication method select options
        configLayout
                .findChild(WebhookTriggerConfig.AUTH_METHOD_CONFIG_KEY, SelectInputElement.class)
                .ifPresent(field -> {
                    var options = List.of(
                            RadioInputElementOption.of(WebhookTriggerConfig.AUTH_METHOD_OPTION_BASIC, "Basic Auth"),
                            RadioInputElementOption.of(WebhookTriggerConfig.AUTH_METHOD_OPTION_BEARER, "Bearer Token"),
                            RadioInputElementOption.of(WebhookTriggerConfig.AUTH_METHOD_OPTION_QUERY_PARAM, "Query-Parameter")
                    );

                    field.setOptions(options);
                });

        // Add request body type select options
        configLayout
                .findChild(WebhookTriggerConfig.REQUEST_BODY_TYPE_CONFIG_KEY, SelectInputElement.class)
                .ifPresent(field -> {
                    var options = List.of(
                            RadioInputElementOption.of(WebhookTriggerConfig.REQUEST_BODY_TYPE_OPTION_JSON, "JSON"),
                            RadioInputElementOption.of(WebhookTriggerConfig.REQUEST_BODY_TYPE_OPTION_FORM, "Formulardaten"),
                            RadioInputElementOption.of(WebhookTriggerConfig.REQUEST_BODY_TYPE_OPTION_XML, "XML")
                    );

                    field.setOptions(options);
                });

        // Add processing type select options
        configLayout
                .findChild(WebhookTriggerConfig.PROCESSING_TYPE_CONFIG_KEY, SelectInputElement.class)
                .ifPresent(field -> {
                    var options = List.of(
                            RadioInputElementOption.of(WebhookTriggerConfig.PROCESSING_TYPE_OPTION_AUTOMATIC, "Automatisch"),
                            RadioInputElementOption.of(WebhookTriggerConfig.PROCESSING_TYPE_OPTION_CODE, "Code")
                    );

                    field.setOptions(options);
                });

        // Set visibility function for request body config group
        configLayout
                .findChild(WebhookTriggerConfig.REQUEST_BODY_CONFIG_GROUP_ID, GroupLayoutElement.class)
                .ifPresent(group -> {
                    var visibilityFunc = ElementVisibilityFunctions
                            .of(new NoCodeExpression(
                                    NoCodeOrOperator.OPERATOR_ID,
                                    new NoCodeExpression(
                                            NoCodeEqualsOperator.OPERATOR_ID,
                                            new NoCodeReference(WebhookTriggerConfig.REQUEST_METHOD_CONFIG_KEY),
                                            new NoCodeStaticValue(WebhookTriggerConfig.REQUEST_METHOD_OPTION_PATCH)
                                    ),
                                    new NoCodeExpression(
                                            NoCodeEqualsOperator.OPERATOR_ID,
                                            new NoCodeReference(WebhookTriggerConfig.REQUEST_METHOD_CONFIG_KEY),
                                            new NoCodeStaticValue(WebhookTriggerConfig.REQUEST_METHOD_OPTION_POST)
                                    ),
                                    new NoCodeExpression(
                                            NoCodeEqualsOperator.OPERATOR_ID,
                                            new NoCodeReference(WebhookTriggerConfig.REQUEST_METHOD_CONFIG_KEY),
                                            new NoCodeStaticValue(WebhookTriggerConfig.REQUEST_METHOD_OPTION_PUT)
                                    )
                            ))
                            .recalculateReferencedIds();
                    group.setVisibility(visibilityFunc);

                    group
                            .findChild(WebhookTriggerConfig.PROCESSING_CODE_CONFIG_KEY)
                            .ifPresent(codeEditor -> {
                                // Set visibility function for processing code editor
                                var visibilityFuncCode = ElementVisibilityFunctions
                                        .of(new NoCodeExpression(
                                                NoCodeEqualsOperator.OPERATOR_ID,
                                                new NoCodeReference(WebhookTriggerConfig.PROCESSING_TYPE_CONFIG_KEY),
                                                new NoCodeStaticValue(WebhookTriggerConfig.PROCESSING_TYPE_OPTION_CODE)
                                        ))
                                        .recalculateReferencedIds();
                                codeEditor.setVisibility(visibilityFuncCode);
                            });
                });

        // Set visibility function for auth config group
        configLayout
                .findChild(WebhookTriggerConfig.AUTH_CONFIG_GROUP_ID, GroupLayoutElement.class)
                .ifPresent(group -> {
                    var visibilityFunc = ElementVisibilityFunctions
                            .of(new NoCodeExpression(
                                    NoCodeEqualsOperator.OPERATOR_ID,
                                    new NoCodeReference(WebhookTriggerConfig.AUTH_REQUIRED_CONFIG_KEY),
                                    new NoCodeStaticValue(true)
                            ))
                            .recalculateReferencedIds();
                    group.setVisibility(visibilityFunc);

                    // Set visibility for basic auth fields
                    var basicAuthVisibility = ElementVisibilityFunctions
                            .of(new NoCodeExpression(
                                    NoCodeEqualsOperator.OPERATOR_ID,
                                    new NoCodeReference(WebhookTriggerConfig.AUTH_METHOD_CONFIG_KEY),
                                    new NoCodeStaticValue(WebhookTriggerConfig.AUTH_METHOD_OPTION_BASIC)
                            ))
                            .recalculateReferencedIds();
                    group
                            .findChild(WebhookTriggerConfig.AUTH_USERNAME_CONFIG_KEY)
                            .ifPresent(username -> {
                                username.setVisibility(basicAuthVisibility);
                            });
                    group
                            .findChild(WebhookTriggerConfig.AUTH_PASSWORD_CONFIG_KEY)
                            .ifPresent(password -> {
                                password.setVisibility(basicAuthVisibility);
                            });

                    group
                            .findChild(WebhookTriggerConfig.AUTH_TOKEN_CONFIG_KEY)
                            .ifPresent(token -> {
                                // Set visibility for token field (bearer or query param)
                                var keyAuthVisibility = ElementVisibilityFunctions
                                        .of(NoCodeExpression.of(
                                                NoCodeOrOperator.OPERATOR_ID,
                                                NoCodeExpression.of(
                                                        NoCodeEqualsOperator.OPERATOR_ID,
                                                        new NoCodeReference(WebhookTriggerConfig.AUTH_METHOD_CONFIG_KEY),
                                                        new NoCodeStaticValue(WebhookTriggerConfig.AUTH_METHOD_OPTION_BEARER)
                                                ),
                                                NoCodeExpression.of(
                                                        NoCodeEqualsOperator.OPERATOR_ID,
                                                        new NoCodeReference(WebhookTriggerConfig.AUTH_METHOD_CONFIG_KEY),
                                                        new NoCodeStaticValue(WebhookTriggerConfig.AUTH_METHOD_OPTION_QUERY_PARAM)
                                                )
                                        ))
                                        .recalculateReferencedIds();
                                token.setVisibility(keyAuthVisibility);
                            });
                });

        return configLayout;
    }

    @Override
    public void validateConfiguration(@Nonnull ProcessNodeEntity processNodeEntity,
                                      @Nonnull ElementData configuration) throws ResponseException {

        var slugEdo = configuration
                .getOrDefault(
                        WebhookTriggerConfig.SLUG_CONFIG_KEY,
                        new ElementDataObject(ElementType.Text)
                );

        var slug = slugEdo
                .getOptionalValue(String.class)
                .orElseThrow(() -> {
                    slugEdo.addComputedError("Der Webhook-Slug ist ein Pflichtfeld und darf nicht leer sein.");
                    return ResponseException
                            .badRequest(configuration);
                });

        var spec = SpecificationBuilder
                .create(ProcessNodeEntity.class)
                .withNotEquals("processId", processNodeEntity.getProcessId())
                .withJsonEquals("configuration", List.of(WebhookTriggerConfig.SLUG_CONFIG_KEY), slug)
                .build();
        var otherExists = processDefinitionNodeRepository
                .exists(spec);

        if (otherExists) {
            slugEdo.setComputedError(
                    "Der Webhook-Slug %s wird bereits von einem anderen Webhook verwendet. Bitte wählen Sie einen eindeutigen Slug.",
                    StringUtils.quote(slug)
            );
            throw ResponseException.badRequest(configuration);
        }
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionContextInit context) throws ProcessNodeExecutionException {
        WebhookTriggerConfig config;
        try {
            config = ElementPOJOMapper
                    .mapToPOJO(context.getThisNode().getConfiguration(), WebhookTriggerConfig.class);
        } catch (ElementDataConversionException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die Konfiguration des Webhook-Trigger-Knotens ist ungültig."
            );
        }

        // Determine the result of this init execution
        var result = new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_NAME)
                .setNodeData(context.getThisProcessInstance().getInitialPayload());

        // Copy the initial payload to the process data if configured
        if (config.requestBodyConfig.copyToProcessData) {
            result.setProcessData(context.getThisProcessInstance().getInitialPayload());
        }

        return result;
    }
}
