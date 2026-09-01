package de.aivot.prosuna.backend.plugins.core.v1.communication;

import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.communication.models.CommunicationProviderContext;
import de.aivot.prosuna.backend.communication.models.CommunicationProviderDefinition;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.enums.StoragePathSelectorMode;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.elements.form.input.*;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import de.aivot.prosuna.backend.identity.enums.IdentityProviderType;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import de.aivot.prosuna.backend.storage.services.StorageService;
import dev.fitko.fitconnect.api.config.ApplicationConfig;
import dev.fitko.fitconnect.api.config.EnvironmentName;
import dev.fitko.fitconnect.api.config.SenderConfig;
import dev.fitko.fitconnect.api.config.SubscriberConfig;
import dev.fitko.fitconnect.api.domain.model.event.EventState;
import dev.fitko.fitconnect.api.domain.model.event.Status;
import dev.fitko.fitconnect.api.domain.model.submission.SentSubmission;
import dev.fitko.fitconnect.api.domain.sender.SendableSubmission;
import dev.fitko.fitconnect.api.domain.zbp.AuthorKeyPair;
import dev.fitko.fitconnect.api.domain.zbp.message.AuthenticationLevel;
import dev.fitko.fitconnect.api.domain.zbp.message.CreateMessage;
import dev.fitko.fitconnect.client.SenderClient;
import dev.fitko.fitconnect.client.bootstrap.ClientFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reserved v1 definition. It intentionally supports no identity-provider type so it cannot be
 * enabled in a binding before transport and addressing have been implemented.
 */
@Component
public class FitConnectCommunicationProviderV1 implements CommunicationProviderDefinition<FitConnectCommunicationProviderV1.Config, FitConnectCommunicationProviderV1.IdentityBinding> {
    public static final String COMPONENT_KEY = "fit_connect_communication_provider";
    private static final String MessageSendingIdentifier = "urn:schema-fitko-de:fit-connect:id.bund.de:message_v6"; // constant

    private final StorageService storageService;
    private final SecretService secretService;

