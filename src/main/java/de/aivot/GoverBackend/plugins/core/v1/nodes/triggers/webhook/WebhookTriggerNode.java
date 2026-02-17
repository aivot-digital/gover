package de.aivot.GoverBackend.plugins.core.v1.nodes.triggers.webhook;

import com.beust.jcommander.Strings;
import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.ElementDataObject;
import de.aivot.GoverBackend.elements.models.elements.ElementVisibilityFunctions;
import de.aivot.GoverBackend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.RadioInputElementOption;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.GoverConfig;
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
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.process.repositories.ProcessNodeRepository;
import de.aivot.GoverBackend.utils.FormattedStringBuilder;
import de.aivot.GoverBackend.utils.StringUtils;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Component
public class WebhookTriggerNode implements ProcessNodeDefinition {
    public static final String NODE_KEY = "webhook";
    private static final String PORT_NAME = "input";

    public static final String DATA_KEY_PAYLOAD = "payload";
    public static final String DATA_KEY_ATTACHMENTS = "attachments";
    public static final String DATA_KEY_REQUEST = "request";

    private final GoverConfig goverConfig;
    private final ProcessNodeRepository processDefinitionNodeRepository;

    @Autowired
    public WebhookTriggerNode(GoverConfig goverConfig,
                              ProcessNodeRepository processDefinitionNodeRepository) {
        this.goverConfig = goverConfig;
        this.processDefinitionNodeRepository = processDefinitionNodeRepository;
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return Core.PLUGIN_KEY;
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
                            RadioInputElementOption.of(WebhookTriggerConfig.REQUEST_BODY_TYPE_OPTION_FORM, "Multipart/Form-Data"),
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

    @Nullable
    @Override
    public GroupLayoutElement getTestingLayout(@Nonnull ProcessNodeDefinitionContextTesting context) throws ResponseException {
        WebhookTriggerConfig config;
        try {
            config = getWebhookTriggerConfig(context.thisNode().getConfiguration());
        } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
            throw ResponseException.internalServerError(
                    e,
                    "Beim Abrufen der Webhook-Trigger-Konfiguration ist ein Fehler aufgetreten. Bitte überprüfen Sie die Knotenkonfiguration."
            );
        }

        var layout = new GroupLayoutElement();
        layout.setId("layout");
        layout.setChildren(new LinkedList<>());

        var triggerUrl = String.format(
                "%s?%s=%s",
                goverConfig.createUrlWithTrailingSlash("/api/public/webhooks", config.slug),
                WebhookTriggerController.TEST_CLAIM_QUERY_PARAM,
                context.testClaim().getAccessKey()
        );

        if (config.authRequired && WebhookTriggerConfig.AUTH_METHOD_OPTION_QUERY_PARAM.equals(config.authConfig.authMethod)) {
            triggerUrl += String.format("&%s=%s", WebhookTriggerController.AUTH_TOKEN_QUERY_PARAM, config.authConfig.authToken);
        }

        var requestAllowsBody = WebhookTriggerConfig.REQUEST_METHOD_OPTION_POST.equals(config.requestMethod) ||
                WebhookTriggerConfig.REQUEST_METHOD_OPTION_PATCH.equals(config.requestMethod) ||
                WebhookTriggerConfig.REQUEST_METHOD_OPTION_PUT.equals(config.requestMethod);

        var requestContentType = requestAllowsBody ? (WebhookTriggerConfig.REQUEST_BODY_TYPE_OPTION_JSON.equals(config.requestBodyConfig.requestBodyType) ?
                "application/json" :
                WebhookTriggerConfig.REQUEST_BODY_TYPE_OPTION_FORM.equals(config.requestBodyConfig.requestBodyType) ?
                        "multipart/form-data" :
                        "application/xml") : null;

        var sb = new FormattedStringBuilder();

        sb.append(
                "<p>Um den Webhook-Trigger zu testen, können Sie einen HTTP-Request an die folgende URL senden:</p>" +
                        "<p><a href=\"%s\">%s</a></p>",
                triggerUrl,
                triggerUrl
        );

        sb.append(
                "<p>Stellen Sie sicher, dass Sie die HTTP-Methode <span class=\"inline-code\">%s</span> verwenden und " +
                        "fügen Sie den Test-Schlüssel <span class=\"inline-code\">%s</span> als Query-Parameter " +
                        "<span class=\"inline-code\">%s</span> hinzu, damit der Request authentifiziert ist.</p>",
                config.requestMethod,
                context.testClaim().getAccessKey(),
                WebhookTriggerController.TEST_CLAIM_QUERY_PARAM
        );

        if (requestAllowsBody) {
            sb.append(
                    "<p>Da die HTTP-Methode <span class=\"inline-code\">%s</span> verwendet wird, können Sie zusätzlich " +
                            "einen Request-Body mit den Daten senden, die im Testprozess verwendet werden sollen. " +
                            "Wenn Sie keinen Request-Body senden, wird der Testprozess mit einem leeren Payload gestartet. " +
                            "Sie müssen die Daten als <span class=\"inline-code\">%s</span> übertragen.</p>",
                    config.requestMethod,
                    requestContentType
            );
        }

        if (config.authRequired) {
            if (WebhookTriggerConfig.AUTH_METHOD_OPTION_QUERY_PARAM.equals(config.authConfig.authMethod)) {
                sb.append(
                        "<p>Da in der Knotenkonfiguration die Authentifizierung über einen Query-Parameter aktiviert ist, " +
                                "müssen Sie zusätzlich den Authentifizierungs-Token als Query-Parameter " +
                                "<span class=\"inline-code\">%s</span> hinzufügen.</p>",
                        WebhookTriggerController.AUTH_TOKEN_QUERY_PARAM
                );
            } else if (WebhookTriggerConfig.AUTH_METHOD_OPTION_BASIC.equals(config.authConfig.authMethod)) {
                sb.append(
                        "<p>Da in der Knotenkonfiguration die Authentifizierung über Basic Auth aktiviert ist, " +
                                "müssen Sie zusätzlich den Benutzernamen " +
                                "<span class=\"inline-code\">%s</span> und das Passwort " +
                                "<span class=\"inline-code\">%s</span> verwenden.",
                        config.authConfig.authUsername,
                        config.authConfig.authPassword
                );
            } else if (WebhookTriggerConfig.AUTH_METHOD_OPTION_BEARER.equals(config.authConfig.authMethod)) {
                sb.append(
                        "<p>Da in der Knotenkonfiguration die Authentifizierung über eine Bearer Token aktiviert ist, " +
                                "müssen Sie zusätzlich den Authentifizierungs-Token <span class=\"inline-code\">%s</span> im " +
                                "Authorization-Header mit dem Präfix <span class=\"inline-code\">Bearer</span> hinzufügen.</p>",
                        config.authConfig.authToken
                );
            }
        }

        var curlLines = new LinkedList<String>();
        curlLines.add("--request " + config.requestMethod);
        curlLines.add("--url '" + triggerUrl + "'");

        if (requestAllowsBody) {
            curlLines.add("--header 'Content-Type: " + requestContentType + "'");

            if (WebhookTriggerConfig.REQUEST_BODY_TYPE_OPTION_FORM.equals(config.requestBodyConfig.requestBodyType)) {
                curlLines.add("--form 'key=value'");
                curlLines.add("--form 'file=@/path/to/file'");
            } else if (WebhookTriggerConfig.REQUEST_BODY_TYPE_OPTION_JSON.equals(config.requestBodyConfig.requestBodyType)) {
                curlLines.add("--data '{\"key\": \"value\"}'");
            } else if (WebhookTriggerConfig.REQUEST_BODY_TYPE_OPTION_XML.equals(config.requestBodyConfig.requestBodyType)) {
                curlLines.add("--data '&lt;root&gt;&lt;key&gt;value&lt;/key&gt;&lt;/root&gt;'");
            }
        }

        if (config.authRequired) {
            if (WebhookTriggerConfig.AUTH_METHOD_OPTION_BASIC.equals(config.authConfig.authMethod)) {
                curlLines.add("--header 'Authorization: Basic " + StringUtils.encodeBase64String(config.authConfig.authUsername + ":" + config.authConfig.authPassword) + "'");
            } else if (WebhookTriggerConfig.AUTH_METHOD_OPTION_BEARER.equals(config.authConfig.authMethod)) {
                curlLines.add("--header 'Authorization: Bearer " + config.authConfig.authToken + "'");
            }
        }

        sb.append("<p>Sie können den folgenden Shell-Befehl verwenden, um den Webhook-Trigger mit einem Test-Request zu testen:</p>");
        sb.append("<pre class=\"code-block\">curl \\\n");
        sb.append(Strings.join(" \\\n", curlLines.stream().map(l -> "  " + l).toList()));
        sb.append("</pre>");

        var rtx = new RichTextContentElement();
        rtx.setId("text");
        rtx.setContent(sb.toString());
        layout.addChild(rtx);

        return layout;
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
        var config = getWebhookTriggerConfig(context.getThisNode().getConfiguration());

        // Determine the result of this init execution
        var result = new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_NAME)
                .setNodeData(context.getThisProcessInstance().getInitialPayload());

