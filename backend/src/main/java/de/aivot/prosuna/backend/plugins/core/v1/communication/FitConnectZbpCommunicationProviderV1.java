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
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import de.aivot.prosuna.backend.storage.enums.StorageProviderType;
import de.aivot.prosuna.backend.storage.services.StorageService;
import dev.fitko.fitconnect.rest.client.config.FitConnectEnvironment;
import dev.fitko.fitconnect.rest.model.event.EventState;
import dev.fitko.fitconnect.rest.model.submission.SentSubmission;
import dev.fitko.fitconnect.sdk.FitConnectSdk;
import dev.fitko.fitconnect.sdk.api.Addressing;
import dev.fitko.fitconnect.sdk.api.Attachment;
import dev.fitko.fitconnect.sdk.api.OutgoingSubmission;
import dev.fitko.fitconnect.sdk.api.Participant;
import dev.fitko.fitconnect.sdk.api.SubmissionData;
import dev.fitko.fitconnect.sdk.api.event.CaseEvent;
import dev.fitko.fitconnect.sdk.clients.Organisation;
import dev.fitko.fitconnect.zbp.internal.ZBPEnvelopeBuilder;
import dev.fitko.fitconnect.zbp.model.AuthenticationLevel;
import dev.fitko.fitconnect.zbp.model.AuthorKeyPair;
import dev.fitko.fitconnect.zbp.model.CreateMessage;
import dev.fitko.fitconnect.zbp.model.ZBPAttachmentMetadata;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Sends identity-bound messages and attachments to ZBP through the FIT-Connect bridge service. */
@Component
public class FitConnectZbpCommunicationProviderV1 implements CommunicationProviderDefinition<FitConnectZbpCommunicationProviderV1.Config, FitConnectZbpCommunicationProviderV1.IdentityBinding> {
    public static final String COMPONENT_KEY = "fit_connect_zbp_communication_provider";
    private static final String MESSAGE_SENDING_IDENTIFIER = "urn:schema-fitko-de:fit-connect:id.bund.de:message_v6";
    private static final URI ZBP_MESSAGE_SCHEMA_URI = URI.create(
            "https://schema.fitko.de/fit-connect/id.bund.de/message_v6/1.0.0/zbp-message.schema.json"
    );

    private final StorageService storageService;
    private final SecretService secretService;