    public FitConnectCommunicationProviderV1(StorageService storageService, SecretService secretService) {
        this.storageService = storageService;
        this.secretService = secretService;
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return CorePlugin.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public String getComponentKey() {
        return COMPONENT_KEY;
    }

    @Nonnull
    @Override
    public String getComponentVersion() {
        return "1.0.0";
    }

    @Nonnull
    @Override
    public String getName() {
        return "FIT-Connect-Kommunikation";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Vorbereitete, noch nicht aktivierbare Integration für FIT-Connect.";
    }


    @Nonnull
    @Override
    public Class<Config> getConfigClass() {
        return Config.class;
    }

    @Nonnull
    @Override
    public ConfigLayoutElement getConfigLayout() throws ResponseException {
        ConfigLayoutElement config;
        try {
            config = ElementPOJOMapper.createFromPOJO(Config.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError("Failed to create config layout for FIT-Connect communication provider.", e);
        }

        config
                .findChild(Config.ZBP_CERTIFICATE_PRIVATE_KEY_PATH_FIELD_ID, StoragePathSelectorInputElement.class)
                .ifPresent(element -> {
                    element.setMode(StoragePathSelectorMode.File);
                });

        config
                .findChild(Config.ZBP_CERTIFICATE_CLIENT_CERT_PATH_FIELD_ID, StoragePathSelectorInputElement.class)
                .ifPresent(element -> {
                    element.setMode(StoragePathSelectorMode.File);
                });

        var secretOptions = secretService
                .list()
                .stream()
                .map(secret -> SelectInputElementOption.of(
                        secret.getKey().toString(),
                        secret.getName() == null || secret.getName().isBlank()
                                ? secret.getKey().toString()
                                : secret.getName()
                ))
                .toList();

        config
                .findChild(Config.SENDER_CLIENT_SECRET_KEY_FIELD_ID, SelectInputElement.class)
                .ifPresent(element -> {
                    element.setOptions(secretOptions);
                });

        return config;
    }

    @Nonnull
    @Override
    public List<IdentityProviderType> getSupportedIdentityProviderTypes() {
        return List.of(
                IdentityProviderType.BundId,
                IdentityProviderType.BayernId,
                IdentityProviderType.ShId,
                IdentityProviderType.Custom
        );
    }

    @Nonnull
    @Override
    public Class<IdentityBinding> getIdentityProviderBindingConfigClass() {
        return IdentityBinding.class;
    }

    @Nonnull
    @Override
    public ConfigLayoutElement getIdentityProviderBindingConfigLayout(@Nonnull IdentityProviderEntity identityProviderEntity) throws ResponseException {
        var attributeOptions = identityProviderEntity.getAttributes() == null
                ? List.<SelectInputElementOption>of()
                : identityProviderEntity.getAttributes()
                .stream()
                .filter(attribute -> attribute.getKeyInData() != null && !attribute.getKeyInData().isBlank())
                .map(attribute -> SelectInputElementOption.of(
                        attribute.getKeyInData(),
                        attribute.getLabel() == null || attribute.getLabel().isBlank()
                                ? attribute.getKeyInData()
                                : attribute.getLabel()
                ))
                .toList();

        ConfigLayoutElement config;
        try {
            config = ElementPOJOMapper.createFromPOJO(IdentityBinding.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError("Failed to create identity provider binding config layout for FIT-Connect communication provider.", e);
        }

        config
                .findChild(IdentityBinding.BPK2_ATTRIBUTE_FIELD_ID, SelectInputElement.class)
                .ifPresent(element -> {
                    element.setOptions(attributeOptions);
                });

        config
                .findChild(IdentityBinding.STORK_QAA_LEVEL_FIELD_ID, SelectInputElement.class)
                .ifPresent(element -> {
                    element.setOptions(attributeOptions);
                });

        return config;
    }

    @Nullable
    @Override
    public GroupLayoutElement getTestingLayout() throws ResponseException {
        var group = new GroupLayoutElement();

        var postfachId = new TextInputElement();
        postfachId.setId("postfachId");
        postfachId.setLabel("Postfach-ID");
        postfachId.setHint("Postfach-ID des Testnutzers, an den die Testnachricht gesendet wird.");
        postfachId.setRequired(true);
        group.addChild(postfachId);

        return group;
    }

    @Override
    public Map<String, Object> sendMessage(@Nonnull CommunicationProviderContext<Config, IdentityBinding> context,
                                           @Nonnull IdentityData identity,
                                           @Nonnull CommunicationMessage message) throws CommunicationException {
        final Config config = context.communicationProviderConfiguration();
        final UUID destinationId = getDestinationId(config);
        final AuthorKeyPair authorKeyPair = getAuthorKeyPair(context.communicationProviderConfiguration());
        final UUID postfachId = getPostfachId(context, identity);

        final CreateMessage zbpMessage = CreateMessage
                .builder()
                .content(message.body())
                .sender("FIT-Connect")
                .service("FIT-Connect Test")
                .title(message.subject())
                .retrievalConfirmationAddress("retrieval@mail.net")
                .replyAddress("reply@mail.net")
                .mailboxUuid(postfachId)
                .stork_qaa_level(AuthenticationLevel.ONE)
                .build();

        final SendableSubmission submission = SendableSubmission
                .Builder()
                .setDestination(destinationId)
                .setServiceType(MessageSendingIdentifier, "ZBP Message Forwarding")
                .setZBPMessage(zbpMessage, authorKeyPair)
                .build();

        final ApplicationConfig applicationConfig = getApplicationConfig(config);

        final SenderClient senderClient = ClientFactory
                .createSenderClient(applicationConfig);

        final SentSubmission sentSubmission = senderClient.send(submission);

        final Status status = senderClient.getSubmissionStatus(sentSubmission);

        if (status.getState() != EventState.ACCEPTED && status.getState() != EventState.SUBMITTED) {
            throw new CommunicationException("Failed to send message via FIT-Connect. Status: " + status);
        }

        return Map.of(
                "postfachId", postfachId.toString(),
                "submissionId", sentSubmission.getSubmissionId().toString(),
                "status", status.getState().name()
        );
    }

    @Nonnull
    private static UUID getPostfachId(@Nonnull CommunicationProviderContext<Config, IdentityBinding> context, @Nonnull IdentityData identity) {
        var postfachIdAttribute = context
                .identityProviderBindingConfiguration()
                .bpk2Attribute;
        var postfachIdString = identity
                .attributes()
                .get(postfachIdAttribute);
        UUID postfachId;
        try {
            postfachId = UUID.fromString(postfachIdString);
        } catch (Exception e) {
            throw new CommunicationException("Failed to parse BPK2 attribute value as UUID: " + postfachIdString, e);
        }
        return postfachId;
    }

    @Nonnull
    private static UUID getDestinationId(Config config) {
        UUID destinationId;
        try {
            destinationId = UUID.fromString(config.destinationId);
        } catch (Exception e) {
            throw new CommunicationException("Failed to parse destination ID as UUID: " + config.destinationId, e);
        }
        return destinationId;
    }

    private ApplicationConfig getApplicationConfig(Config config) {
        final EnvironmentName environmentName = new EnvironmentName("TEST");

        final UUID senderClientSecretKey;
        try {
            senderClientSecretKey = UUID.fromString(config.senderClientSecret);
        } catch (Exception e) {
            throw new CommunicationException("Failed to parse sender client secret key as UUID: " + config.senderClientSecret, e);
        }

        final var senderClientSecretEntity = secretService
                .retrieve(senderClientSecretKey)
                .orElseThrow(() -> new CommunicationException("Sender client secret not found: " + senderClientSecretKey));

        final String senderClientSecret;
        try {
            senderClientSecret = secretService.decrypt(senderClientSecretEntity);
        } catch (Exception e) {
            throw new CommunicationException("Failed to decrypt sender client secret: " + senderClientSecretKey, e);
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

    private AuthorKeyPair getAuthorKeyPair(Config config) {
        String privateKeyPem;
        try {
            privateKeyPem = resolveFile(config.zbpCertificatePrivateKeyPath);
        } catch (ResponseException | IOException e) {
            throw new CommunicationException("Failed to resolve private key for FIT-Connect communication provider.", e);
        }

        String clientCertPem;
        try {
            clientCertPem = resolveFile(config.zbpCertificateClientCertPath);
        } catch (ResponseException | IOException e) {
            throw new CommunicationException("Failed to resolve client certificate for FIT-Connect communication provider.", e);
        }

        return AuthorKeyPair
                .builder()
                .authorPrivateKeyAsPem(privateKeyPem)
                .authorCertificateAsPem(clientCertPem)
                .build();
    }

    private String resolveFile(StoragePathSelectorInputElementValue p) throws ResponseException, IOException {
        if (p == null || p.getStorageProviderId() == null || p.getPath() == null) {
            throw new IOException("Die Datei ist nicht konfiguriert.");
        }

        var is = storageService
                .getDocumentContent(p.getStorageProviderId(), p.getPath());

        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    @LayoutElementPOJOBinding(id = "fit-connect-provider-config", type = ElementType.ConfigLayout)
    public static class Config {
        public static final String ZBP_CERTIFICATE_PRIVATE_KEY_PATH_FIELD_ID = "zbpCertificatePrivateKeyPath";
        @InputElementPOJOBinding(id = ZBP_CERTIFICATE_PRIVATE_KEY_PATH_FIELD_ID, type = ElementType.StoragePathSelector, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Privater Schlüssel"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Pfad zum privaten Schlüssel des FIT-Connect-Zertifikats."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "allowReadOnlyStorageProviders", boolValue = true),

        })
        public StoragePathSelectorInputElementValue zbpCertificatePrivateKeyPath;

        public static final String ZBP_CERTIFICATE_CLIENT_CERT_PATH_FIELD_ID = "zbpCertificateClientCertPath";
        @InputElementPOJOBinding(id = ZBP_CERTIFICATE_CLIENT_CERT_PATH_FIELD_ID, type = ElementType.StoragePathSelector, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Client-Zertifikat"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Pfad zum Client-Zertifikat des FIT-Connect-Zertifikats."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "allowReadOnlyStorageProviders", boolValue = true),
        })
        public StoragePathSelectorInputElementValue zbpCertificateClientCertPath;

        public static final String DESTINATION_ID_FIELD_ID = "destinationId";
        @InputElementPOJOBinding(id = DESTINATION_ID_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Zustellpunkt-ID"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Zustellpunkt-ID für die Nachrichtenübermittlung."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
        })
        public String destinationId;

        public static final String SENDER_CLIENT_ID_FIELD_ID = "senderClientId";
        @InputElementPOJOBinding(id = SENDER_CLIENT_ID_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Sender Client ID"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Client ID für den Sender."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
        })
        public String senderClientId;

        public static final String SENDER_CLIENT_SECRET_KEY_FIELD_ID = "senderClientSecret";
        @InputElementPOJOBinding(id = SENDER_CLIENT_SECRET_KEY_FIELD_ID, type = ElementType.Select, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Sender Client Secret"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Client Secret für den Sender."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
        })
        public String senderClientSecret;
    }

    @LayoutElementPOJOBinding(id = "fit-connect-identity-provider-binding-config", type = ElementType.ConfigLayout)
    public static class IdentityBinding {
        public static final String BPK2_ATTRIBUTE_FIELD_ID = "bpk2Attribute";
        @InputElementPOJOBinding(id = BPK2_ATTRIBUTE_FIELD_ID, type = ElementType.Select, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "BPK2-Attribut"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Attribut des Nutzerkontenanbieters."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
        })
        public String bpk2Attribute;

        public static final String STORK_QAA_LEVEL_FIELD_ID = "storkQaaLevel";
        @InputElementPOJOBinding(id = STORK_QAA_LEVEL_FIELD_ID, type = ElementType.Select, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Attribut für das Stork QAA Level"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Attribut des Nutzerkontenanbieters."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
        })
        public String storkQaaLevel;
    }
}
