package de.aivot.prosuna.backend.plugins.core.v1.communication;

import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.communication.models.CommunicationProviderContext;
import de.aivot.prosuna.backend.communication.models.CommunicationProviderDefinition;
import de.aivot.prosuna.backend.communication.models.MailCommunicationSendOptions;
import de.aivot.prosuna.backend.communication.services.DefaultMailCommunicationService;
import de.aivot.prosuna.backend.communication.utils.EmailAddressUtils;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.elements.ElementValueFunctions;
import de.aivot.prosuna.backend.elements.models.elements.ElementVisibilityFunctions;
import de.aivot.prosuna.backend.elements.models.elements.form.content.AlertContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RadioInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RadioInputElementOption;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElementPattern;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.AlertType;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import de.aivot.prosuna.backend.identity.enums.IdentityProviderType;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.mail.services.MailConfigurationService;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.plugins.core.v1.operators.common.NoCodeEqualsOperator;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class MailCommunicationProviderV1 implements CommunicationProviderDefinition<MailCommunicationProviderV1.Config, MailCommunicationProviderV1.IdentityBinding> {
    public static final String COMPONENT_KEY = "mail_communication_provider";
    public static final String EMAIL_ATTRIBUTE_FIELD_ID = "emailAttribute";
    public static final String CUSTOMER_EMAIL_FIELD_ID = "email";

    public static final String SENDER_MODE_FIELD_ID = "senderMode";
    public static final String SENDER_MODE_DEFAULT = "default";
    public static final String SENDER_MODE_CUSTOM = "custom";
    public static final String DEFAULT_SENDER_GROUP_ID = "defaultSenderGroup";
    public static final String DEFAULT_SENDER_NAME_FIELD_ID = "defaultSenderName";
    public static final String DEFAULT_SENDER_ADDRESS_FIELD_ID = "defaultSenderAddress";
    public static final String CUSTOM_SENDER_GROUP_ID = "customSenderGroup";
    public static final String CUSTOM_SENDER_NAME_FIELD_ID = "customSenderName";
    public static final String CUSTOM_SENDER_ADDRESS_FIELD_ID = "customSenderAddress";
    public static final String REPLY_TO_ADDRESS_FIELD_ID = "replyToAddress";
    public static final String CUSTOM_SENDER_ALERT_ID = "customSenderAlert";
    private static final String CONFIG_LAYOUT_ID = "mail-provider-config";

    private final DefaultMailCommunicationService defaultMailCommunicationService;
    private final MailConfigurationService mailConfigurationService;

    public MailCommunicationProviderV1(DefaultMailCommunicationService defaultMailCommunicationService,
                                       MailConfigurationService mailConfigurationService) {
        this.defaultMailCommunicationService = defaultMailCommunicationService;
        this.mailConfigurationService = mailConfigurationService;
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
        return "E-Mail-Kommunikation";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Versendet Nachrichten per E-Mail an die ausgewählte Identität.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Versendet Nachrichten an eine E-Mail-Adresse aus den Daten der ausgewählten Identität oder an eine von der Kund:in ergänzte Adresse.";
    }

    @Nonnull
    @Override
    public Class<Config> getConfigClass() {
        return Config.class;
    }

    @Nonnull
    @Override
    public ConfigLayoutElement getConfigLayout() throws ResponseException {
        final ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper.createFromPOJO(Config.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(
                    "Das Konfigurationslayout für den E-Mail-Kommunikationsanbieter konnte nicht erstellt werden.",
                    e
            );
        }

        layout
                .findChild(SENDER_MODE_FIELD_ID, RadioInputElement.class)
                .ifPresent(senderMode -> {
                    senderMode.setOptions(List.of(
                            RadioInputElementOption.of(SENDER_MODE_DEFAULT, "Standardabsender verwenden"),
                            RadioInputElementOption.of(SENDER_MODE_CUSTOM, "Eigenen Absender verwenden")
                    ));
                    senderMode.setValue(new ElementValueFunctions().setNoCode(
                            NoCodeStaticValue.of(SENDER_MODE_DEFAULT)
                    ));
                });

        var mailConfiguration = mailConfigurationService.getConfiguration();

        layout
                .findChild(DEFAULT_SENDER_NAME_FIELD_ID, TextInputElement.class)
                .ifPresent(field -> field.setValue(new ElementValueFunctions().setNoCode(NoCodeStaticValue.of(
                        displayConfiguredValue(mailConfiguration.senderName())
                ))));
        layout
                .findChild(DEFAULT_SENDER_ADDRESS_FIELD_ID, TextInputElement.class)
                .ifPresent(field -> field.setValue(new ElementValueFunctions().setNoCode(NoCodeStaticValue.of(
                        displayConfiguredValue(mailConfiguration.senderAddress())
                ))));
        layout
                .findChild(DEFAULT_SENDER_GROUP_ID, GroupLayoutElement.class)
                .ifPresent(group -> group.setVisibility(senderModeVisibility(SENDER_MODE_DEFAULT)));
        layout
                .findChild(CUSTOM_SENDER_GROUP_ID, GroupLayoutElement.class)
                .ifPresent(group -> {
                    group.setVisibility(senderModeVisibility(SENDER_MODE_CUSTOM));
                    group.getChildren().addFirst(createCustomSenderAlert(mailConfiguration.host()));
                });
        layout
                .findChild(CUSTOM_SENDER_ADDRESS_FIELD_ID, TextInputElement.class)
                .ifPresent(field -> field.setPattern(emailPattern()));
        layout
                .findChild(REPLY_TO_ADDRESS_FIELD_ID, TextInputElement.class)
                .ifPresent(field -> field.setPattern(optionalEmailPattern()));

        return layout;
    }

    @Nonnull
    @Override
    public List<IdentityProviderType> getSupportedIdentityProviderTypes() {
        return Arrays.asList(IdentityProviderType.values());
    }

    @Nonnull
    @Override
    public Class<IdentityBinding> getIdentityProviderBindingConfigClass() {
        return IdentityBinding.class;
    }

    @Nonnull
    @Override
    public ConfigLayoutElement getIdentityProviderBindingConfigLayout(@Nonnull IdentityProviderEntity identityProviderEntity) {
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

        var emailAttribute = new SelectInputElement();
        emailAttribute.setId(EMAIL_ATTRIBUTE_FIELD_ID);
        emailAttribute.setLabel("E-Mail-Attribut");
        emailAttribute.setHint("Optionales Attribut des Nutzerkontenanbieters. Fehlt es in den Anmeldedaten, wird die E-Mail-Adresse von der Kund:in abgefragt.");
        emailAttribute.setRequired(false);
        emailAttribute.setOptions(attributeOptions);

        var layout = new ConfigLayoutElement();
        layout.setId("mail-identity-provider-binding-config");
        layout.setChildren(List.of(emailAttribute));
        return layout;
    }

    @Override
    public GroupLayoutElement getCustomerLayout(
            @Nonnull CommunicationProviderContext<Config, IdentityBinding> context,
            @Nonnull IdentityData identityData
    ) {
        if (resolveMappedEmail(context.identityProviderBindingConfiguration(), identityData) != null) {
            return null;
        }

        var email = new TextInputElement();
        email.setId(CUSTOMER_EMAIL_FIELD_ID);
        email.setLabel("E-Mail-Adresse");
        email.setHint("An diese Adresse werden Nachrichten zu Ihrem Vorgang gesendet.");
        email.setAutocomplete("email");
        email.setRequired(true);
        email.setPattern(TextInputElementPattern.of(
                EmailAddressUtils.EMAIL_PATTERN_VALUE,
                "Bitte geben Sie eine gültige E-Mail-Adresse ein."
        ));

        var layout = new GroupLayoutElement();
        layout.setId("mail-customer-config");
        layout.setChildren(List.of(email));
        return layout;
    }

    @Override
    public Map<String, Object> sendMessage(@Nonnull CommunicationProviderContext<Config, IdentityBinding> context,
                                           @Nonnull IdentityData identity,
                                           @Nonnull CommunicationMessage message) throws CommunicationException {
        var recipient = resolveEmail(context.identityProviderBindingConfiguration(), identity);
        if (recipient == null) {
            throw new CommunicationException("Für die Identität %s ist keine E-Mail-Adresse verfügbar.", identity.identityId());
        }
        var config = context.communicationProviderConfiguration();
        var options = switch (config.senderMode) {
            case SENDER_MODE_DEFAULT -> MailCommunicationSendOptions.defaults();
            case SENDER_MODE_CUSTOM -> {
                if (config.customSender == null) {
                    throw new CommunicationException("Die eigene Absenderkonfiguration fehlt.");
                }
                yield MailCommunicationSendOptions.customSender(
                        config.customSender.name,
                        config.customSender.address,
                        config.customSender.replyToAddress
                );
            }
            case null, default -> throw new CommunicationException("Die Absenderkonfiguration ist ungültig.");
        };
        defaultMailCommunicationService.sendMessage(recipient, message, options);

        assert message.subject() != null;
        assert message.htmlBody() != null;

        return Map.of(
                "recipient", recipient,
                "subject", message.subject(),
                "body", message.htmlBody()
        );
    }

    private static String resolveEmail(@Nonnull IdentityBinding identityBinding, @Nonnull IdentityData identity) {
        var mappedEmail = resolveMappedEmail(identityBinding, identity);
        if (mappedEmail != null) {
            return mappedEmail;
        }
        var customerEmail = identity.communicationProviderData().get(CUSTOMER_EMAIL_FIELD_ID);
        if (customerEmail instanceof String value && !value.isBlank()) {
            return value.trim();
        }
        return null;
    }

    private static String resolveMappedEmail(@Nonnull IdentityBinding identityBinding, @Nonnull IdentityData identity) {
        if (identityBinding.emailAttribute != null && !identityBinding.emailAttribute.isBlank()) {
            var mapped = identity.attributes().get(identityBinding.emailAttribute);
            if (mapped != null) {
                var trimmed = mapped.trim();
                if (EmailAddressUtils.isValidSingleAddress(trimmed)) {
                    return trimmed;
                }
            }
        }
        return null;
    }

    private static ElementVisibilityFunctions senderModeVisibility(@Nonnull String senderMode) {
        return ElementVisibilityFunctions.of(NoCodeExpression.of(
                NoCodeEqualsOperator.OPERATOR_ID,
                NoCodeReference.of(SENDER_MODE_FIELD_ID),
                NoCodeStaticValue.of(senderMode)
        )).recalculateReferencedIds();
    }

    private static TextInputElementPattern emailPattern() {
        return TextInputElementPattern.of(
                EmailAddressUtils.EMAIL_PATTERN_VALUE,
                "Bitte geben Sie eine gültige E-Mail-Adresse ein."
        );
    }

    private static TextInputElementPattern optionalEmailPattern() {
        return TextInputElementPattern.of(
                "^(?:$|[^\\s@]+@[^\\s@]+\\.[^\\s@]+)$",
                "Bitte geben Sie eine gültige E-Mail-Adresse ein."
        );
    }

    private static String displayConfiguredValue(String value) {
        return value == null || value.isBlank() ? "Nicht konfiguriert" : value.trim();
    }

    private static AlertContentElement createCustomSenderAlert(String smtpHost) {
        var smtpServer = smtpHost == null || smtpHost.isBlank()
                ? "den verwendeten SMTP-Server"
                : "den SMTP-Server „%s“".formatted(smtpHost.trim());
        var alert = new AlertContentElement();
        alert.setId(CUSTOM_SENDER_ALERT_ID);
        alert.setTitle("Eigene Absenderadresse prüfen");
        alert.setText("""
                Der SMTP-Zugang für %s erlaubt möglicherweise nicht jede Absenderadresse. Viele E-Mail-Anbieter lassen nur die Adresse des angemeldeten Kontos oder vorher freigeschaltete Adressen zu. Wenn die hier eingetragene Absenderadresse nicht freigegeben ist, lehnt der Server den Versand ab. In manchen Fällen wird die Nachricht zwar zugestellt, aber als Spam eingestuft. Verwenden Sie deshalb nur eine Adresse, die bei diesem Anbieter für den Versand freigeschaltet ist.
                """.formatted(smtpServer).trim());
        alert.setAlertType(AlertType.Warning);
        return alert;
    }

    @LayoutElementPOJOBinding(id = CONFIG_LAYOUT_ID, type = ElementType.ConfigLayout)
    public static class Config {
        @InputElementPOJOBinding(id = SENDER_MODE_FIELD_ID, type = ElementType.Radio, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Absender"),
                @ElementPOJOBindingProperty(
                        key = "hint",
                        strValue = "Wählen Sie, ob der globale Standardabsender oder eigene Absenderdaten verwendet werden sollen."
                ),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
        })
        public String senderMode;

        public DefaultSenderConfig defaultSender;

        public CustomSenderConfig customSender;
    }

    @LayoutElementPOJOBinding(id = DEFAULT_SENDER_GROUP_ID, type = ElementType.GroupLayout)
    public static class DefaultSenderConfig {
        @InputElementPOJOBinding(id = DEFAULT_SENDER_NAME_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "From Name"),
                @ElementPOJOBindingProperty(key = "disabled", boolValue = true),
        })
        public String name;

        @InputElementPOJOBinding(id = DEFAULT_SENDER_ADDRESS_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "From Adresse"),
                @ElementPOJOBindingProperty(key = "disabled", boolValue = true),
        })
        public String address;

    }

    @LayoutElementPOJOBinding(id = CUSTOM_SENDER_GROUP_ID, type = ElementType.GroupLayout)
    public static class CustomSenderConfig {
        @InputElementPOJOBinding(id = CUSTOM_SENDER_NAME_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "From Name"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Anzeigename des Absenders."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "maxCharacters", intValue = 255),
        })
        public String name;

        @InputElementPOJOBinding(id = CUSTOM_SENDER_ADDRESS_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "From Adresse"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "E-Mail-Adresse des Absenders."),
                @ElementPOJOBindingProperty(key = "autocomplete", strValue = "email"),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "maxCharacters", intValue = 254),
        })
        public String address;

        @InputElementPOJOBinding(id = REPLY_TO_ADDRESS_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Reply-To-Adresse"),
                @ElementPOJOBindingProperty(
                        key = "hint",
                        strValue = "Optional. Bleibt das Feld leer, werden Antworten an die From-Adresse gesendet."
                ),
                @ElementPOJOBindingProperty(key = "autocomplete", strValue = "email"),
                @ElementPOJOBindingProperty(key = "required", falseValue = true),
                @ElementPOJOBindingProperty(key = "maxCharacters", intValue = 254),
        })
        public String replyToAddress;
    }

    @LayoutElementPOJOBinding(id = "mail-identity-provider-binding-config", type = ElementType.ConfigLayout)
    public static class IdentityBinding {
        @InputElementPOJOBinding(id = EMAIL_ATTRIBUTE_FIELD_ID, type = ElementType.Select)
        public String emailAttribute;
    }
}