    public FitConnectZbpCommunicationProviderV1(StorageService storageService, SecretService secretService) {
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
        return "FIT-Connect-ZBP-Kommunikation";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Versendet Nachrichten und Anhänge über FIT-Connect an das zentrale Bürgerpostfach.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Versendet Nachrichten einschließlich Anhängen über einen konfigurierten FIT-Connect-Zustellpunkt an das zentrale Bürgerpostfach einer Identität.";
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
                    element.setAllowedStorageProviderTypes(List.of(StorageProviderType.Assets));
                });

        config
                .findChild(Config.ZBP_CERTIFICATE_CLIENT_CERT_PATH_FIELD_ID, StoragePathSelectorInputElement.class)
                .ifPresent(element -> {
                    element.setMode(StoragePathSelectorMode.File);
                    element.setAllowedStorageProviderTypes(List.of(StorageProviderType.Assets));
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
        final UUID senderDestinationId = getSenderDestinationId(config);
        final AuthorKeyPair authorKeyPair = getAuthorKeyPair(context.communicationProviderConfiguration());
        final UUID postfachId = getPostfachId(context, identity);

        final List<Attachment> fitConnectAttachments = new ArrayList<>();
        final List<ZBPAttachmentMetadata> attachmentMetadata = new ArrayList<>();
        if (message.attachments() != null) {
            var attachmentIndex = 1;
            for (var att : message.attachments()) {
                var attachmentContent = att.getContent();
                if (attachmentContent == null) {
                    throw new CommunicationException("Attachment content is null for attachment: " + att.getName());
                }

                final byte[] attachmentData;
                try (attachmentContent) {
                    attachmentData = attachmentContent.readAllBytes();
                } catch (IOException e) {
                    throw new CommunicationException("Failed to read attachment: " + att.getName(), e);
                }

                final String contentType = att.getContentType() != null ? att.getContentType() : "application/octet-stream";
                final String fileName = att.getName() == null || att.getName().isBlank()
                        ? "attachment-" + attachmentIndex
                        : att.getName();

                final Attachment fitConnectAttachment = Attachment
                        .builder()
                        .fromBytes(attachmentData)
                        .mimeType(contentType)
                        .fileName(fileName)
                        .description(fileName)
                        .build();

                fitConnectAttachments.add(fitConnectAttachment);
                attachmentMetadata.add(ZBPAttachmentMetadata.from(fileName, attachmentData));
                attachmentIndex++;
            }
        }

        final AuthenticationLevel mappedAuthenticationLevel = mapAuthenticationLevel(context, identity);

        final CreateMessage zbpMessage = CreateMessage
                .builder()
                .content(message.htmlBody())
                .sender("FIT-Connect")
                .service("FIT-Connect Test")
                .title(message.subject())
                //.retrievalConfirmationAddress("retrieval@mail.net")
                //.replyAddress("reply@mail.net")
                .mailboxUuid(postfachId)
                .stork_qaa_level(mappedAuthenticationLevel)
                .attachmentMetadata(attachmentMetadata)
                .build();

        final OutgoingSubmission submission = OutgoingSubmission
                .to(Participant.of(
                        destinationId,
                        Addressing.toService(MESSAGE_SENDING_IDENTIFIER, "ZBP Message Forwarding")
                ))
                .setData(SubmissionData.json(
                        ZBPEnvelopeBuilder.fromAuthorPayload(zbpMessage, authorKeyPair),
                        ZBP_MESSAGE_SCHEMA_URI
                ))
                .addAttachments(fitConnectAttachments)
                .build();

        final Organisation organisation;
        try {
            organisation = createOrganisation(
                    config.senderClientId,
                    resolveSenderClientSecret(config),
                    senderDestinationId
            );
        } catch (CommunicationException e) {
            throw e;
        } catch (Exception e) {
            throw new CommunicationException("Failed to initialize FIT-Connect organisation client.", e);
        }

        final SentSubmission sentSubmission;
        final CaseEvent status;
        try {
            sentSubmission = organisation.send(submission);
            status = organisation.cases().logOf(sentSubmission).latest();
        } catch (Exception e) {
            throw new CommunicationException("Failed to send message via FIT-Connect.", e);
        }

        if (status.state() != EventState.ACCEPTED && status.state() != EventState.SUBMITTED) {
            throw new CommunicationException("Failed to send message via FIT-Connect. Status: " + status);
        }

        return Map.of(
                "postfachId", postfachId.toString(),
                "submissionId", sentSubmission.submissionId().toString(),
                "status", status.state().name()
        );
    }

    private AuthenticationLevel mapAuthenticationLevel(CommunicationProviderContext<Config, IdentityBinding> context,
                                                       IdentityData identity) {
        var attributeKey = context.identityProviderBindingConfiguration().storkQaaLevel;
        if (attributeKey == null || attributeKey.isBlank()) {
            return AuthenticationLevel.ONE;
        }

        var authenticationLevel = identity.attributes().get(attributeKey);
        if (authenticationLevel == null) {
            return AuthenticationLevel.ONE;
        }

        return switch (authenticationLevel) {
            case "level2" -> AuthenticationLevel.TWO;
            case "level3" -> AuthenticationLevel.THREE;
            case "level4" -> AuthenticationLevel.FOUR;
            default -> AuthenticationLevel.ONE;
        };
    }

    @Nonnull
    private static UUID getPostfachId(@Nonnull CommunicationProviderContext<Config, IdentityBinding> context, @Nonnull IdentityData identity) throws CommunicationException {
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
    private static UUID getDestinationId(Config config) throws CommunicationException {
        UUID destinationId;
        try {
            destinationId = UUID.fromString(config.destinationId);
        } catch (Exception e) {
            throw new CommunicationException("Failed to parse destination ID as UUID: " + config.destinationId, e);
        }
        return destinationId;
    }

    @Nonnull
    private static UUID getSenderDestinationId(Config config) throws CommunicationException {
        try {
            return UUID.fromString(config.senderDestinationId);
        } catch (Exception e) {
            throw new CommunicationException("Failed to parse sender destination ID as UUID: " + config.senderDestinationId, e);
        }
    }

    @Nonnull
    private String resolveSenderClientSecret(Config config) throws CommunicationException {
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

        return senderClientSecret;
    }

    @Nonnull
    Organisation createOrganisation(@Nonnull String clientId,
                                    @Nonnull String clientSecret,
                                    @Nonnull UUID senderDestinationId) {
        return FitConnectSdk
                .fromConfigBuilder()
                .credentials(clientId, clientSecret)
                .environment(FitConnectEnvironment.TEST)
                .build()
                .organisation(senderDestinationId);
    }

    private AuthorKeyPair getAuthorKeyPair(Config config) throws CommunicationException {
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

        try {
            return AuthorKeyPair
                    .builder()
                    .authorPrivateKeyAsPem(privateKeyPem)
                    .authorCertificateAsPem(clientCertPem)
                    .build();
        } catch (RuntimeException e) {
            throw new CommunicationException("Failed to parse the ZBP author certificate or private key.", e);
        }
    }

    private String resolveFile(StoragePathSelectorInputElementValue p) throws ResponseException, IOException {
        if (p == null || p.getStorageProviderId() == null || p.getPath() == null) {
            throw new IOException("Die Datei ist nicht konfiguriert.");
        }

        try (var content = storageService.getDocumentContent(p.getStorageProviderId(), p.getPath())) {
            return new String(content.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @LayoutElementPOJOBinding(id = "fit-connect-provider-config", type = ElementType.ConfigLayout)
    public static class Config {
        public static final String ZBP_CERTIFICATE_PRIVATE_KEY_PATH_FIELD_ID = "zbpCertificatePrivateKeyPath";
        public static final String ZBP_CERTIFICATE_CLIENT_CERT_PATH_FIELD_ID = "zbpCertificateClientCertPath";
        public static final String DESTINATION_ID_FIELD_ID = "destinationId";
        public static final String SENDER_DESTINATION_ID_FIELD_ID = "senderDestinationId";
        public static final String SENDER_CLIENT_ID_FIELD_ID = "senderClientId";
        public static final String SENDER_CLIENT_SECRET_KEY_FIELD_ID = "senderClientSecret";

        @InputElementPOJOBinding(id = ZBP_CERTIFICATE_PRIVATE_KEY_PATH_FIELD_ID, type = ElementType.StoragePathSelector, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Privater Schlüssel"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Pfad zum privaten Schlüssel des FIT-Connect-Zertifikats."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "allowReadOnlyStorageProviders", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0),
        })
        public StoragePathSelectorInputElementValue zbpCertificatePrivateKeyPath;

        @InputElementPOJOBinding(id = ZBP_CERTIFICATE_CLIENT_CERT_PATH_FIELD_ID, type = ElementType.StoragePathSelector, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Client-Zertifikat"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Pfad zum Client-Zertifikat des FIT-Connect-Zertifikats."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "allowReadOnlyStorageProviders", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0),
        })
        public StoragePathSelectorInputElementValue zbpCertificateClientCertPath;

        @InputElementPOJOBinding(id = DESTINATION_ID_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Empfänger-Zustellpunkt-ID"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Zustellpunkt-ID des ZBP-Brückendienstes, der die Nachricht empfängt."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0),
        })
        public String destinationId;

        @InputElementPOJOBinding(id = SENDER_DESTINATION_ID_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Absender-Zustellpunkt-ID"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Zustellpunkt-ID der Organisation, die die ZBP-Nachricht versendet."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0),
        })
        public String senderDestinationId;

        @InputElementPOJOBinding(id = SENDER_CLIENT_ID_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Sender Client ID"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Client ID für den Sender."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0),
        })
        public String senderClientId;

        @InputElementPOJOBinding(id = SENDER_CLIENT_SECRET_KEY_FIELD_ID, type = ElementType.SecretSelectInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Sender Client Secret"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Client Secret für den Sender."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0),
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
