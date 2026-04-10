package de.aivot.GoverBackend.plugins.core.v1.nodes.triggers.webhook;

import com.beust.jcommander.Strings;
import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.elements.models.EffectiveElementValues;
import de.aivot.GoverBackend.elements.models.elements.ElementOverrideFunctions;
import de.aivot.GoverBackend.elements.models.elements.ElementVisibilityFunctions;
import de.aivot.GoverBackend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.GoverBackend.elements.models.elements.form.input.TextInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.TextInputElementPattern;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.javascript.models.JavascriptCode;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.nocode.models.NoCodeExpression;
import de.aivot.GoverBackend.nocode.models.NoCodeReference;
import de.aivot.GoverBackend.nocode.models.NoCodeStaticValue;
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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Component
public class WebhookTriggerNodeV1 implements ProcessNodeDefinition {
    public static final String NODE_KEY = "webhook";
    private static final String PORT_NAME = "input";

    public static final String INITIAL_DATA_KEY_PAYLOAD = "payload";
    public static final String INITIAL_DATA_KEY_ATTACHMENTS = "attachments";
    public static final String INITIAL_DATA_KEY_REQUEST = "request";
    public static final String INITIAL_DATA_KEY_STARTED = "started";

    private final GoverConfig goverConfig;
    private final ProcessNodeRepository processDefinitionNodeRepository;

    @Autowired
    public WebhookTriggerNodeV1(GoverConfig goverConfig,
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
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        INITIAL_DATA_KEY_PAYLOAD,
                        "Eingangsdaten",
                        "Die Daten, die an den Auslöser übermittelt wurden"
                ),
                new ProcessNodeOutput(
                        INITIAL_DATA_KEY_ATTACHMENTS,
                        "List der Anlagen",
                        "Die Liste aller Anlagen, welche an den Auslöser übermittelt wurden"
                ),
                new ProcessNodeOutput(
                        INITIAL_DATA_KEY_REQUEST,
                        "Anfragedetails",
                        "Informationen über die HTTP-Anfrage an den Auslöser (HTTP-Methode, Headers, Query-Parameter)"
                ),
                new ProcessNodeOutput(
                        INITIAL_DATA_KEY_STARTED,
                        "Eingangszeitstempel",
                        "Der Zeitstempel des Dateneingangs an den Auslöser"
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
                    .createFromPOJO(WebhookTriggerConfigV1.class);
        } catch (ElementDataConversionException e) {
            throw new RuntimeException(e);
        }

        // Configure the slug input
        configLayout
                .findChild(WebhookTriggerConfigV1.SLUG_CONFIG_KEY, TextInputElement.class)
                .ifPresent(field -> {
                    var pattern = new TextInputElementPattern()
                            .setRegex("^[a-zA-Z0-9-]+$")
                            .setMessage("Der Webhook-Slug darf nur aus Buchstaben, Zahlen und Bindestrichen bestehen.");
                    field.setPattern(pattern);
                });

        // Automatic Link Element
        var linkPrefix = goverConfig.createUrlWithTrailingSlash("/api/public/webhooks/v1", context.processDefinition().getAccessKey().toString());

        var linkOverride = new ElementOverrideFunctions()
                .setJavascriptCode(JavascriptCode.of("""
                        (() => {
                            const slug = ctx.effectiveValues.%s || '{Webhook-URL}';
                            const requestMethod = ctx.effectiveValues.%s;
                            const requestBodyType = ctx.effectiveValues.%s;
                            const authRequired = ctx.effectiveValues.%s === true;
                            const authMethod = ctx.effectiveValues.%s;
                            const authToken = ctx.effectiveValues.%s ?? '';
                            const requestAllowsBody = requestMethod === '%s' || requestMethod === '%s' || requestMethod === '%s';
                            const requestBodySuffix = requestAllowsBody
                                ? requestBodyType === '%s'
                                    ? 'json/'
                                    : requestBodyType === '%s'
                                        ? 'form-data/'
                                        : 'xml/'
                                : '';
                            let link = '%s' + slug + '/' + requestBodySuffix;
                            if (authRequired && authMethod === '%s') {
                                link += '?%s=' + authToken;
                            }
                            return { ...element, content: `**Vollständige Webhook-URL:**\\n\\n${link}` };
                        })();
                        """,
                        WebhookTriggerConfigV1.SLUG_CONFIG_KEY,
                        WebhookTriggerConfigV1.REQUEST_METHOD_CONFIG_KEY,
                        WebhookTriggerConfigV1.REQUEST_BODY_TYPE_CONFIG_KEY,
                        WebhookTriggerConfigV1.AUTH_REQUIRED_CONFIG_KEY,
                        WebhookTriggerConfigV1.AUTH_METHOD_CONFIG_KEY,
                        WebhookTriggerConfigV1.AUTH_TOKEN_CONFIG_KEY,
                        WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST,
                        WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_PATCH,
                        WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_PUT,
                        WebhookTriggerConfigV1.REQUEST_BODY_TYPE_OPTION_JSON,
                        WebhookTriggerConfigV1.REQUEST_BODY_TYPE_OPTION_FORM,
                        linkPrefix,
                        WebhookTriggerConfigV1.AUTH_METHOD_OPTION_QUERY_PARAM,
                        WebhookTriggerControllerV1.AUTH_TOKEN_QUERY_PARAM
                ))
                .recalculateReferencedIds();

