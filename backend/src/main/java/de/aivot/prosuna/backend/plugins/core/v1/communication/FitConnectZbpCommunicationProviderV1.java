package de.aivot.prosuna.backend.plugins.core.v1.communication;

import de.aivot.prosuna.backend.communication.entities.CommunicationProviderBindingEntity;
import de.aivot.prosuna.backend.communication.entities.CommunicationProviderEntity;
import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.communication.models.CommunicationProviderContext;
import de.aivot.prosuna.backend.communication.models.CommunicationProviderDefinition;
import de.aivot.prosuna.backend.asset.services.AssetContentResolverService;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.enums.AssetVisibility;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
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
import dev.fitko.fitconnect.sdk.clients.OnlineService;
import dev.fitko.fitconnect.zbp.internal.ZBPEnvelopeBuilder;
import dev.fitko.fitconnect.zbp.model.AuthenticationLevel;
import dev.fitko.fitconnect.zbp.model.AuthorKeyPair;
import dev.fitko.fitconnect.zbp.model.CreateMessage;
import dev.fitko.fitconnect.zbp.model.ZBPAttachmentMetadata;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

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
    public static final String TEST_POSTFACH_ID_FIELD_ID = "postfachId";
    private static final String TESTING_LAYOUT_ID = "fit-connect-zbp-testing-config";
    private static final String TEST_CONTEXT_ID = "communication-provider-test";
    private static final int TEST_BINDING_ID = -1;
    private static final String UUID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
    private static final String MESSAGE_SENDING_IDENTIFIER = "urn:schema-fitko-de:fit-connect:id.bund.de:message_v6";
    private static final String PROSUNA_NAME = "Prosuna";
    private static final URI ZBP_MESSAGE_SCHEMA_URI = URI.create(
            "https://schema.fitko.de/fit-connect/id.bund.de/message_v6/1.0.0/zbp-message.schema.json"
    );

    private static final String KEY_MIME_TYPE = "application/pkcs8";
    private static final String CERT_MIME_TYPE = "application/x-x509-ca-cert";

    private final AssetContentResolverService assetContentResolverService;
    private final SecretService secretService;

    public FitConnectZbpCommunicationProviderV1(AssetContentResolverService assetContentResolverService,
                                                SecretService secretService) {
        this.assetContentResolverService = assetContentResolverService;
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
                .findChild(Config.ZBP_CERTIFICATE_PRIVATE_KEY_ASSET_KEY_FIELD_ID, AssetSelectInputElement.class)
                .ifPresent(element -> {
                    element.setAllowedMimeTypes(List.of(CERT_MIME_TYPE));
                    element.setAssetVisibility(AssetVisibility.Private);
                });

        config
                .findChild(Config.ZBP_CERTIFICATE_CLIENT_CERT_ASSET_KEY_FIELD_ID, AssetSelectInputElement.class)
                .ifPresent(element -> {
                    element.setAllowedMimeTypes(List.of(KEY_MIME_TYPE));
                    element.setAssetVisibility(AssetVisibility.Private);
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
        var postfachId = new TextInputElement();
        postfachId.setId(TEST_POSTFACH_ID_FIELD_ID);
        postfachId.setLabel("Postfach-ID");
        postfachId.setHint("Postfach-ID des Testnutzers, an den die Testnachricht gesendet wird.");
        postfachId.setRequired(true);
        postfachId.setPattern(TextInputElementPattern.of(
                UUID_REGEX,
                "Bitte geben Sie eine gültige UUID ein."
        ));

        var layout = new GroupLayoutElement();
        layout.setId(TESTING_LAYOUT_ID);
        layout.setChildren(List.of(postfachId));
        return layout;
    }

    @Override
    public void handleTest(@Nonnull CommunicationProviderEntity providerEntity,
                           @Nonnull Config config,
                           @Nonnull AuthoredElementValues inputs) throws CommunicationException {
        var input = inputs.get(TEST_POSTFACH_ID_FIELD_ID);
        if (!(input instanceof String rawPostfachId) || rawPostfachId.isBlank()) {
            throw new CommunicationException("Die Postfach-ID des Testnutzers ist erforderlich.");
        }

        var normalizedPostfachId = rawPostfachId.trim();
        final UUID postfachId;
        try {
            postfachId = UUID.fromString(normalizedPostfachId);
        } catch (IllegalArgumentException e) {
            throw new CommunicationException("Die Postfach-ID des Testnutzers muss eine gültige UUID sein.", e);
        }
        if (!postfachId.toString().equalsIgnoreCase(normalizedPostfachId)) {
            throw new CommunicationException("Die Postfach-ID des Testnutzers muss eine gültige UUID sein.");
        }

        var identityProviderKey = UUID.randomUUID();
        var testIdentityProvider = new IdentityProviderEntity()
                .setKey(identityProviderKey)
                .setMetadataIdentifier(TEST_CONTEXT_ID)
                .setUniqueIdAttribute("id")
                .setType(IdentityProviderType.Custom)
                .setName("Kommunikationsanbieter-Test")
                .setDescription("Temporärer Nutzerkontenanbieter für einen Kommunikationstest.")
                .setAuthorizationEndpoint("")
                .setTokenEndpoint("")
                .setClientId(TEST_CONTEXT_ID)
                .setAttributes(List.of())
                .setDefaultScopes(List.of())
                .setAdditionalParams(List.of())
                .setIsEnabled(true)
                .setIsTestProvider(providerEntity.getTestProvider());
        var testBinding = new CommunicationProviderBindingEntity()
                .setId(TEST_BINDING_ID)
                .setIdentityProviderKey(identityProviderKey)
                .setCommunicationProviderId(providerEntity.getId())
                .setName("Kommunikationsanbieter-Test")
                .setDescription("Temporäre Anbindung für einen Kommunikationstest.")
                .setEnabled(true)
                .setPosition(0)
                .setConfiguration(new AuthoredElementValues());
        var testIdentityBinding = new IdentityBinding();
        testIdentityBinding.bpk2Attribute = TEST_POSTFACH_ID_FIELD_ID;

        var testContext = new CommunicationProviderContext<>(
                providerEntity,
                testIdentityProvider,
                testBinding,
                config,
                testIdentityBinding
        );
        var testIdentity = new IdentityData(
                TEST_CONTEXT_ID,
                TEST_CONTEXT_ID,
                IdentityType.IdentityProvider,
                identityProviderKey,
                TEST_CONTEXT_ID,
                TEST_CONTEXT_ID,
                null,
                Map.of(TEST_POSTFACH_ID_FIELD_ID, postfachId.toString()),
                TEST_BINDING_ID,
                Map.of()
        );
        var testMessage = CommunicationMessage.of(
                "Testnachricht",
                "Dies ist eine Testnachricht.",
                "<p>Dies ist eine Testnachricht.</p>"
        );

        sendMessage(testContext, testIdentity, testMessage);
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

        final CreateMessage zbpMessage = createZbpMessage(
                message,
                postfachId,
                mappedAuthenticationLevel,
                attachmentMetadata
        );

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

        final OnlineService onlineService;
        try {
            onlineService = createOnlineService(
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
            sentSubmission = onlineService.send(submission);
            status = onlineService.cases().logOf(sentSubmission).latest();
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

    @Nonnull
    static CreateMessage createZbpMessage(
            @Nonnull CommunicationMessage message,
            @Nonnull UUID postfachId,
            @Nonnull AuthenticationLevel authenticationLevel,
            @Nonnull List<ZBPAttachmentMetadata> attachmentMetadata
    ) throws CommunicationException {
        var departmentName = message.sendingDepartment() == null
                ? null
                : message.sendingDepartment().getName();
        var sender = departmentName == null || departmentName.isBlank()
                ? PROSUNA_NAME
                : departmentName.trim();

        return CreateMessage
                .builder()
                .content(renderMessageHtml(message))
                .sender(sender)
                .service(PROSUNA_NAME)
                .title(message.subject())
                //.retrievalConfirmationAddress("retrieval@mail.net")
                //.replyAddress("reply@mail.net")
                .mailboxUuid(postfachId)
                .stork_qaa_level(authenticationLevel)
                .attachmentMetadata(attachmentMetadata)
                .build();
    }

    @Nonnull
    static String renderMessageHtml(@Nonnull CommunicationMessage message) throws CommunicationException {
        var content = new StringBuilder(message.htmlBody());
        for (var callToAction : message.callToActions()) {
            if (callToAction == null) {
                throw new CommunicationException("Eine Aktion der Nachricht darf nicht leer sein.");
            }
            var title = callToAction.title() == null ? null : callToAction.title().trim();
            if (title == null || title.isEmpty()) {
                throw new CommunicationException("Der Titel einer Aktion darf nicht leer sein.");
            }
            var link = callToAction.link() == null ? null : callToAction.link().trim();
            if (link == null || link.isEmpty()) {
                throw new CommunicationException("Der Link einer Aktion darf nicht leer sein.");
            }

            if (content.length() > 0) {
                content.append('\n');
            }
            content.append("<p><a href=\"")
                    .append(HtmlUtils.htmlEscape(link))
                    .append("\">")
                    .append(HtmlUtils.htmlEscape(title))
                    .append("</a></p>");
        }
        return content.toString();
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
    OnlineService createOnlineService(@Nonnull String clientId,
                                      @Nonnull String clientSecret,
                                      @Nonnull UUID senderDestinationId) {
        return FitConnectSdk
                .fromConfigBuilder()
                .credentials(clientId, clientSecret)
                .environment(FitConnectEnvironment.TEST)
                .build()
                .onlineService(senderDestinationId);
    }

    private AuthorKeyPair getAuthorKeyPair(Config config) throws CommunicationException {
        String privateKeyPem;
        try {
            privateKeyPem = resolveFile(
                    config.zbpCertificatePrivateKeyAssetKey,
                    "Der private Schlüssel des FIT-Connect-Zertifikats"
            );
        } catch (ResponseException e) {
            throw new CommunicationException("Failed to resolve private key for FIT-Connect communication provider.", e);
        }

        String clientCertPem;
        try {
            clientCertPem = resolveFile(
                    config.zbpCertificateClientCertAssetKey,
                    "Das Client-Zertifikat des FIT-Connect-Zertifikats"
            );
        } catch (ResponseException e) {
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

    private String resolveFile(@Nullable String assetKey, @Nonnull String description) throws ResponseException {
        return new String(
                assetContentResolverService.resolveContent(assetKey, AssetVisibility.Private, description),
                StandardCharsets.UTF_8
        );
    }

    @LayoutElementPOJOBinding(id = "fit-connect-provider-config", type = ElementType.ConfigLayout)
    public static class Config {
        public static final String ZBP_CERTIFICATE_PRIVATE_KEY_ASSET_KEY_FIELD_ID = "zbpCertificatePrivateKeyAssetKey";
        public static final String ZBP_CERTIFICATE_CLIENT_CERT_ASSET_KEY_FIELD_ID = "zbpCertificateClientCertAssetKey";
        public static final String DESTINATION_ID_FIELD_ID = "destinationId";
        public static final String SENDER_DESTINATION_ID_FIELD_ID = "senderDestinationId";
        public static final String SENDER_CLIENT_ID_FIELD_ID = "senderClientId";
        public static final String SENDER_CLIENT_SECRET_KEY_FIELD_ID = "senderClientSecret";

        @InputElementPOJOBinding(id = ZBP_CERTIFICATE_PRIVATE_KEY_ASSET_KEY_FIELD_ID, type = ElementType.AssetSelectInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Privater Schlüssel"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Private Schlüssel-Datei im PEM-Format mit dem Schlüssel des FIT-Connect-Zertifikats."),
                @ElementPOJOBindingProperty(key = "dialogTitle", strValue = "Privaten Schlüssel auswählen"),
                @ElementPOJOBindingProperty(key = "placeholder", strValue = "Keine Schlüssel-Datei ausgewählt"),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0),
        })
        public String zbpCertificatePrivateKeyAssetKey;

        @InputElementPOJOBinding(id = ZBP_CERTIFICATE_CLIENT_CERT_ASSET_KEY_FIELD_ID, type = ElementType.AssetSelectInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Client-Zertifikat"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Private Zertifikats-Datei im PEM-Format mit dem Client-Zertifikat für FIT-Connect."),
                @ElementPOJOBindingProperty(key = "dialogTitle", strValue = "Client-Zertifikat auswählen"),
                @ElementPOJOBindingProperty(key = "placeholder", strValue = "Keine Zertifikats-Datei ausgewählt"),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "weight", doubleValue = 6.0),
        })
        public String zbpCertificateClientCertAssetKey;

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
                @ElementPOJOBindingProperty(key = "label", strValue = "Postfach-ID-Attribut"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Attribut des Nutzerkontenanbieters."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
        })
        public String bpk2Attribute;

        public static final String STORK_QAA_LEVEL_FIELD_ID = "storkQaaLevel";
        @InputElementPOJOBinding(id = STORK_QAA_LEVEL_FIELD_ID, type = ElementType.Select, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Attribut für das Vertrauensniveau"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Attribut des Nutzerkontenanbieters."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
        })
        public String storkQaaLevel;
    }
}
