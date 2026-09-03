package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.models.ProcessDataValueUtils;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import dev.fitko.fitconnect.api.config.ApplicationConfig;
import dev.fitko.fitconnect.api.config.EnvironmentName;
import dev.fitko.fitconnect.api.config.SenderConfig;
import dev.fitko.fitconnect.api.config.SubscriberConfig;
import dev.fitko.fitconnect.api.domain.model.event.EventState;
import dev.fitko.fitconnect.api.domain.model.event.Status;
import dev.fitko.fitconnect.api.domain.model.submission.SentSubmission;
import dev.fitko.fitconnect.api.domain.sender.SendableSubmission;
import dev.fitko.fitconnect.client.SenderClient;
import dev.fitko.fitconnect.client.bootstrap.ClientFactory;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Component
public class FitConnectSendJsonActionNodeV1 implements ProcessNodeDefinition<FitConnectSendJsonActionNodeV1.SendDataFitConnectActionNodeV1Config> {
    public static final String NODE_KEY = "fit_connect_send_json";
    private final SecretService secretService;
    private final JsonMapper jsonMapper;

    public FitConnectSendJsonActionNodeV1(SecretService secretService, JsonMapper jsonMapper) {
        this.secretService = secretService;
        this.jsonMapper = jsonMapper;
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return CorePlugin.PLUGIN_KEY;
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
        return "Datenübertragung an FIT-Connect";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Überträgt einen JSON-Datensatz an FIT-Connect.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "";
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
        };
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        "success",
                        "Erfolgreich",
                        "Der JSON-Datensatz wurde erfolgreich an FIT-Connect übertragen."
                )
        );
    }

    @Nonnull
    @Override
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        ConfigLayoutElement config;
        try {
            config = ElementPOJOMapper.createFromPOJO(SendDataFitConnectActionNodeV1Config.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError("Failed to create configuration layout for node: " + NODE_KEY, e);
        }
        return config;
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<SendDataFitConnectActionNodeV1Config> context) throws ProcessNodeExecutionException {
        final SendDataFitConnectActionNodeV1Config config = context.getConfigurationOfExecutingNode();

        final String destinationIdStr = config.destinationId;
        UUID destinationId;
        try {
            destinationId = UUID.fromString(destinationIdStr);
        } catch (IllegalArgumentException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Invalid destination ID: " + destinationIdStr, e);
        }

        var jsonData = ProcessDataValueUtils
                .resolveProcessDataValue(
                        context.getCurrentProcessExecutionData(),
                        config.jsonDatasetProcessKey
                );
        var jsonBody = jsonMapper.writeValueAsString(jsonData);

        var serviceSchema = URI.create(config.jsonSchemaLink);

        final SendableSubmission submission = SendableSubmission
                .Builder()
                .setDestination(destinationId)
                .setServiceType(config.serviceIdentifier, config.serviceName)
                .setJsonData(jsonBody, serviceSchema)
                .build();

        final ApplicationConfig applicationConfig = getApplicationConfig(config);

        final SenderClient senderClient = ClientFactory
                .createSenderClient(applicationConfig);

        final SentSubmission sentSubmission = senderClient.send(submission);

        final Status status = senderClient.getSubmissionStatus(sentSubmission);

        if (status.getState() != EventState.ACCEPTED && status.getState() != EventState.SUBMITTED) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Failed to send json data via FIT-Connect. Status: " + status);
        }

        return null;
    }

    private ApplicationConfig getApplicationConfig(SendDataFitConnectActionNodeV1Config config) throws ProcessNodeExecutionException {
        final EnvironmentName environmentName = new EnvironmentName("TEST");

        final UUID senderClientSecretKey;
        try {
            senderClientSecretKey = UUID.fromString(config.senderClientSecret);
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Failed to parse sender client secret key as UUID: " + config.senderClientSecret, e);
        }

        final var senderClientSecretEntity = secretService
                .retrieve(senderClientSecretKey)
                .orElseThrow(() -> new ProcessNodeExecutionExceptionInvalidConfiguration("Sender client secret not found: " + senderClientSecretKey));

        final String senderClientSecret;
        try {
            senderClientSecret = secretService.decrypt(senderClientSecretEntity);
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Failed to decrypt sender client secret: " + senderClientSecretKey, e);
        }

        final SenderConfig senderConfig = SenderConfig
                .builder()
                .clientId(config.senderClientId)
                .clientSecret(senderClientSecret)
                .build();

        final SubscriberConfig subscriberConfig = SubscriberConfig
                .builder()
                .build();

        return ApplicationConfig
                .builder()
                .activeEnvironment(environmentName)
                .senderConfig(senderConfig)
                .subscriberConfig(subscriberConfig)
                .build();
    }

    @Nonnull
    @Override
    public Class<SendDataFitConnectActionNodeV1Config> getNodeConfigurationClass() {
        return SendDataFitConnectActionNodeV1Config.class;
    }

    @LayoutElementPOJOBinding(id = FitConnectSendJsonActionNodeV1.NODE_KEY, type = ElementType.ConfigLayout)
    public static class SendDataFitConnectActionNodeV1Config {
        public static final String SERVICE_IDENTIFIER_FIELD_ID = "serviceIdentifier";
        @InputElementPOJOBinding(id = SERVICE_IDENTIFIER_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Service Identifier"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Service Identifier für die Nachrichtenübermittlung."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0),
        })
        public String serviceIdentifier;

        public static final String SERVICE_NAME_FIELD_ID = "serviceName";
        @InputElementPOJOBinding(id = SERVICE_NAME_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Service Name"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Service Name für die Nachrichtenübermittlung."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0),
        })
        public String serviceName;

        public static final String DESTINATION_ID_FIELD_ID = "destinationId";
        @InputElementPOJOBinding(id = DESTINATION_ID_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Zustellpunkt-ID"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Zustellpunkt-ID für die Nachrichtenübermittlung."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0),
        })
        public String destinationId;

        public static final String SENDER_CLIENT_ID_FIELD_ID = "senderClientId";
        @InputElementPOJOBinding(id = SENDER_CLIENT_ID_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Sender Client ID"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Client ID für den Sender."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0),
        })
        public String senderClientId;

        public static final String SENDER_CLIENT_SECRET_KEY_FIELD_ID = "senderClientSecret";
        @InputElementPOJOBinding(id = SENDER_CLIENT_SECRET_KEY_FIELD_ID, type = ElementType.SecretSelectInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Sender Client Secret"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Client Secret für den Sender."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0),
        })
        public String senderClientSecret;

        public static final String JSON_DATASET_PROCESS_KEY_FIELD_ID = "jsonDatasetProcessKey";
        @InputElementPOJOBinding(id = JSON_DATASET_PROCESS_KEY_FIELD_ID, type = ElementType.ProcessDataKeyInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "JSON-Datensatz Prozessdaten-Schlüssel"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Prozessdaten-Schlüssel, unter dem der JSON-Datensatz für die Nachrichtenübermittlung gespeichert ist."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0),
        })
        public String jsonDatasetProcessKey;

        public static final String JSON_SCHEMA_LINK_FIELD_ID = "jsonSchemaLink";
        @InputElementPOJOBinding(id = JSON_SCHEMA_LINK_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "JSON-Schema Link"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Link zum JSON-Schema, das den Datensatz validiert."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0),
        })
        public String jsonSchemaLink;
    }
}