        var link = new RichTextContentElement();
        link.setId("link");
        link.setContent("**Vollständige Webhook-URL:**\n\n" + linkPrefix + "{Webhook-URL}/xml/");
        link.setOverride(linkOverride);
        configLayout.insertChildAfter(link, WebhookTriggerConfigV1.SLUG_CONFIG_KEY);

        // Add request method select options
        configLayout
                .findChild(WebhookTriggerConfigV1.REQUEST_METHOD_CONFIG_KEY, SelectInputElement.class)
                .ifPresent(field -> {
                    var options = List.of(
                            SelectInputElementOption.of(WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_GET, WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_GET),
                            SelectInputElementOption.of(WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST, WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST),
                            SelectInputElementOption.of(WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_PATCH, WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_PATCH),
                            SelectInputElementOption.of(WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_PUT, WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_PUT),
                            SelectInputElementOption.of(WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_DELETE, WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_DELETE)
                    );

                    field.setOptions(options);
                });

        // Add authentication method select options
        configLayout
                .findChild(WebhookTriggerConfigV1.AUTH_METHOD_CONFIG_KEY, SelectInputElement.class)
                .ifPresent(field -> {
                    var options = List.of(
                            SelectInputElementOption.of(WebhookTriggerConfigV1.AUTH_METHOD_OPTION_BASIC, "Basic Auth"),
                            // Bearer Token is removed because it clashes with the default spring boot jwt bearer token implementation
                            // SelectInputElementOption.of(WebhookTriggerConfigV1.AUTH_METHOD_OPTION_BEARER, "Bearer Token"),
                            SelectInputElementOption.of(WebhookTriggerConfigV1.AUTH_METHOD_OPTION_QUERY_PARAM, "Query-Parameter")
                    );

                    field.setOptions(options);
                });

        // Add request body type select options
        configLayout
                .findChild(WebhookTriggerConfigV1.REQUEST_BODY_TYPE_CONFIG_KEY, SelectInputElement.class)
                .ifPresent(field -> {
                    var options = List.of(
                            SelectInputElementOption.of(WebhookTriggerConfigV1.REQUEST_BODY_TYPE_OPTION_JSON, "JSON"),
                            SelectInputElementOption.of(WebhookTriggerConfigV1.REQUEST_BODY_TYPE_OPTION_FORM, "Multipart/Form-Data"),
                            SelectInputElementOption.of(WebhookTriggerConfigV1.REQUEST_BODY_TYPE_OPTION_XML, "XML")
                    );

                    field.setOptions(options);
                });

        // Add processing type select options
        configLayout
                .findChild(WebhookTriggerConfigV1.PROCESSING_TYPE_CONFIG_KEY, SelectInputElement.class)
                .ifPresent(field -> {
                    var options = List.of(
                            SelectInputElementOption.of(WebhookTriggerConfigV1.PROCESSING_TYPE_OPTION_AUTOMATIC, "Automatisch"),
                            SelectInputElementOption.of(WebhookTriggerConfigV1.PROCESSING_TYPE_OPTION_CODE, "Code")
                    );

                    field.setOptions(options);
                });

