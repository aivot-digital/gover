package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.prosuna.backend.process.models.ProcessDataValueUtils;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import de.aivot.prosuna.backend.utils.StringUtils;
import dev.fitko.fitconnect.core.http.api.RestApiException;
import dev.fitko.fitconnect.rest.client.config.FitConnectEnvironment;
import dev.fitko.fitconnect.rest.model.event.EventState;
import dev.fitko.fitconnect.rest.model.submission.SentSubmission;
import dev.fitko.fitconnect.sdk.FitConnectSdk;
import dev.fitko.fitconnect.sdk.api.Addressing;
import dev.fitko.fitconnect.sdk.api.OutgoingSubmission;
import dev.fitko.fitconnect.sdk.api.Participant;
import dev.fitko.fitconnect.sdk.api.SubmissionData;
import dev.fitko.fitconnect.sdk.api.event.CaseEvent;
import dev.fitko.fitconnect.sdk.clients.OnlineService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class FitConnectSendJsonActionNodeV1 implements ProcessNodeDefinition<FitConnectSendJsonActionNodeV1.SendDataFitConnectActionNodeV1Config> {
    public static final String NODE_KEY = "fit_connect_send_json";
    private static final String PORT_SUCCESS = "success";
    private static final Set<String> SUPPORTED_ENVIRONMENTS = Set.of(
            SendDataFitConnectActionNodeV1Config.ENVIRONMENT_TEST,
            SendDataFitConnectActionNodeV1Config.ENVIRONMENT_STAGE,
            SendDataFitConnectActionNodeV1Config.ENVIRONMENT_PROD
    );

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
        return "Überträgt einen JSON-Datensatz aus den Vorgangsdaten über einen konfigurierten FIT-Connect-Onlinedienst an den ausgewählten Empfänger-Zustellpunkt.";
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
                        PORT_SUCCESS,
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

        config
                .findChild(SendDataFitConnectActionNodeV1Config.ENVIRONMENT_FIELD_ID, SelectInputElement.class)
                .ifPresent(field -> field.setOptions(List.of(
                        SelectInputElementOption.of(
                                SendDataFitConnectActionNodeV1Config.ENVIRONMENT_TEST,
                                SendDataFitConnectActionNodeV1Config.ENVIRONMENT_TEST
                        ),
                        SelectInputElementOption.of(
                                SendDataFitConnectActionNodeV1Config.ENVIRONMENT_STAGE,
                                SendDataFitConnectActionNodeV1Config.ENVIRONMENT_STAGE
                        ),
                        SelectInputElementOption.of(
                                SendDataFitConnectActionNodeV1Config.ENVIRONMENT_PROD,
                                SendDataFitConnectActionNodeV1Config.ENVIRONMENT_PROD
                        )
                )));
        return config;
    }

    @Nullable
    @Override
    public Map<String, List<String>> validateConfiguration(
            @Nonnull ProcessNodeEntity processNodeEntity,
            @Nonnull SendDataFitConnectActionNodeV1Config configuration
    ) {
        var errors = new LinkedHashMap<String, List<String>>();
        var environment = normalizeEnvironment(configuration.environment);
        if (environment == null || !SUPPORTED_ENVIRONMENTS.contains(environment)) {
            errors.put(
                    SendDataFitConnectActionNodeV1Config.ENVIRONMENT_FIELD_ID,
                    List.of("Wählen Sie eine unterstützte FIT-Connect-Umgebung aus.")
            );
        }
        validateUuid(
                configuration.destinationId,
                SendDataFitConnectActionNodeV1Config.DESTINATION_ID_FIELD_ID,
                "Die Empfänger-Zustellpunkt-ID muss eine gültige UUID sein.",
                errors
        );
        validateUuid(
                configuration.senderDestinationId,
                SendDataFitConnectActionNodeV1Config.SENDER_DESTINATION_ID_FIELD_ID,
                "Die Absender-Zustellpunkt-ID muss eine gültige UUID sein.",
                errors
        );
        return errors.isEmpty() ? null : errors;
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<SendDataFitConnectActionNodeV1Config> context) throws ProcessNodeExecutionException {
        final SendDataFitConnectActionNodeV1Config config = context.getConfigurationOfExecutingNode();
        final String environmentName = requireEnvironment(config.environment);
        final FitConnectEnvironment environment = resolveEnvironment(environmentName);
        final UUID destinationId = requireUuid(
                config.destinationId,
                "Empfänger-Zustellpunkt-ID"
        );
        final UUID senderDestinationId = requireUuid(
                config.senderDestinationId,
                "Absender-Zustellpunkt-ID"
        );

        var jsonData = ProcessDataValueUtils
                .resolveProcessDataValue(
                        context.getCurrentProcessExecutionData(),
                        config.jsonDatasetProcessKey
                );
        var jsonBody = jsonMapper.writeValueAsString(jsonData);

        final URI serviceSchema;
        try {
            serviceSchema = URI.create(config.jsonSchemaLink);
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Der JSON-Schema-Link „%s“ ist keine gültige URI.",
                    config.jsonSchemaLink
            );
        }

        final OutgoingSubmission submission = OutgoingSubmission
                .to(Participant.of(
                        destinationId,
                        Addressing.toService(config.serviceIdentifier, config.serviceName)
                ))
                .setData(SubmissionData.json(jsonBody, serviceSchema))
                .build();

        final String senderClientSecret = resolveSenderClientSecret(config);
        final OnlineService onlineService;
        try {
            onlineService = createOnlineService(
                    config.senderClientId,
                    senderClientSecret,
                    environment,
                    senderDestinationId
            );
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Der FIT-Connect-Onlinedienst „%s“ für die Umgebung „%s“ konnte nicht initialisiert werden: %s",
                    senderDestinationId,
                    environmentName,
                    resolveExceptionMessage(e)
            );
        }

        final SentSubmission sentSubmission;
        try {
            sentSubmission = onlineService.send(submission);
        } catch (Exception e) {
            if (hasNotFoundCause(e)) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        e,
                        "Der FIT-Connect-Zustellpunkt „%s“ wurde in der Umgebung „%s“ nicht gefunden.",
                        destinationId,
                        environmentName
                );
            }
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Die JSON-Daten konnten nicht an den FIT-Connect-Zustellpunkt „%s“ in der Umgebung „%s“ übertragen werden: %s",
                    destinationId,
                    environmentName,
                    resolveExceptionMessage(e)
            );
        }

        final CaseEvent status;
        try {
            status = onlineService.cases().logOf(sentSubmission).latest();
        } catch (Exception e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Der Status der FIT-Connect-Einreichung an den Zustellpunkt „%s“ in der Umgebung „%s“ konnte nicht abgerufen werden: %s",
                    destinationId,
                    environmentName,
                    resolveExceptionMessage(e)
            );
        }

        if (status.state() != EventState.ACCEPTED && status.state() != EventState.SUBMITTED) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    "FIT-Connect meldete für die Einreichung an den Zustellpunkt „%s“ in der Umgebung „%s“ den Status „%s“. Probleme: %s",
                    destinationId,
                    environmentName,
                    status.state(),
                    status.problems()
            );
        }

        return ProcessNodeExecutionResultTaskCompleted
                .of(PORT_SUCCESS)
                .setProcessData(context.getCurrentProcessExecutionData().getProcessData())
                .setNodeData(new LinkedHashMap<>());
    }

    @Nonnull
    OnlineService createOnlineService(@Nonnull String clientId,
                                      @Nonnull String clientSecret,
                                      @Nonnull FitConnectEnvironment environment,
                                      @Nonnull UUID senderDestinationId) {
        return FitConnectSdk
                .fromConfigBuilder()
                .credentials(clientId, clientSecret)
                .environment(environment)
                .build()
                .onlineService(senderDestinationId);
    }

    @Nonnull
    private String resolveSenderClientSecret(
            @Nonnull SendDataFitConnectActionNodeV1Config config
    ) throws ProcessNodeExecutionException {
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

        return senderClientSecret;
    }

    private static void validateUuid(@Nullable String rawValue,
                                     @Nonnull String fieldId,
                                     @Nonnull String errorMessage,
                                     @Nonnull Map<String, List<String>> errors) {
        try {
            UUID.fromString(StringUtils.toNullableTrimmedString(rawValue));
        } catch (Exception e) {
            errors.put(fieldId, List.of(errorMessage));
        }
    }

    @Nonnull
    private static UUID requireUuid(@Nullable String rawValue,
                                    @Nonnull String description) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var value = StringUtils.toNullableTrimmedString(rawValue);
        if (value == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die FIT-Connect-%s muss hinterlegt werden.",
                    description
            );
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die FIT-Connect-%s „%s“ ist keine gültige UUID.",
                    description,
                    value
            );
        }
    }

    @Nonnull
    private static FitConnectEnvironment resolveEnvironment(@Nonnull String environment) {
        return switch (environment) {
            case SendDataFitConnectActionNodeV1Config.ENVIRONMENT_TEST -> FitConnectEnvironment.TEST;
            case SendDataFitConnectActionNodeV1Config.ENVIRONMENT_STAGE -> FitConnectEnvironment.STAGE;
            case SendDataFitConnectActionNodeV1Config.ENVIRONMENT_PROD -> FitConnectEnvironment.PROD;
            default -> throw new IllegalArgumentException("Unsupported FIT-Connect environment: " + environment);
        };
    }

    @Nullable
    private static String normalizeEnvironment(@Nullable String rawEnvironment) {
        var environment = StringUtils.toNullableTrimmedString(rawEnvironment);
        return environment == null ? null : environment.toUpperCase(Locale.ROOT);
    }

    @Nonnull
    private static String requireEnvironment(@Nullable String rawEnvironment) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var environment = normalizeEnvironment(rawEnvironment);
        if (environment == null || !SUPPORTED_ENVIRONMENTS.contains(environment)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die FIT-Connect-Umgebung muss konfiguriert sein und einem der Werte TEST, STAGE oder PROD entsprechen."
            );
        }
        return environment;
    }

    private static boolean hasNotFoundCause(@Nonnull Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof RestApiException restApiException && restApiException.isNotFound()) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    @Nonnull
    private static String resolveExceptionMessage(@Nonnull Throwable throwable) {
        var message = StringUtils.toNullableTrimmedString(throwable.getMessage());
        return message == null ? "Unbekannter Fehler" : message;
    }

    @Nonnull
    @Override
    public Class<SendDataFitConnectActionNodeV1Config> getNodeConfigurationClass() {
        return SendDataFitConnectActionNodeV1Config.class;
    }

    /** Configuration required to transmit JSON process data through FIT-Connect. */
    @LayoutElementPOJOBinding(id = FitConnectSendJsonActionNodeV1.NODE_KEY, type = ElementType.ConfigLayout)
    public static class SendDataFitConnectActionNodeV1Config {
        public static final String ENVIRONMENT_FIELD_ID = "environment";
        public static final String ENVIRONMENT_TEST = "TEST";
        public static final String ENVIRONMENT_STAGE = "STAGE";
        public static final String ENVIRONMENT_PROD = "PROD";
        public static final String SERVICE_IDENTIFIER_FIELD_ID = "serviceIdentifier";
        public static final String SERVICE_NAME_FIELD_ID = "serviceName";
        public static final String DESTINATION_ID_FIELD_ID = "destinationId";
        public static final String SENDER_DESTINATION_ID_FIELD_ID = "senderDestinationId";
        public static final String SENDER_CLIENT_ID_FIELD_ID = "senderClientId";
        public static final String SENDER_CLIENT_SECRET_KEY_FIELD_ID = "senderClientSecret";
        public static final String JSON_DATASET_PROCESS_KEY_FIELD_ID = "jsonDatasetProcessKey";
        public static final String JSON_SCHEMA_LINK_FIELD_ID = "jsonSchemaLink";

        /** FIT-Connect environment used for all sender API calls. */
        @InputElementPOJOBinding(id = ENVIRONMENT_FIELD_ID, type = ElementType.Select, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "FIT-Connect-Umgebung"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Die Umgebung, an die der JSON-Datensatz übertragen wird."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0),
        })
        public String environment;

        /** Identifier of the service type assigned to the submission. */
        @InputElementPOJOBinding(id = SERVICE_IDENTIFIER_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Service Identifier"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Service Identifier für die Nachrichtenübermittlung."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0),
        })
        public String serviceIdentifier;

        /** Human-readable service name assigned to the submission. */
        @InputElementPOJOBinding(id = SERVICE_NAME_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Service Name"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Service Name für die Nachrichtenübermittlung."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0),
        })
        public String serviceName;

        /** UUID of the receiving organisation in the selected FIT-Connect environment. */
        @InputElementPOJOBinding(id = DESTINATION_ID_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Empfänger-Zustellpunkt-ID"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Zustellpunkt-ID der Organisation, die den Datensatz empfängt."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0),
        })
        public String destinationId;

        /** UUID of the type-C online service that sends the submission. */
        @InputElementPOJOBinding(id = SENDER_DESTINATION_ID_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Absender-Zustellpunkt-ID"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Zustellpunkt-ID des Onlinedienstes, der den Datensatz versendet."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0),
        })
        public String senderDestinationId;

        /** Client ID used to authenticate the sender against FIT-Connect. */
        @InputElementPOJOBinding(id = SENDER_CLIENT_ID_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Sender Client ID"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Client ID für den Sender."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0),
        })
        public String senderClientId;

        /** Secret reference whose decrypted value is the FIT-Connect sender client secret. */
        @InputElementPOJOBinding(id = SENDER_CLIENT_SECRET_KEY_FIELD_ID, type = ElementType.SecretSelectInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Sender Client Secret"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Client Secret für den Sender."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0),
        })
        public String senderClientSecret;

        /** Process-data key from which the JSON submission body is read. */
        @InputElementPOJOBinding(id = JSON_DATASET_PROCESS_KEY_FIELD_ID, type = ElementType.ProcessDataKeyInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "JSON-Datensatz Prozessdaten-Schlüssel"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Prozessdaten-Schlüssel, unter dem der JSON-Datensatz für die Nachrichtenübermittlung gespeichert ist."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0),
        })
        public String jsonDatasetProcessKey;

        /** URI of the JSON schema used by FIT-Connect to validate the submitted data. */
        @InputElementPOJOBinding(id = JSON_SCHEMA_LINK_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "JSON-Schema Link"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Link zum JSON-Schema, das den Datensatz validiert."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 12.0),
        })
        public String jsonSchemaLink;
    }
}