        // Copy the initial payload to the process data if configured
        if (Boolean.TRUE.equals(config.requestBodyConfig.copyToProcessData)) {
            Object payloadObj = context.getThisProcessInstance().getInitialPayload().get(DATA_KEY_PAYLOAD);

            if (payloadObj == null) {
                // If there is no payload, we can just set an empty map as process data
                result.setProcessData(Map.of());
            } else {
                Map<String, Object> payload;
                try {
                    payload = (Map<String, Object>) payloadObj;
                } catch (ClassCastException e) {
                    throw new ProcessNodeExecutionExceptionUnknown(
                            e,
                            "Die Daten, die über den Webhook empfangen wurden, haben nicht das erwartete Format und können daher nicht verarbeitet werden. Erwartet wurde eon Objekt, aber es wurde %s empfangen.",
                            StringUtils.quote(payloadObj.getClass().getName())
                    );
                }
                result.setProcessData(payload);
            }
        } else {
            result.setProcessData(Map.of());
        }

        result.setNodeData(context.getThisProcessInstance().getInitialPayload());

        return result;
    }

    @Nonnull
    private static WebhookTriggerConfig getWebhookTriggerConfig(@Nonnull ElementData configuration) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        WebhookTriggerConfig config;
        try {
            config = ElementPOJOMapper
                    .mapToPOJO(configuration, WebhookTriggerConfig.class);
        } catch (ElementDataConversionException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die Konfiguration des Webhook-Trigger-Knotens ist ungültig."
            );
        }
        return config;
    }
}