        // Set visibility function for request body config group
        configLayout
                .findChild(WebhookTriggerConfigV1.REQUEST_BODY_CONFIG_GROUP_ID, GroupLayoutElement.class)
                .ifPresent(group -> {
                    var visibilityFunc = ElementVisibilityFunctions
                            .of(new NoCodeExpression(
                                    NoCodeOrOperator.OPERATOR_ID,
                                    new NoCodeExpression(
                                            NoCodeEqualsOperator.OPERATOR_ID,
                                            new NoCodeReference(WebhookTriggerConfigV1.REQUEST_METHOD_CONFIG_KEY),
                                            new NoCodeStaticValue(WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_PATCH)
                                    ),
                                    new NoCodeExpression(
                                            NoCodeEqualsOperator.OPERATOR_ID,
                                            new NoCodeReference(WebhookTriggerConfigV1.REQUEST_METHOD_CONFIG_KEY),
                                            new NoCodeStaticValue(WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST)
                                    ),
                                    new NoCodeExpression(
                                            NoCodeEqualsOperator.OPERATOR_ID,
                                            new NoCodeReference(WebhookTriggerConfigV1.REQUEST_METHOD_CONFIG_KEY),
                                            new NoCodeStaticValue(WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_PUT)
                                    )
                            ))
                            .recalculateReferencedIds();
                    group.setVisibility(visibilityFunc);

                    group
                            .findChild(WebhookTriggerConfigV1.PROCESSING_CODE_CONFIG_KEY)
                            .ifPresent(codeEditor -> {
                                // Set visibility function for processing code editor
                                var visibilityFuncCode = ElementVisibilityFunctions
                                        .of(new NoCodeExpression(
                                                NoCodeEqualsOperator.OPERATOR_ID,
                                                new NoCodeReference(WebhookTriggerConfigV1.PROCESSING_TYPE_CONFIG_KEY),
                                                new NoCodeStaticValue(WebhookTriggerConfigV1.PROCESSING_TYPE_OPTION_CODE)
                                        ))
                                        .recalculateReferencedIds();
                                codeEditor.setVisibility(visibilityFuncCode);
                            });
                });

        // Set visibility function for auth config group
        configLayout
                .findChild(WebhookTriggerConfigV1.AUTH_CONFIG_GROUP_ID, GroupLayoutElement.class)
                .ifPresent(group -> {
                    var visibilityFunc = ElementVisibilityFunctions
                            .of(new NoCodeExpression(
                                    NoCodeEqualsOperator.OPERATOR_ID,
                                    new NoCodeReference(WebhookTriggerConfigV1.AUTH_REQUIRED_CONFIG_KEY),
                                    new NoCodeStaticValue(true)
                            ))
                            .recalculateReferencedIds();
                    group.setVisibility(visibilityFunc);

                    // Set visibility for basic auth fields
                    var basicAuthVisibility = ElementVisibilityFunctions
                            .of(new NoCodeExpression(
                                    NoCodeEqualsOperator.OPERATOR_ID,
                                    new NoCodeReference(WebhookTriggerConfigV1.AUTH_METHOD_CONFIG_KEY),
                                    new NoCodeStaticValue(WebhookTriggerConfigV1.AUTH_METHOD_OPTION_BASIC)
                            ))
                            .recalculateReferencedIds();
                    group
                            .findChild(WebhookTriggerConfigV1.AUTH_USERNAME_CONFIG_KEY)
                            .ifPresent(username -> {
                                username.setVisibility(basicAuthVisibility);
                            });
                    group
                            .findChild(WebhookTriggerConfigV1.AUTH_PASSWORD_CONFIG_KEY)
                            .ifPresent(password -> {
                                password.setVisibility(basicAuthVisibility);
                            });

                    group
                            .findChild(WebhookTriggerConfigV1.AUTH_TOKEN_CONFIG_KEY)
                            .ifPresent(token -> {
                                // Set visibility for token field (bearer or query param)
                                var keyAuthVisibility = ElementVisibilityFunctions
                                        .of(NoCodeExpression.of(
                                                NoCodeEqualsOperator.OPERATOR_ID,
                                                new NoCodeReference(WebhookTriggerConfigV1.AUTH_METHOD_CONFIG_KEY),
                                                new NoCodeStaticValue(WebhookTriggerConfigV1.AUTH_METHOD_OPTION_QUERY_PARAM)
                                        ))
                                        /* Bearer Token is removed because it clashes with the default spring boot jwt bearer token implementation
                                        .of(NoCodeExpression.of(
                                                NoCodeOrOperator.OPERATOR_ID,
                                                NoCodeExpression.of(
                                                        NoCodeEqualsOperator.OPERATOR_ID,
                                                        new NoCodeReference(WebhookTriggerConfigV1.AUTH_METHOD_CONFIG_KEY),
                                                        new NoCodeStaticValue(WebhookTriggerConfigV1.AUTH_METHOD_OPTION_BEARER)
                                                ),
                                                NoCodeExpression.of(
                                                        NoCodeEqualsOperator.OPERATOR_ID,
                                                        new NoCodeReference(WebhookTriggerConfigV1.AUTH_METHOD_CONFIG_KEY),
                                                        new NoCodeStaticValue(WebhookTriggerConfigV1.AUTH_METHOD_OPTION_QUERY_PARAM)
                                                )
                                        ))
                                         */
                                        .recalculateReferencedIds();
                                token.setVisibility(keyAuthVisibility);
                            });
                });

        return configLayout;
    }

    @Nullable
    @Override
    public GroupLayoutElement getTestingLayout(@Nonnull ProcessNodeDefinitionContextTesting context) throws ResponseException {
        WebhookTriggerConfigV1 config;
        try {
            config = getWebhookTriggerConfig(context.configuration().getEffectiveValues());
        } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
            throw ResponseException.internalServerError(
                    e,
                    "Beim Abrufen der Webhook-Trigger-Konfiguration ist ein Fehler aufgetreten. Bitte überprüfen Sie die Knotenkonfiguration."
            );
        }

        var layout = new GroupLayoutElement();
        layout.setId("layout");
        layout.setChildren(new LinkedList<>());

        var requestAllowsBody = requestMethodAllowsBody(config.requestMethod);

        var triggerUrl = appendQueryParameter(
                createWebhookUrl(
                        context.processDefinition().getAccessKey().toString(),
                        config.slug,
                        config.requestMethod,
                        config.requestBodyConfig != null ? config.requestBodyConfig.requestBodyType : null
                ),
                WebhookTriggerControllerV1.TEST_CLAIM_QUERY_PARAM,
                context.testClaim().getAccessKey()
        );

        if (usesQueryParamAuthentication(config.authRequired, config.authConfig)) {
            triggerUrl = appendQueryParameter(triggerUrl, WebhookTriggerControllerV1.AUTH_TOKEN_QUERY_PARAM, config.authConfig.authToken);
        }

        var requestContentType = requestAllowsBody ? (config.requestBodyConfig != null && WebhookTriggerConfigV1.REQUEST_BODY_TYPE_OPTION_JSON.equals(config.requestBodyConfig.requestBodyType) ?
                                                      "application/json" :
                                                      config.requestBodyConfig != null && WebhookTriggerConfigV1.REQUEST_BODY_TYPE_OPTION_FORM.equals(config.requestBodyConfig.requestBodyType) ?
                                                      "multipart/form-data" :
                                                      "application/xml") : null;

        var sb = new FormattedStringBuilder();

        sb.appendLine("Um den Webhook-Trigger zu testen, können Sie einen HTTP-Request an die folgende URL senden:");
        sb.appendLine("");
        sb.appendLine("<%s>", triggerUrl);
        sb.appendLine("");
        sb.appendLine(
                "Stellen Sie sicher, dass Sie die HTTP-Methode `%s` verwenden und " +
                        "fügen Sie den Test-Schlüssel `%s` als Query-Parameter " +
                        "`%s` hinzu, damit der Request authentifiziert ist.",
                config.requestMethod,
                context.testClaim().getAccessKey(),
                WebhookTriggerControllerV1.TEST_CLAIM_QUERY_PARAM
        );

        if (requestAllowsBody) {
            sb.appendLine("");
            sb.appendLine(
                    "Da die HTTP-Methode `%s` verwendet wird, können Sie zusätzlich " +
                            "einen Request-Body mit den Daten senden, die im Testprozess verwendet werden sollen. " +
                            "Wenn Sie keinen Request-Body senden, wird der Testprozess mit einem leeren Payload gestartet. " +
                            "Sie müssen die Daten als `%s` übertragen.",
                    config.requestMethod,
                    requestContentType
            );
        }

        if (Boolean.TRUE.equals(config.authRequired) && config.authConfig != null) {
            if (WebhookTriggerConfigV1.AUTH_METHOD_OPTION_QUERY_PARAM.equals(config.authConfig.authMethod)) {
                sb.appendLine("");
                sb.appendLine(
                        "Da in der Knotenkonfiguration die Authentifizierung über einen Query-Parameter aktiviert ist, " +
                                "müssen Sie zusätzlich den Authentifizierungs-Token als Query-Parameter " +
                                "`%s` hinzufügen.",
                        WebhookTriggerControllerV1.AUTH_TOKEN_QUERY_PARAM
                );
            } else if (WebhookTriggerConfigV1.AUTH_METHOD_OPTION_BASIC.equals(config.authConfig.authMethod)) {
                sb.appendLine("");
                sb.appendLine(
                        "Da in der Knotenkonfiguration die Authentifizierung über Basic Auth aktiviert ist, " +
                                "müssen Sie zusätzlich den Benutzernamen " +
                                "`%s` und das Passwort " +
                                "`%s` verwenden.",
                        config.authConfig.authUsername,
                        config.authConfig.authPassword
                );
            }
            /* Bearer Token is removed because it clashes with the default spring boot jwt bearer token implementation
            else if (WebhookTriggerConfigV1.AUTH_METHOD_OPTION_BEARER.equals(config.authConfig.authMethod)) {
                sb.appendLine("");
                sb.appendLine(
                        "Da in der Knotenkonfiguration die Authentifizierung über eine Bearer Token aktiviert ist, " +
                                "müssen Sie zusätzlich den Authentifizierungs-Token `%s` im " +
                                "Authorization-Header mit dem Präfix `Bearer` hinzufügen.",
                        config.authConfig.authToken
                );
            }
            */
        }

        var curlLines = new LinkedList<String>();
        curlLines.add("--request " + config.requestMethod);
        curlLines.add("--url '" + triggerUrl + "'");

        if (requestAllowsBody) {
            curlLines.add("--header 'Content-Type: " + requestContentType + "'");

            if (WebhookTriggerConfigV1.REQUEST_BODY_TYPE_OPTION_FORM.equals(config.requestBodyConfig.requestBodyType)) {
                curlLines.add("--form 'key=value'");
                curlLines.add("--form 'file=@/path/to/file'");
            } else if (WebhookTriggerConfigV1.REQUEST_BODY_TYPE_OPTION_JSON.equals(config.requestBodyConfig.requestBodyType)) {
                curlLines.add("--data '{\"key\": \"value\"}'");
            } else if (WebhookTriggerConfigV1.REQUEST_BODY_TYPE_OPTION_XML.equals(config.requestBodyConfig.requestBodyType)) {
                curlLines.add("--data '<root><key>value</key></root>'");
            }
        }

        if (Boolean.TRUE.equals(config.authRequired) && config.authConfig != null) {
            if (WebhookTriggerConfigV1.AUTH_METHOD_OPTION_BASIC.equals(config.authConfig.authMethod)) {
                curlLines.add("--header 'Authorization: Basic " + StringUtils.encodeBase64String(config.authConfig.authUsername + ":" + config.authConfig.authPassword) + "'");
            }
            /* Bearer Token is removed because it clashes with the default spring boot jwt bearer token implementation
            else if (WebhookTriggerConfigV1.AUTH_METHOD_OPTION_BEARER.equals(config.authConfig.authMethod)) {
                curlLines.add("--header 'Authorization: Bearer " + config.authConfig.authToken + "'");
            }
            */
        }

        sb.appendLine("");
        sb.appendLine("Sie können den folgenden Shell-Befehl verwenden, um den Webhook-Trigger mit einem Test-Request zu testen:");
        sb.appendLine("");
        sb.appendLine("```sh");
        sb.appendLine("curl \\");
        sb.append("%s", Strings.join(" \\\n", curlLines.stream().map(l -> "  " + l).toList()));
        sb.appendLine("");
        sb.append("```");

        var rtx = new RichTextContentElement();
        rtx.setId("text");
        rtx.setContent(sb.toString());
        layout.addChild(rtx);

        return layout;
    }

    @Override
    public Map<String, String> validateConfiguration(@Nonnull ProcessNodeEntity processNodeEntity,
                                                     @Nonnull AuthoredElementValues configuration,
                                                     @Nonnull DerivedRuntimeElementData derivedRuntimeElementData) throws ResponseException {
        var res = new HashMap<String, String>();

        var slug = StringUtils.toNullableTrimmedString(configuration.get(WebhookTriggerConfigV1.SLUG_CONFIG_KEY));
        var slugState = derivedRuntimeElementData.getElementStates().get(WebhookTriggerConfigV1.SLUG_CONFIG_KEY);

        var spec = SpecificationBuilder
                .create(ProcessNodeEntity.class)
                .withNotEquals("processId", processNodeEntity.getProcessId())
                .withJsonEquals("configuration", List.of(WebhookTriggerConfigV1.SLUG_CONFIG_KEY), slug)
                .build();
        var otherExists = processDefinitionNodeRepository
                .exists(spec);

        if (otherExists) {
            var errorMessage = String.format("Der Webhook-Slug %s wird bereits von einem anderen Webhook verwendet. Bitte wählen Sie einen eindeutigen Slug.", slug);
            slugState.setError(errorMessage);
            res.put(WebhookTriggerConfigV1.SLUG_CONFIG_KEY, errorMessage);
        }

        return res.isEmpty() ? null : res;
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionContextInit context) throws ProcessNodeExecutionException {
        var config = getWebhookTriggerConfig(context.getConfiguration().getEffectiveValues());

        // Get the initial payload from the process instance.
        // This payload is set by the WebhookTriggerController when the webhook is called and contains the data received via the webhook.
        var initialPayload = context
                .getThisProcessInstance()
                .getInitialPayload();

        // Determine the result of this init execution
        var result = new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_NAME)
                .setNodeData(initialPayload);

        // Copy the initial payload to the process data if configured
        if (config.requestBodyConfig != null && Boolean.TRUE.equals(config.requestBodyConfig.copyToProcessData)) {
            Object payloadObj = initialPayload.get(INITIAL_DATA_KEY_PAYLOAD);

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

        return result;
    }

    private boolean requestMethodAllowsBody(@Nullable String requestMethod) {
        return WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_POST.equals(requestMethod) ||
                WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_PATCH.equals(requestMethod) ||
                WebhookTriggerConfigV1.REQUEST_METHOD_OPTION_PUT.equals(requestMethod);
    }

    @Nullable
    private String resolveRequestBodyPathSegment(@Nullable String requestMethod,
                                                 @Nullable String requestBodyType) {
        if (!requestMethodAllowsBody(requestMethod)) {
            return null;
        }

        if (WebhookTriggerConfigV1.REQUEST_BODY_TYPE_OPTION_JSON.equals(requestBodyType)) {
            return "json";
        }

        if (WebhookTriggerConfigV1.REQUEST_BODY_TYPE_OPTION_FORM.equals(requestBodyType)) {
            return "form-data";
        }

        return "xml";
    }

    @Nonnull
    private String createWebhookUrl(@Nonnull String processAccessKey,
                                    @Nullable String slug,
                                    @Nullable String requestMethod,
                                    @Nullable String requestBodyType) {
        return goverConfig.createUrlWithTrailingSlash(
                "/api/public/webhooks/v1",
                processAccessKey,
                slug,
                resolveRequestBodyPathSegment(requestMethod, requestBodyType)
        );
    }

    private boolean usesQueryParamAuthentication(@Nullable Boolean authRequired,
                                                 @Nullable WebhookTriggerConfigV1.WebhookConfigAuth authConfig) {
        return Boolean.TRUE.equals(authRequired) &&
                authConfig != null &&
                WebhookTriggerConfigV1.AUTH_METHOD_OPTION_QUERY_PARAM.equals(authConfig.authMethod);
    }

    @Nonnull
    private String appendQueryParameter(@Nonnull String url,
                                        @Nonnull String parameterName,
                                        @Nullable Object parameterValue) {
        return url +
                (url.contains("?") ? "&" : "?") +
                parameterName +
                "=" +
                (parameterValue == null ? "" : parameterValue);
    }

    @Nonnull
    private static WebhookTriggerConfigV1 getWebhookTriggerConfig(@Nonnull EffectiveElementValues configuration) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        WebhookTriggerConfigV1 config;
        try {
            config = ElementPOJOMapper
                    .mapToPOJO(configuration, WebhookTriggerConfigV1.class);
        } catch (ElementDataConversionException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die Konfiguration des Webhook-Trigger-Knotens ist ungültig."
            );
        }
        return config;
    }
}
